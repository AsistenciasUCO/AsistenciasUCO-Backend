package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxDispatcher;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.OutboxNotificacionAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaRenderer;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SmtpNotificationProperties;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import io.micrometer.core.instrument.MeterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración reales contra SQL Server + GreenMail (TEST_PLAN §4, IT-E01..IT-E10).
 */
@Tag("integration")
@SpringBootTest(properties = {
        "app.adapters.notification.provider=outbox",
        "app.providers.outbox-notification.sender=smtp",
        "app.providers.outbox-notification.sweeper-interval=1s",
        "app.providers.outbox-notification.backoff-base=1s",
        "app.providers.outbox-notification.lease=5s",
        "app.providers.outbox-notification.max-attempts=3",
        "app.providers.outbox-notification.retention-days=30",
        "app.providers.smtp-notification.host=127.0.0.1",
        "app.providers.smtp-notification.from-address=notificaciones@uco.edu.co",
        "app.providers.smtp-notification.from-name=Sistema de Asistencias UCO",
        "app.providers.smtp-notification.starttls=false",
        "app.providers.smtp-notification.ssl=false",
        "app.providers.smtp-notification.require-confirmed-email=false",
        "app.providers.smtp-notification.default-locale=es",
        "app.providers.smtp-notification.template-cache-ttl=1s",
        "app.providers.smtp-notification.rate-per-minute=60",
        "app.providers.smtp-notification.rate-per-day=500",
        "app.providers.smtp-notification.max-bcc=50",
        "app.providers.smtp-notification.platform-url=https://asistencias.uco.edu.co"
})
@MockitoBean(types = JwtDecoder.class)
@Import(NotificacionSmtpGreenMailIT.TestJacksonConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificacionSmtpGreenMailIT {

    private static final int SMTP_PORT = allocateFreePort();
    private static GreenMail greenMail;
    private static boolean greenMailActivo = false;
    private static final AtomicInteger DOC_SEQ = new AtomicInteger(198500000);

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("app.providers.smtp-notification.port", () -> SMTP_PORT);
    }

    private static int allocateFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo reservar puerto libre para GreenMail", e);
        }
    }

    @TestConfiguration
    static class TestJacksonConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private OutboxNotificacionAdapter outboxNotificacionAdapter;

    @Autowired
    private NotificacionOutboxDispatcher dispatcher;

    @Autowired
    private TopeEnvios topeEnvios;

    @Autowired
    private ContactoDestinatarioResolver contactoResolver;

    @Autowired
    private PlantillaRenderer plantillaRenderer;

    @Autowired
    private CorreoTransport correoTransport;

    @Autowired
    private NotificacionOutboxRepository outboxRepository;

    @Autowired
    private OutboxNotificationProperties outboxProperties;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Set<UUID> trackedNotifIds = ConcurrentHashMap.newKeySet();
    private final Set<UUID> trackedUserIds = ConcurrentHashMap.newKeySet();
    private final Set<UUID> trackedTemplateIds = ConcurrentHashMap.newKeySet();

    private ListAppender<ILoggingEvent> logAppender;
    private Logger smtpLogger;

    @BeforeAll
    void startGreenMail() {
        ServerSetup setup = new ServerSetup(SMTP_PORT, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
        setup.setServerStartupTimeout(5000L);
        greenMail = new GreenMail(setup);
        greenMail.start();
        greenMailActivo = true;
    }

    @AfterAll
    void stopGreenMail() {
        limpiarTodo();
        if (greenMail != null && greenMailActivo) {
            greenMail.stop();
            greenMailActivo = false;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        CorrelationIdContext.set(UUID.randomUUID().toString());
        asegurarGreenMailActivo();
        greenMail.purgeEmailFromAllMailboxes();
        topeEnvios.reiniciarVentanas();

        smtpLogger = (Logger) LoggerFactory.getLogger(SmtpNotificacionChannelSender.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        smtpLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        if (smtpLogger != null && logAppender != null) {
            smtpLogger.detachAppender(logAppender);
            logAppender.stop();
        }
        CorrelationIdContext.clear();
        topeEnvios.reiniciarVentanas();
        limpiarTodo();
    }

    private void asegurarGreenMailActivo() {
        if (!greenMailActivo) {
            ServerSetup setup = new ServerSetup(SMTP_PORT, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
            setup.setServerStartupTimeout(5000L);
            greenMail = new GreenMail(setup);
            greenMail.start();
            greenMailActivo = true;
        }
    }

    private void detenerGreenMailTemporalmente() {
        if (greenMail != null && greenMailActivo) {
            greenMail.stop();
            greenMailActivo = false;
        }
    }

    private void limpiarTodo() {
        restaurarPlantillasSemilla();
        for (UUID id : trackedNotifIds) {
            jdbcTemplate.update("DELETE FROM dbo.NotificacionSaliente WHERE id = ?", id);
        }
        trackedNotifIds.clear();
        jdbcTemplate.update("DELETE FROM dbo.NotificacionSaliente WHERE datos LIKE '%\"notif002aIt\":\"true\"%'");

        for (UUID userId : trackedUserIds) {
            jdbcTemplate.update("DELETE FROM dbo.Usuario WHERE id = ?", userId);
        }
        trackedUserIds.clear();
    }

    private void restaurarPlantillasSemilla() {
        for (UUID tplId : trackedTemplateIds) {
            jdbcTemplate.update("DELETE FROM dbo.PlantillaNotificacion WHERE id = ?", tplId);
        }
        trackedTemplateIds.clear();
        jdbcTemplate.update(
                "UPDATE dbo.PlantillaNotificacion SET activa = 1 " +
                        "WHERE idioma = 'es' AND version = 1 AND tipo IN ('RETIRO_ESTUDIANTE_GRUPO')"
        );
    }

    private UUID crearUsuarioPrueba(String correo, boolean confirmado, boolean habilitado) {
        UUID userId = UUID.randomUUID();
        UUID idTipoDoc = jdbcTemplate.queryForObject(
                "SELECT TOP 1 id FROM dbo.TipoIdentificacion",
                UUID.class
        );
        int numDoc = DOC_SEQ.incrementAndGet();
        jdbcTemplate.update(
                "INSERT INTO dbo.Usuario (" +
                        "id, tipoIdIdentificacion, numeroIdentificacion, primerApellido, segundoApellido, " +
                        "primerNombre, segundoNombre, correo, correoConfirmado, estado, [password]" +
                        ") VALUES (?, ?, ?, N'TestNotif', N'Uco', N'Usuario', N'', ?, ?, ?, N'HashTest')",
                userId, idTipoDoc, numDoc, correo, confirmado ? 1 : 0, habilitado ? 1 : 0
        );
        trackedUserIds.add(userId);
        return userId;
    }

    private Map<String, Object> esperarFilaPorDestinatario(UUID destId, String estadoEsperado, int maxSegundos) {
        String patron = "%" + destId + "%";
        final AtomicReference<Map<String, Object>> ultimaRef = new AtomicReference<>();
        try {
            await().atMost(Duration.ofSeconds(maxSegundos))
                    .pollInterval(Duration.ofMillis(250))
                    .until(() -> {
                        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                                "SELECT n.id, n.intentos, n.entregadoEn, n.codigoError, n.detalleError, e.codigo AS estado " +
                                        "FROM dbo.NotificacionSaliente n " +
                                        "JOIN dbo.EstadoNotificacion e ON n.idEstado = e.id " +
                                        "WHERE n.destinatarios LIKE ?",
                                patron
                        );
                        if (!rows.isEmpty()) {
                            Map<String, Object> ultima = rows.get(0);
                            ultimaRef.set(ultima);
                            Object idObj = ultima.get("id");
                            UUID id = idObj instanceof UUID u ? u : UUID.fromString(idObj.toString());
                            trackedNotifIds.add(id);
                            return estadoEsperado.equals(ultima.get("estado"));
                        }
                        return false;
                    });
        } catch (org.awaitility.core.ConditionTimeoutException _) {
            // Se retorna la ultima fila capturada si agoto el tiempo
        }
        return ultimaRef.get();
    }

    private String extraerParteTexto(MimeMessage message, String mimePrefix) throws Exception {
        return buscarParteRecursiva(message.getContent(), message.getContentType(), mimePrefix);
    }

    private String buscarParteRecursiva(Object content, String contentType, String mimePrefix) throws Exception {
        if (contentType != null && contentType.toLowerCase().startsWith(mimePrefix.toLowerCase()) && content instanceof String s) {
            return s;
        }
        if (content instanceof Multipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart part = mp.getBodyPart(i);
                String hallado = buscarParteRecursiva(part.getContent(), part.getContentType(), mimePrefix);
                if (!hallado.isEmpty()) {
                    return hallado;
                }
            }
        }
        return "";
    }

    @Test
    @Order(1)
    @DisplayName("IT-E01: RETIRO_ESTUDIANTE_GRUPO registrado en outbox -> despachado -> GreenMail recibe 1 correo con plantilla de BD")
    void ITE01_retiro_estudiante_grupo_despachado_en_greenmail() throws Exception {
        UUID destId = crearUsuarioPrueba("est.ite01@uco.edu.co", true, true);
        Notificacion notificacion = new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO",
                "Asunto respaldo",
                "Mensaje respaldo",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", destId, null, null)),
                Map.of(
                        "grupo", "ISW-101 (Grupo 01)",
                        "estudiante", "Ana Gómez",
                        "motivo", "Inasistencia reiterada",
                        "notif002aIt", "true"
                )
        );

        outboxNotificacionAdapter.notificar(notificacion);
        dispatcher.signal();

        Map<String, Object> fila = esperarFilaPorDestinatario(destId, "ENTREGADA", 10);
        assertNotNull(fila);
        assertEquals("ENTREGADA", fila.get("estado"));

        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertTrue(messages.length >= 1);
        MimeMessage msg = messages[0];
        assertEquals("Retiro del grupo ISW-101 (Grupo 01)", msg.getSubject());
        String textoPlano = extraerParteTexto(msg, "text/plain");
        assertTrue(textoPlano.contains("Se registró el retiro de Ana Gómez del grupo ISW-101 (Grupo 01)."));
        assertFalse(textoPlano.contains("Inasistencia reiterada"), "El motivo opcional no está en la plantilla semilla v1");
    }

    @Test
    @Order(2)
    @DisplayName("IT-E02: Multipart text/plain y text/html con layout fijo y valores escapados")
    void ITE02_multipart_texto_y_html_con_layout_y_escape() throws Exception {
        UUID destId = crearUsuarioPrueba("est.ite02@uco.edu.co", true, true);
        Notificacion notificacion = new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO",
                "Asunto respaldo",
                "Mensaje respaldo",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", destId, null, null)),
                Map.of(
                        "grupo", "ISW-102 <G1>",
                        "estudiante", "Ana <script>alert(1)</script> & Co",
                        "notif002aIt", "true"
                )
        );

        outboxNotificacionAdapter.notificar(notificacion);
        dispatcher.signal();

        Map<String, Object> fila = esperarFilaPorDestinatario(destId, "ENTREGADA", 10);
        assertNotNull(fila);
        assertEquals("ENTREGADA", fila.get("estado"));

        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
        MimeMessage msg = greenMail.getReceivedMessages()[0];
        String textoPlano = extraerParteTexto(msg, "text/plain");
        String html = extraerParteTexto(msg, "text/html");

        assertTrue(textoPlano.contains("Ana <script>alert(1)</script> & Co"));
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("Universidad Católica de Oriente — Gestión de Asistencia"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(html.contains("<script>alert(1)</script>"));
        assertTrue(html.contains("https://asistencias.uco.edu.co"));
    }

    @Test
    @Order(3)
    @DisplayName("IT-E03: Cambiar plantilla activa (v2) -> siguiente correo usa el contenido nuevo tras TTL de caché")
    void ITE03_cambiar_plantilla_activa_usa_contenido_nuevo_tras_ttl() throws Exception {
        UUID destId1 = crearUsuarioPrueba("est1.ite03@uco.edu.co", true, true);
        UUID destId2 = crearUsuarioPrueba("est2.ite03@uco.edu.co", true, true);

        // 1. Calentar caché con v1
        outboxNotificacionAdapter.notificar(new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO",
                "Asunto respaldo",
                "Mensaje respaldo",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", destId1, null, null)),
                Map.of("grupo", "G-V1", "estudiante", "Pedro", "notif002aIt", "true")
        ));
        dispatcher.signal();
        assertEquals("ENTREGADA", esperarFilaPorDestinatario(destId1, "ENTREGADA", 10).get("estado"));
        greenMail.purgeEmailFromAllMailboxes();

        // 2. Desactivar v1 e insertar v2 activa
        UUID idV2 = UUID.randomUUID();
        trackedTemplateIds.add(idV2);
        jdbcTemplate.update(
                "UPDATE dbo.PlantillaNotificacion SET activa = 0 " +
                        "WHERE tipo = 'RETIRO_ESTUDIANTE_GRUPO' AND idioma = 'es' AND version = 1"
        );
        jdbcTemplate.update(
                "INSERT INTO dbo.PlantillaNotificacion (id, tipo, idioma, version, asunto, cuerpo, activa, creadoPor, creadoEn) " +
                        "VALUES (?, 'RETIRO_ESTUDIANTE_GRUPO', 'es', 2, N'[V2] Retiro en {grupo}', N'Plantilla V2 para {estudiante} en {grupo}.', 1, NULL, SYSUTCDATETIME())",
                idV2
        );

        // Esperar TTL de caché (1s configurado en @SpringBootTest)
        await().pollDelay(Duration.ofMillis(1300)).until(() -> true);

        outboxNotificacionAdapter.notificar(new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO",
                "Asunto respaldo",
                "Mensaje respaldo",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", destId2, null, null)),
                Map.of("grupo", "G-V2", "estudiante", "Lucía", "notif002aIt", "true")
        ));
        dispatcher.signal();

        assertEquals("ENTREGADA", esperarFilaPorDestinatario(destId2, "ENTREGADA", 10).get("estado"));
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
        MimeMessage msg = greenMail.getReceivedMessages()[0];
        assertEquals("[V2] Retiro en G-V2", msg.getSubject());
        assertTrue(extraerParteTexto(msg, "text/plain").contains("Plantilla V2 para Lucía en G-V2."));
    }

    @Test
    @Order(4)
    @DisplayName("IT-E04: Sin plantilla activa en BD -> se envía con el respaldo por defecto del classpath")
    void ITE04_sin_plantilla_activa_usa_respaldo_por_defecto() throws Exception {
        UUID destId = crearUsuarioPrueba("est.ite04@uco.edu.co", true, true);

        jdbcTemplate.update(
                "UPDATE dbo.PlantillaNotificacion SET activa = 0 " +
                        "WHERE tipo = 'RETIRO_ESTUDIANTE_GRUPO' AND idioma = 'es' AND version = 1"
        );
        await().pollDelay(Duration.ofMillis(1300)).until(() -> true);

        outboxNotificacionAdapter.notificar(new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO",
                "Asunto almacenado ignorado",
                "Mensaje almacenado ignorado",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", destId, null, null)),
                Map.of("grupo", "G-DEFAULT", "estudiante", "Carlos", "notif002aIt", "true")
        ));
        dispatcher.signal();

        assertEquals("ENTREGADA", esperarFilaPorDestinatario(destId, "ENTREGADA", 10).get("estado"));
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
        MimeMessage msg = greenMail.getReceivedMessages()[0];
        assertEquals("Retiro del grupo G-DEFAULT", msg.getSubject());
        assertTrue(extraerParteTexto(msg, "text/plain").contains("Se registró el retiro de Carlos del grupo G-DEFAULT."));
    }

    @Test
    @Order(5)
    @DisplayName("IT-E05: Usuario destinatario inhabilitado -> omitido; el usuario activo recibe el correo")
    void ITE05_usuario_inhabilitado_omitido_y_activo_recibe() {
        UUID uInactivo = crearUsuarioPrueba("inactivo.ite05@uco.edu.co", true, false);
        UUID uActivo = crearUsuarioPrueba("activo.ite05@uco.edu.co", true, true);

        outboxNotificacionAdapter.notificar(new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO",
                "Asunto",
                "Mensaje",
                List.of(
                        new Notificacion.Destinatario("ESTUDIANTE", uInactivo, null, null),
                        new Notificacion.Destinatario("DOCENTE", uActivo, null, null)
                ),
                Map.of("grupo", "G-05", "estudiante", "Mario", "notif002aIt", "true")
        ));
        dispatcher.signal();

        assertEquals("ENTREGADA", esperarFilaPorDestinatario(uActivo, "ENTREGADA", 10).get("estado"));
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
        assertNotNull(greenMail.getUserManager().getUserByEmail("activo.ite05@uco.edu.co"),
                "El destinatario activo debe haber recibido el correo en GreenMail");
        org.junit.jupiter.api.Assertions.assertNull(
                greenMail.getUserManager().getUserByEmail("inactivo.ite05@uco.edu.co"),
                "El destinatario inhabilitado debe ser omitido y no recibir RCPT TO en GreenMail"
        );
    }

    @Test
    @Order(6)
    @DisplayName("IT-E06: Ningún destinatario con correo activo -> fila FALLIDA_DEFINITIVA sin reintentos (intentos=1)")
    void ITE06_ningun_destinatario_con_correo_marca_fallida_definitiva() {
        UUID uInactivo = crearUsuarioPrueba("inactivo.ite06@uco.edu.co", true, false);

        outboxNotificacionAdapter.notificar(new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO",
                "Asunto",
                "Mensaje",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", uInactivo, null, null)),
                Map.of("grupo", "G-06", "estudiante", "Nadie", "notif002aIt", "true")
        ));
        dispatcher.signal();

        Map<String, Object> fila = esperarFilaPorDestinatario(uInactivo, "FALLIDA_DEFINITIVA", 10);
        assertNotNull(fila);
        assertEquals("FALLIDA_DEFINITIVA", fila.get("estado"));
        assertEquals(1, ((Number) fila.get("intentos")).intValue());
        assertEquals(0, greenMail.getReceivedMessages().length);
    }

    @Test
    @Order(7)
    @DisplayName("IT-E07: SMTP caído -> reintentos con backoff -> recupera al volver el servidor; fila ENTREGADA")
    void ITE07_smtp_caido_reintenta_y_recupera_al_volver() {
        UUID destId = crearUsuarioPrueba("est.ite07@uco.edu.co", true, true);

        detenerGreenMailTemporalmente();
        try {
            outboxNotificacionAdapter.notificar(new Notificacion(
                    "RETIRO_ESTUDIANTE_GRUPO",
                    "Asunto",
                    "Mensaje",
                    List.of(new Notificacion.Destinatario("ESTUDIANTE", destId, null, null)),
                    Map.of("grupo", "G-07", "estudiante", "Elena", "notif002aIt", "true")
            ));
            dispatcher.signal();

            // Esperar a que falle el primer intento y quede PENDIENTE con intentos >= 1
            await().atMost(Duration.ofSeconds(3))
                    .pollInterval(Duration.ofMillis(100))
                    .until(() -> {
                        Map<String, Object> f = esperarFilaPorDestinatario(destId, "PENDIENTE", 1);
                        return f != null && ((Number) f.get("intentos")).intValue() >= 1;
                    });
        } finally {
            asegurarGreenMailActivo();
        }

        dispatcher.signal();
        Map<String, Object> filaFinal = esperarFilaPorDestinatario(destId, "ENTREGADA", 12);
        assertNotNull(filaFinal);
        assertEquals("ENTREGADA", filaFinal.get("estado"));
        assertTrue(((Number) filaFinal.get("intentos")).intValue() >= 2);
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
    }

    @Test
    @Order(8)
    @DisplayName("IT-E08: Tope de envíos agotado -> filas quedan PENDIENTE con intentos=0; se despachan al liberarse cupo")
    void ITE08_tope_envios_agotado_conserva_pendiente_sin_intentos_y_despacha_al_liberar() {
        UUID destId = crearUsuarioPrueba("est.ite08@uco.edu.co", true, true);

        // Agotar el tope por minuto (60 configurados)
        for (int i = 0; i < 60; i++) {
            assertTrue(topeEnvios.intentarConsumir());
        }
        assertFalse(topeEnvios.disponible());

        outboxNotificacionAdapter.notificar(new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO",
                "Asunto",
                "Mensaje",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", destId, null, null)),
                Map.of("grupo", "G-08", "estudiante", "Tomás", "notif002aIt", "true")
        ));
        dispatcher.signal();
        await().pollDelay(Duration.ofMillis(1200)).until(() -> true);

        Map<String, Object> enEspera = esperarFilaPorDestinatario(destId, "PENDIENTE", 2);
        assertNotNull(enEspera);
        assertEquals("PENDIENTE", enEspera.get("estado"));
        assertEquals(0, ((Number) enEspera.get("intentos")).intValue());
        assertEquals(0, greenMail.getReceivedMessages().length);

        // Liberar cupo y despachar
        topeEnvios.reiniciarVentanas();
        dispatcher.signal();

        Map<String, Object> entregada = esperarFilaPorDestinatario(destId, "ENTREGADA", 10);
        assertNotNull(entregada);
        assertEquals("ENTREGADA", entregada.get("estado"));
        assertEquals(1, ((Number) entregada.get("intentos")).intValue());
        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
    }

    @Test
    @Order(9)
    @DisplayName("IT-E09: 4 avisos de retiro producen correos legibles y distinguibles por grupo (CULM-001 A: tipos SOLICITUD_INGRESO_* retirados)")
    void ITE09_los_4_tipos_producen_correo_legible_y_distinguible() throws Exception {
        UUID d1 = crearUsuarioPrueba("d1.ite09@uco.edu.co", true, true);
        UUID d2 = crearUsuarioPrueba("d2.ite09@uco.edu.co", true, true);
        UUID d3 = crearUsuarioPrueba("d3.ite09@uco.edu.co", true, true);
        UUID d4 = crearUsuarioPrueba("d4.ite09@uco.edu.co", true, true);

        outboxNotificacionAdapter.notificar(new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO", "A1", "M1",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", d1, null, null)),
                Map.of("grupo", "G-901", "estudiante", "Est Uno", "notif002aIt", "true")
        ));
        outboxNotificacionAdapter.notificar(new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO", "A2", "M2",
                List.of(new Notificacion.Destinatario("DOCENTE", d2, null, null)),
                Map.of("grupo", "G-902", "estudiante", "Est Dos", "notif002aIt", "true")
        ));
        outboxNotificacionAdapter.notificar(new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO", "A3", "M3",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", d3, null, null)),
                Map.of("grupo", "G-903", "estudiante", "Est Tres", "notif002aIt", "true")
        ));
        outboxNotificacionAdapter.notificar(new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO", "A4", "M4",
                List.of(new Notificacion.Destinatario("DOCENTE", d4, null, null)),
                Map.of("grupo", "G-904", "estudiante", "Est Cuatro", "notif002aIt", "true")
        ));
        dispatcher.signal();

        assertEquals("ENTREGADA", esperarFilaPorDestinatario(d1, "ENTREGADA", 10).get("estado"));
        assertEquals("ENTREGADA", esperarFilaPorDestinatario(d2, "ENTREGADA", 10).get("estado"));
        assertEquals("ENTREGADA", esperarFilaPorDestinatario(d3, "ENTREGADA", 10).get("estado"));
        assertEquals("ENTREGADA", esperarFilaPorDestinatario(d4, "ENTREGADA", 10).get("estado"));

        assertTrue(greenMail.waitForIncomingEmail(5000, 4));
        List<String> subjects = java.util.Arrays.stream(greenMail.getReceivedMessages())
                .map(m -> {
                    try {
                        return m.getSubject();
                    } catch (Exception _) {
                        return "";
                    }
                })
                .toList();

        assertTrue(subjects.contains("Retiro del grupo G-901"));
        assertTrue(subjects.contains("Retiro del grupo G-902"));
        assertTrue(subjects.contains("Retiro del grupo G-903"));
        assertTrue(subjects.contains("Retiro del grupo G-904"));
    }

    @Test
    @Order(10)
    @DisplayName("IT-E10: Ningún log del sender contiene direcciones de correo ni cuerpo renderizado")
    void ITE10_ningun_log_contiene_correos_ni_cuerpo() {
        String correoSecreto = "privacidad.ite10.unico@uco.edu.co";
        String cuerpoSecreto = "DatoUltraSecretoDeCuerpo987654";
        UUID destId = crearUsuarioPrueba(correoSecreto, true, true);

        outboxNotificacionAdapter.notificar(new Notificacion(
                "RETIRO_ESTUDIANTE_GRUPO",
                "Asunto",
                "Mensaje",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", destId, null, null)),
                Map.of("grupo", "G-10", "estudiante", cuerpoSecreto, "notif002aIt", "true")
        ));
        dispatcher.signal();

        assertEquals("ENTREGADA", esperarFilaPorDestinatario(destId, "ENTREGADA", 10).get("estado"));

        for (ILoggingEvent event : logAppender.list) {
            String msg = event.getFormattedMessage();
            assertFalse(msg.contains(correoSecreto), "El log no debe exponer el correo del destinatario: " + msg);
            assertFalse(msg.contains(cuerpoSecreto), "El log no debe exponer el cuerpo renderizado: " + msg);
        }
    }

    @Test
    @Order(11)
    @DisplayName("IT-E11: Modo de pruebas (GreenMail): solo los buzones de redirect-to reciben el correo; los buzones reales quedan vacíos; asunto [PRUEBA] y aviso con N correcto")
    void ITE11_modo_pruebas_redirige_solo_a_redirect_to_y_omite_reales() throws Exception {
        UUID real1 = crearUsuarioPrueba("real1.ite11@uco.edu.co", true, true);
        UUID real2 = crearUsuarioPrueba("real2.ite11@uco.edu.co", true, true);

        dispatcher.stop();
        try {
            SmtpNotificationProperties testModeProps = new SmtpNotificationProperties(
                    "127.0.0.1",
                    SMTP_PORT,
                    "",
                    "",
                    false,
                    false,
                    "notificaciones@uco.edu.co",
                    "Sistema de Asistencias UCO",
                    java.time.Duration.ofSeconds(10),
                    java.time.Duration.ofSeconds(20),
                    50,
                    60,
                    500,
                    false,
                    "https://asistencias.uco.edu.co",
                    java.time.Duration.ofSeconds(1),
                    new SmtpNotificationProperties.TestMode(true, "qa1.ite11@prueba.test, qa2.ite11@prueba.test")
            );
            SmtpNotificacionChannelSender testModeSender = new SmtpNotificacionChannelSender(
                    contactoResolver,
                    plantillaRenderer,
                    correoTransport,
                    topeEnvios,
                    testModeProps,
                    meterRegistry
            );
            NotificacionOutboxDispatcher testModeDispatcher = new NotificacionOutboxDispatcher(
                    outboxRepository,
                    testModeSender,
                    outboxProperties,
                    meterRegistry,
                    java.time.Clock.systemUTC()
            );

            outboxNotificacionAdapter.notificar(new Notificacion(
                    "RETIRO_ESTUDIANTE_GRUPO",
                    "Asunto respaldo",
                    "Mensaje respaldo",
                    List.of(
                            new Notificacion.Destinatario("ESTUDIANTE", real1, null, null),
                            new Notificacion.Destinatario("DOCENTE", real2, null, null)
                    ),
                    Map.of("grupo", "G-11", "estudiante", "Valeria", "notif002aIt", "true")
            ));

            testModeDispatcher.ejecutarCiclo();

            Map<String, Object> fila = esperarFilaPorDestinatario(real1, "ENTREGADA", 10);
            assertNotNull(fila);
            assertEquals("ENTREGADA", fila.get("estado"));

            // GreenMail recibe 2 copias (1 para qa1 y 1 para qa2 en BCC)
            assertTrue(greenMail.waitForIncomingEmail(5000, 2));
            assertNotNull(greenMail.getUserManager().getUserByEmail("qa1.ite11@prueba.test"),
                    "El buzón de pruebas qa1 debe recibir el correo");
            assertNotNull(greenMail.getUserManager().getUserByEmail("qa2.ite11@prueba.test"),
                    "El buzón de pruebas qa2 debe recibir el correo");
            org.junit.jupiter.api.Assertions.assertNull(
                    greenMail.getUserManager().getUserByEmail("real1.ite11@uco.edu.co"),
                    "El buzón del destinatario real 1 debe quedar vacío"
            );
            org.junit.jupiter.api.Assertions.assertNull(
                    greenMail.getUserManager().getUserByEmail("real2.ite11@uco.edu.co"),
                    "El buzón del destinatario real 2 debe quedar vacío"
            );

            MimeMessage msg = greenMail.getReceivedMessages()[0];
            assertEquals("[PRUEBA] Retiro del grupo G-11", msg.getSubject());
            String textoPlano = extraerParteTexto(msg, "text/plain");
            String html = extraerParteTexto(msg, "text/html");
            String avisoEsperado = "MODO DE PRUEBAS: este correo no se envió a los destinatarios reales. Destinatarios originales omitidos: 2.";
            assertTrue(textoPlano.startsWith(avisoEsperado));
            assertTrue(html.contains("<p>" + avisoEsperado + "</p>"));
            assertFalse(textoPlano.contains("real1.ite11@uco.edu.co"));
            assertFalse(textoPlano.contains("real2.ite11@uco.edu.co"));
            assertFalse(html.contains("real1.ite11@uco.edu.co"));
            assertFalse(html.contains("real2.ite11@uco.edu.co"));
        } finally {
            dispatcher.start();
        }
    }
}
