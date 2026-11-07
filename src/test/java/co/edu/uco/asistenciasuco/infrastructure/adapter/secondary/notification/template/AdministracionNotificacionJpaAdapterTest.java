package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.TipoPlantillaAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VersionPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VistaPreviaPlantillaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxDispatcher;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SmtpNotificationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdministracionNotificacionJpaAdapterTest {

    private EntityManager entityManager;
    private PlantillaNotificacionJpaRepository plantillaRepository;
    private PlantillaPorDefectoCatalogo catalogoDefecto;
    private PlantillaRenderer plantillaRenderer;
    private NotificationAdapterProperties adapterProperties;
    private OutboxNotificationProperties outboxProperties;
    private ObjectProvider<SmtpNotificationProperties> smtpPropsProvider;
    private ObjectProvider<NotificacionChannelSender> channelSenderProvider;
    private ObjectProvider<NotificacionOutboxDispatcher> dispatcherProvider;
    private NotificacionPort notificacionPort;
    private NotificacionChannelSender channelSender;
    private NotificacionOutboxDispatcher dispatcher;

    private AdministracionNotificacionJpaAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        entityManager = mock(EntityManager.class);
        plantillaRepository = mock(PlantillaNotificacionJpaRepository.class);
        catalogoDefecto = new PlantillaPorDefectoCatalogo();
        plantillaRenderer = new PlantillaRenderer(plantillaRepository, catalogoDefecto, new SimpleMeterRegistry(), "https://asistencias.uco.edu.co");
        adapterProperties = new NotificationAdapterProperties(NotificationAdapterProperties.Provider.OUTBOX, null);
        outboxProperties = new OutboxNotificationProperties(
                OutboxNotificationProperties.Sender.SMTP,
                true,
                50,
                Duration.ofSeconds(60),
                Duration.ofSeconds(30),
                5,
                Duration.ofSeconds(30),
                Duration.ofMinutes(15),
                30,
                Duration.ofHours(6),
                "inst-01"
        );
        smtpPropsProvider = mock(ObjectProvider.class);
        channelSenderProvider = mock(ObjectProvider.class);
        dispatcherProvider = mock(ObjectProvider.class);
        notificacionPort = mock(NotificacionPort.class);
        channelSender = mock(NotificacionChannelSender.class);
        dispatcher = mock(NotificacionOutboxDispatcher.class);

        when(channelSenderProvider.getIfAvailable()).thenReturn(channelSender);
        when(channelSender.disponible()).thenReturn(true);
        when(dispatcherProvider.getIfAvailable()).thenReturn(dispatcher);

        adapter = new AdministracionNotificacionJpaAdapter(
                entityManager,
                plantillaRepository,
                catalogoDefecto,
                plantillaRenderer,
                adapterProperties,
                outboxProperties,
                smtpPropsProvider,
                channelSenderProvider,
                dispatcherProvider,
                notificacionPort
        );
    }

    private StoredProcedureQuery mockSpQuery(final String spName) {
        final StoredProcedureQuery sp = mock(StoredProcedureQuery.class);
        when(entityManager.createStoredProcedureQuery(spName)).thenReturn(sp);
        when(sp.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(sp);
        when(sp.setParameter(anyString(), any())).thenReturn(sp);
        return sp;
    }

    @Test
    @DisplayName("consultarEstadoCanal: retorna datos de SMTP cuando esta configurado")
    void consultarEstadoCanal_conSmtpConfigurado() {
        final SmtpNotificationProperties smtp = new SmtpNotificationProperties(
                "smtp.uco.edu.co",
                587,
                "user",
                "secret-pass",
                true,
                false,
                "asistencias@uco.edu.co",
                "Asistencias UCO",
                Duration.ofSeconds(10),
                Duration.ofSeconds(20),
                50,
                60,
                500,
                true,
                "https://asistencias.uco.edu.co",
                Duration.ofSeconds(60),
                new SmtpNotificationProperties.TestMode(true, List.of("test@uco.edu.co"))
        );
        when(smtpPropsProvider.getIfAvailable()).thenReturn(smtp);

        final EstadoCanalNotificacionProjection estado = adapter.consultarEstadoCanal();
        assertEquals("outbox", estado.provider());
        assertEquals("smtp", estado.sender());
        assertTrue(estado.dispatcherEnabled());
        assertTrue(estado.disponible());
        assertEquals("smtp.uco.edu.co", estado.host());
        assertEquals(587, estado.port());
        assertEquals("user", estado.username());
        assertTrue(estado.tienePasswordConfigurado());
        assertEquals("asistencias@uco.edu.co", estado.fromAddress());
        assertTrue(estado.starttls());
        assertFalse(estado.ssl());
        assertTrue(estado.requireConfirmedEmail());
        assertTrue(estado.testModeEnabled());
        assertEquals(1, estado.testModeRedirectCount());
    }

    @Test
    @DisplayName("consultarPlantillas: usa catalogo de respaldo si el SP falla o no existe")
    void consultarPlantillas_fallbackCatalogo() {
        when(entityManager.createStoredProcedureQuery("dbo.usp_consultar_plantillas_notificacion_admin"))
                .thenThrow(new RuntimeException("SP no encontrado"));

        final List<TipoPlantillaAdminProjection> plantillas = adapter.consultarPlantillas("es", UUID.randomUUID());
        assertFalse(plantillas.isEmpty());
        assertTrue(plantillas.stream().anyMatch(p -> "RETIRO_ESTUDIANTE_GRUPO".equals(p.tipo())));
        assertTrue(plantillas.stream().anyMatch(p -> "RECLAMO_ASISTENCIA_RADICADO".equals(p.tipo())));
        assertTrue(plantillas.stream().anyMatch(p -> "RECLAMO_ASISTENCIA_RESUELTO".equals(p.tipo())));
        assertTrue(plantillas.stream().anyMatch(p -> "ALERTA_RIESGO_INASISTENCIA".equals(p.tipo())));
        assertTrue(plantillas.stream().anyMatch(p -> "ALERTA_PERDIDA_INASISTENCIA".equals(p.tipo())));
        assertTrue(plantillas.stream().anyMatch(p -> "CIERRE_MASIVO_PERIODO_EJECUTADO".equals(p.tipo())));
    }

    @Test
    @DisplayName("crearVersionPlantilla: ejecuta SP e invalida cache del repositorio")
    void crearVersionPlantilla_ejecutaEInvalidaCache() {
        final StoredProcedureQuery sp = mockSpQuery("dbo.usp_guardar_plantilla_notificacion");
        final UUID corrId = UUID.randomUUID();
        final Object[] canonicalOk = {corrId, "OK", "DBCODE=GEN_003|OK", Boolean.TRUE};
        final UUID versionId = UUID.randomUUID();
        final Object[] versionRow = {versionId, "RETIRO_ESTUDIANTE_GRUPO", "es", 2, "Asunto v2", "Cuerpo v2", Boolean.FALSE, Instant.now()};

        when(sp.execute()).thenReturn(true);
        when(sp.hasMoreResults()).thenReturn(true, false);
        when(sp.getResultList())
                .thenReturn(List.of((Object) canonicalOk))
                .thenReturn(List.of((Object) versionRow));

        final UUID adminId = UUID.randomUUID();
        final VersionPlantillaProjection version = adapter.crearVersionPlantilla(
                "RETIRO_ESTUDIANTE_GRUPO", "es", "Asunto v2", "Cuerpo v2", adminId
        );

        assertNotNull(version);
        assertEquals(2, version.version());
        assertEquals("Asunto v2", version.asunto());
        verify(plantillaRepository).invalidarCache();
    }

    @Test
    @DisplayName("activarVersionPlantilla: ejecuta SP e invalida cache del repositorio")
    void activarVersionPlantilla_ejecutaEInvalidaCache() {
        final StoredProcedureQuery sp = mockSpQuery("dbo.usp_activar_version_plantilla_notificacion");
        final UUID corrId = UUID.randomUUID();
        final Object[] canonicalOk = {corrId, "OK", "DBCODE=GEN_003|OK", Boolean.TRUE};

        when(sp.execute()).thenReturn(true);
        when(sp.getResultList()).thenReturn(List.of((Object) canonicalOk));

        final UUID adminId = UUID.randomUUID();
        adapter.activarVersionPlantilla("RETIRO_ESTUDIANTE_GRUPO", "es", 2, adminId);

        verify(plantillaRepository).invalidarCache();
    }

    @Test
    @DisplayName("generarVistaPrevia: renderiza asunto y cuerpo con marcadores de muestra")
    void generarVistaPrevia_renderizaPlantilla() {
        final VistaPreviaPlantillaProjection prev = adapter.generarVistaPrevia(
                "RETIRO_ESTUDIANTE_GRUPO", "es", "Retiro del {grupo}", "Estimado {estudiante}, se retiro."
        );
        assertNotNull(prev);
        assertTrue(prev.asunto().contains("G01 de Ingeniería de Software"));
        assertTrue(prev.textoPlano().contains("Ana María Gómez"));
        assertTrue(prev.html().contains("Ana María Gómez"));
        assertTrue(prev.html().contains("Ir a la plataforma"));
    }

    @Test
    @DisplayName("consultarCola: ejecuta SP y mapea pagina y resumen")
    void consultarCola_mapeaCorrectamente() {
        final StoredProcedureQuery sp = mockSpQuery("dbo.usp_consultar_notificaciones_salientes_admin");
        final UUID corrId = UUID.randomUUID();
        final Object[] canonicalOk = {corrId, "OK", "DBCODE=GEN_003|OK", Boolean.TRUE};
        final UUID notifId = UUID.randomUUID();
        final Object[] itemRow = {
                notifId, "RETIRO_ESTUDIANTE_GRUPO", "Asunto", "PENDIENTE", "Pendiente",
                Boolean.FALSE, 2, 0, Instant.now(), null, null, null, null, corrId
        };
        final Object[] metaRow = {1L, 1};
        final Object[] sumRow = {1L, 0L, 5L, 0L, 0L};

        when(sp.execute()).thenReturn(true);
        when(sp.hasMoreResults()).thenReturn(true, true, true, false);
        when(sp.getResultList())
                .thenReturn(List.of((Object) canonicalOk))
                .thenReturn(List.of((Object) itemRow))
                .thenReturn(List.of((Object) metaRow))
                .thenReturn(List.of((Object) sumRow));

        final PaginaColaNotificacionProjection pagina = adapter.consultarCola(null, null, 1, 20, UUID.randomUUID());
        assertEquals(1, pagina.totalElementos());
        assertEquals(1, pagina.elementos().size());
        assertEquals(notifId, pagina.elementos().getFirst().id());
        assertEquals(corrId, pagina.elementos().getFirst().referenciaTraza());
        assertEquals(1, pagina.resumen().pendientes());
        assertEquals(5, pagina.resumen().entregadas());
    }

    @Test
    @DisplayName("reintentarNotificacion: ejecuta SP y despierta despachador")
    void reintentarNotificacion_ejecutaYDespierta() {
        final StoredProcedureQuery sp = mockSpQuery("dbo.usp_reintentar_notificacion_saliente");
        final UUID corrId = UUID.randomUUID();
        final Object[] canonicalOk = {corrId, "OK", "DBCODE=GEN_003|OK", Boolean.TRUE};

        when(sp.execute()).thenReturn(true);
        when(sp.getResultList()).thenReturn(List.of((Object) canonicalOk));

        final UUID notifId = UUID.randomUUID();
        final UUID adminId = UUID.randomUUID();
        adapter.reintentarNotificacion(notifId, adminId);

        verify(dispatcher).despertar();
    }

    @Test
    @DisplayName("enviarCorreoPrueba: emite notificacion de prueba y despierta despachador")
    void enviarCorreoPrueba_notificaYDespierta() {
        final UUID adminId = UUID.randomUUID();
        adapter.enviarCorreoPrueba(adminId, "admin@uco.edu.co");

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        assertEquals("PRUEBA_CANAL_NOTIFICACION", captor.getValue().tipo());
        assertEquals("admin@uco.edu.co", captor.getValue().destinatarios().getFirst().correo());
        verify(dispatcher).despertar();
    }

    @Test
    @DisplayName("enviarCorreoPrueba: la verificacion de correo viaja como tipo de prueba, sin emitir token real")
    void enviarCorreoPrueba_verificacionUsaTipoDePrueba() {
        adapter.enviarCorreoPrueba(UUID.randomUUID(), "VERIFICACION_CORREO", "admin@uco.edu.co");

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        assertEquals("PRUEBA_VERIFICACION_CORREO", captor.getValue().tipo());
    }

    // ---- NOTIF-002B: cobertura de ramas de mapeo, errores y respaldos ----

    private static Object[] canonico(final UUID corrId, final boolean exitoso, final String codigo) {
        return new Object[]{corrId, "mensaje", "DBCODE=" + codigo + "|detalle", exitoso};
    }

    private AdministracionNotificacionJpaAdapter adapterSinConfiguracion() {
        final ObjectProvider<SmtpNotificationProperties> sinSmtp = mock(ObjectProvider.class);
        final ObjectProvider<NotificacionChannelSender> sinSender = mock(ObjectProvider.class);
        final ObjectProvider<NotificacionOutboxDispatcher> sinDispatcher = mock(ObjectProvider.class);
        return new AdministracionNotificacionJpaAdapter(
                entityManager, plantillaRepository, catalogoDefecto, plantillaRenderer,
                null, null, sinSmtp, sinSender, sinDispatcher, notificacionPort);
    }

    @Test
    @DisplayName("consultarEstadoCanal: sin propiedades ni SMTP devuelve valores por defecto")
    void consultarEstadoCanal_sinConfiguracion() {
        final EstadoCanalNotificacionProjection estado = adapterSinConfiguracion().consultarEstadoCanal();
        assertEquals("outbox", estado.provider());
        assertEquals("log", estado.sender());
        assertFalse(estado.dispatcherEnabled());
        assertFalse(estado.disponible());
        assertEquals(587, estado.port());
        assertFalse(estado.tienePasswordConfigurado());
        assertEquals(0, estado.testModeRedirectCount());
    }

    @Test
    @DisplayName("consultarEstadoCanal: SMTP con campos opcionales nulos usa valores por defecto")
    void consultarEstadoCanal_smtpConCamposNulos() {
        final SmtpNotificationProperties smtp = mock(SmtpNotificationProperties.class);
        when(smtpPropsProvider.getIfAvailable()).thenReturn(smtp);
        when(smtp.password()).thenReturn(" ");
        when(smtp.port()).thenReturn(null);
        when(smtp.maxBcc()).thenReturn(null);
        when(smtp.ratePerMinute()).thenReturn(null);
        when(smtp.ratePerDay()).thenReturn(null);
        when(smtp.starttls()).thenReturn(null);

        final EstadoCanalNotificacionProjection estado = adapter.consultarEstadoCanal();

        assertFalse(estado.tienePasswordConfigurado());
        assertEquals(587, estado.port());
        assertEquals(50, estado.maxBcc());
        assertEquals(60, estado.ratePerMinute());
        assertEquals(500, estado.ratePerDay());
        assertFalse(estado.testModeEnabled());
        assertFalse(estado.starttls());
    }

    @Test
    @DisplayName("consultarPlantillas: mapea tipos, marcadores y versiones del SP")
    void consultarPlantillas_mapeaResultadosDelSp() {
        final StoredProcedureQuery sp = mockSpQuery("dbo.usp_consultar_plantillas_notificacion_admin");
        final UUID corrId = UUID.randomUUID();
        final UUID versionId = UUID.randomUUID();
        final Object[] tipo = {"RETIRO_ESTUDIANTE_GRUPO", "es", 1, null, null, null};
        final Object[] marcador = {"RETIRO_ESTUDIANTE_GRUPO", "grupo", null, null, Boolean.TRUE};
        final Object[] version = {versionId, "RETIRO_ESTUDIANTE_GRUPO", "es", 1, "Asunto", "Cuerpo", Boolean.TRUE};

        when(sp.execute()).thenReturn(true);
        when(sp.hasMoreResults()).thenReturn(true, true, true, false);
        when(sp.getResultList())
                .thenReturn(List.of((Object) canonico(corrId, true, "GEN_004")))
                .thenReturn(List.of((Object) tipo))
                .thenReturn(List.of((Object) marcador))
                .thenReturn(List.of((Object) version));

        final List<TipoPlantillaAdminProjection> plantillas = adapter.consultarPlantillas(null, UUID.randomUUID());

        assertEquals(1, plantillas.size());
        assertEquals("", plantillas.getFirst().asuntoActivo());
        assertEquals(1, plantillas.getFirst().marcadores().size());
        assertTrue(plantillas.getFirst().marcadores().getFirst().obligatorio());
        assertEquals(versionId, plantillas.getFirst().versiones().getFirst().id());
    }

    @Test
    @DisplayName("consultarPlantillas: un error de negocio del SP no se oculta con el respaldo")
    void consultarPlantillas_errorDeNegocioSePropaga() {
        final StoredProcedureQuery sp = mockSpQuery("dbo.usp_consultar_plantillas_notificacion_admin");
        when(sp.execute()).thenReturn(true);
        when(sp.getResultList()).thenReturn(List.of((Object) canonico(UUID.randomUUID(), false, "SEC_001")));

        assertThrows(co.edu.uco.asistenciasuco.application.exception.ApplicationException.class,
                () -> adapter.consultarPlantillas("es", UUID.randomUUID()));
    }

    @Test
    @DisplayName("crearVersionPlantilla: sin fila de version devuelve una proyeccion v2 por defecto")
    void crearVersionPlantilla_sinFilaDevuelveDefecto() {
        final StoredProcedureQuery sp = mockSpQuery("dbo.usp_guardar_plantilla_notificacion");
        when(sp.execute()).thenReturn(true);
        when(sp.hasMoreResults()).thenReturn(false);
        when(sp.getResultList()).thenReturn(List.of((Object) canonico(UUID.randomUUID(), true, "NTF_006")));

        final VersionPlantillaProjection version = adapter.crearVersionPlantilla(" T ", null, " a ", " c ", UUID.randomUUID());

        assertEquals(2, version.version());
        assertEquals("T", version.tipo());
        assertEquals("es", version.idioma());
        verify(plantillaRepository).invalidarCache();
    }

    @Test
    @DisplayName("crearVersionPlantilla y activar: validan argumentos y envuelven fallos de persistencia")
    void crearYActivar_validanYEnvuelvenFallos() {
        assertThrows(NullPointerException.class, () -> adapter.crearVersionPlantilla(null, "es", "a", "c", UUID.randomUUID()));
        assertThrows(NullPointerException.class, () -> adapter.crearVersionPlantilla("T", "es", null, "c", UUID.randomUUID()));
        assertThrows(NullPointerException.class, () -> adapter.crearVersionPlantilla("T", "es", "a", null, UUID.randomUUID()));
        assertThrows(NullPointerException.class, () -> adapter.activarVersionPlantilla(null, "es", 1, UUID.randomUUID()));

        final StoredProcedureQuery guardar = mockSpQuery("dbo.usp_guardar_plantilla_notificacion");
        when(guardar.execute()).thenThrow(new jakarta.persistence.PersistenceException("caido"));
        assertThrows(DatabaseOperationException.class,
                () -> adapter.crearVersionPlantilla("T", "es", "a", "c", UUID.randomUUID()));

        final StoredProcedureQuery activar = mockSpQuery("dbo.usp_activar_version_plantilla_notificacion");
        when(activar.execute()).thenThrow(new IllegalStateException("caido"));
        assertThrows(DatabaseOperationException.class,
                () -> adapter.activarVersionPlantilla("T", null, 1, UUID.randomUUID()));
    }

    @Test
    @DisplayName("generarVistaPrevia: un tipo sin plantilla por defecto usa los datos basicos de muestra")
    void generarVistaPrevia_tipoDesconocido() {
        final VistaPreviaPlantillaProjection prev = adapter.generarVistaPrevia(null, null, "Hola {estudiante}", "{grupo}");
        assertTrue(prev.asunto().contains("Ana María Gómez"));
        assertTrue(prev.textoPlano().contains("G01 de Ingeniería de Software"));
    }

    @Test
    @DisplayName("consultarCola: acepta distintos tipos de fecha, columnas ausentes y limites de pagina")
    void consultarCola_fechasYLimites() {
        final StoredProcedureQuery sp = mockSpQuery("dbo.usp_consultar_notificaciones_salientes_admin");
        final UUID corrId = UUID.randomUUID();
        final Object[] corto = {corrId, "A", "B", "ENTREGADA"};
        final Object[] completo = {UUID.randomUUID().toString(), "T", "A", "FALLIDA", "Fallida", Boolean.TRUE, 1, 2,
                java.sql.Timestamp.from(Instant.parse("2026-11-06T10:00:00Z")),
                java.time.LocalDateTime.of(2026, 11, 6, 10, 0),
                java.time.OffsetDateTime.parse("2026-11-06T10:00:00Z"),
                "FALLO", "detalle", UUID.randomUUID().toString()};
        final Object[] textoSql = {UUID.randomUUID(), "T", "A", "PENDIENTE", null, 1, 0, 0,
                "2026-11-06 20:33:02.7760222 +00:00", java.util.Date.from(Instant.parse("2026-11-06T10:00:00Z")),
                "2026-11-06T10:00:00Z", null, null, null};
        final Object[] meta = {3L};
        final Object[] resumenCorto = {2L};

        when(sp.execute()).thenReturn(true);
        when(sp.hasMoreResults()).thenReturn(true, true, true, false);
        when(sp.getResultList())
                .thenReturn(List.of((Object) canonico(corrId, true, "GEN_004")))
                .thenReturn(List.of((Object) corto, (Object) completo, (Object) textoSql))
                .thenReturn(List.of((Object) meta))
                .thenReturn(List.of((Object) resumenCorto));

        final PaginaColaNotificacionProjection pagina = adapter.consultarCola("  ", "T", -5, 500, UUID.randomUUID());

        assertEquals(3, pagina.elementos().size());
        assertEquals(1, pagina.numeroPagina());
        assertEquals(20, pagina.tamanoPagina());
        assertEquals(3, pagina.totalElementos());
        assertEquals(1, pagina.totalPaginas() > 0 ? 1 : 0);
        assertEquals(2, pagina.resumen().pendientes());
        assertEquals(0, pagina.resumen().entregadas());
        assertEquals("ENTREGADA", pagina.elementos().getFirst().nombreEstado());
        assertFalse(pagina.elementos().getFirst().esTerminal());
        assertTrue(pagina.elementos().get(1).esTerminal());
        assertEquals(Instant.parse("2026-11-06T20:33:02.776022200Z"), pagina.elementos().get(2).creadoEn());
    }

    @Test
    @DisplayName("consultarCola: sin filas de resumen ni metadatos usa valores vacios; un fallo se envuelve")
    void consultarCola_sinMetadatosYConFallo() {
        final StoredProcedureQuery sp = mockSpQuery("dbo.usp_consultar_notificaciones_salientes_admin");
        when(sp.execute()).thenReturn(true);
        when(sp.hasMoreResults()).thenReturn(false);
        when(sp.getResultList()).thenReturn(List.of((Object) canonico(UUID.randomUUID(), true, "GEN_004")));

        final PaginaColaNotificacionProjection vacia = adapter.consultarCola(null, null, 1, 0, UUID.randomUUID());
        assertTrue(vacia.elementos().isEmpty());
        assertEquals(20, vacia.tamanoPagina());
        assertEquals(0, vacia.resumen().pendientes());

        when(sp.execute()).thenThrow(new IllegalArgumentException("mal"));
        assertThrows(DatabaseOperationException.class,
                () -> adapter.consultarCola(null, null, 1, 20, UUID.randomUUID()));
    }

    @Test
    @DisplayName("reintentarNotificacion: valida el id, envuelve fallos y tolera la ausencia de despachador")
    void reintentarNotificacion_validaciones() {
        assertThrows(NullPointerException.class, () -> adapter.reintentarNotificacion(null, UUID.randomUUID()));

        final StoredProcedureQuery sp = mockSpQuery("dbo.usp_reintentar_notificacion_saliente");
        when(sp.execute()).thenThrow(new jakarta.persistence.PersistenceException("caido"));
        assertThrows(DatabaseOperationException.class,
                () -> adapter.reintentarNotificacion(UUID.randomUUID(), UUID.randomUUID()));

        final StoredProcedureQuery ok = mockSpQuery("dbo.usp_reintentar_notificacion_saliente");
        when(ok.execute()).thenReturn(true);
        when(ok.getResultList()).thenReturn(List.of((Object) canonico(UUID.randomUUID(), true, "NTF_007")));
        final AdministracionNotificacionJpaAdapter sinDespachador = adapterSinConfiguracion();
        sinDespachador.reintentarNotificacion(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    @DisplayName("enviarCorreoPrueba: con tipo y correo en blanco envia sin destino y sin despachador")
    void enviarCorreoPrueba_conTipoYCorreoEnBlanco() {
        final AdministracionNotificacionJpaAdapter sinDespachador = adapterSinConfiguracion();

        sinDespachador.enviarCorreoPrueba(UUID.randomUUID(), "RETIRO_ESTUDIANTE_GRUPO", "  ");

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        assertEquals("RETIRO_ESTUDIANTE_GRUPO", captor.getValue().tipo());
        assertNull(captor.getValue().destinatarios().getFirst().correo());
    }
}
