package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatario;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.DestinatarioRef;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionNoEntregableException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionPersistida;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.ContenidoRenderizado;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.MarcadorDinamicoProvider;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaPorDefectoCatalogo;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaRenderer;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SmtpNotificationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Canal de salida SMTP para el outbox de notificaciones (CONTRACT §4.3).
 * Resuelve contactos al momento de entregar, renderiza plantillas, aplica tope de envios y agrupa en BCC.
 * Nunca registra en logs direcciones de correo, asunto ni cuerpo renderizado (D12/D13).
 */
public class SmtpNotificacionChannelSender implements NotificacionChannelSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpNotificacionChannelSender.class);
    private static final int MAX_SUBJECT_LENGTH = 200;
    private static final String PREFIJO_ASUNTO_PRUEBA = "[PRUEBA] ";
    private static final String METRIC_EMAIL_SEND = "notification.email.send.total";
    private static final String TAG_OUTCOME = "outcome";

    private final ContactoDestinatarioResolver contactoResolver;
    private final PlantillaRenderer plantillaRenderer;
    private final CorreoTransport correoTransport;
    private final TopeEnvios topeEnvios;
    private final SmtpNotificationProperties properties;
    private final MeterRegistry meterRegistry;
    private final MarcadorDinamicoProvider marcadoresDinamicos;

    private final Counter sendSentCounter;
    private final Counter sendRetryCounter;
    private final Counter sendPermanentCounter;
    private final Counter sendNoRecipientsCounter;
    private final Counter recipientsSentCounter;
    private final Counter recipientsOmittedCounter;
    private final Counter redirectedCounter;

    /** Sin marcadores dinamicos: comportamiento previo a IDENT-001. */
    public SmtpNotificacionChannelSender(
            final ContactoDestinatarioResolver contactoResolver,
            final PlantillaRenderer plantillaRenderer,
            final CorreoTransport correoTransport,
            final TopeEnvios topeEnvios,
            final SmtpNotificationProperties properties,
            final MeterRegistry meterRegistry
    ) {
        this(contactoResolver, plantillaRenderer, correoTransport, topeEnvios, properties, meterRegistry,
                MarcadorDinamicoProvider.NINGUNO);
    }

    public SmtpNotificacionChannelSender(
            final ContactoDestinatarioResolver contactoResolver,
            final PlantillaRenderer plantillaRenderer,
            final CorreoTransport correoTransport,
            final TopeEnvios topeEnvios,
            final SmtpNotificationProperties properties,
            final MeterRegistry meterRegistry,
            final MarcadorDinamicoProvider marcadoresDinamicos
    ) {
        this.contactoResolver = Objects.requireNonNull(contactoResolver, "ContactoDestinatarioResolver es obligatorio.");
        this.plantillaRenderer = Objects.requireNonNull(plantillaRenderer, "PlantillaRenderer es obligatorio.");
        this.correoTransport = Objects.requireNonNull(correoTransport, "CorreoTransport es obligatorio.");
        this.topeEnvios = Objects.requireNonNull(topeEnvios, "TopeEnvios es obligatorio.");
        this.properties = Objects.requireNonNull(properties, "SmtpNotificationProperties es obligatorio.");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");
        this.marcadoresDinamicos = Objects.requireNonNull(marcadoresDinamicos, "MarcadorDinamicoProvider es obligatorio.");

        this.sendSentCounter = Counter.builder(METRIC_EMAIL_SEND)
                .tag(TAG_OUTCOME, "sent")
                .register(meterRegistry);
        this.sendRetryCounter = Counter.builder(METRIC_EMAIL_SEND)
                .tag(TAG_OUTCOME, "retry")
                .register(meterRegistry);
        this.sendPermanentCounter = Counter.builder(METRIC_EMAIL_SEND)
                .tag(TAG_OUTCOME, "permanent")
                .register(meterRegistry);
        this.sendNoRecipientsCounter = Counter.builder(METRIC_EMAIL_SEND)
                .tag(TAG_OUTCOME, "no_recipients")
                .register(meterRegistry);
        this.recipientsSentCounter = Counter.builder("notification.email.recipients.total")
                .tag(TAG_OUTCOME, "sent")
                .register(meterRegistry);
        this.recipientsOmittedCounter = Counter.builder("notification.email.recipients.total")
                .tag(TAG_OUTCOME, "omitted")
                .register(meterRegistry);
        this.redirectedCounter = Counter.builder("notification.email.redirected.total")
                .register(meterRegistry);

        if (Boolean.TRUE.equals(this.properties.testMode().enabled())) {
            LOGGER.warn(
                    "Modo de pruebas de correo ACTIVO: los correos se redirigen ({} buzones de prueba configurados).",
                    this.properties.testMode().redirectTo().size()
            );
        }
    }

    @Override
    public boolean disponible() {
        return topeEnvios.disponible();
    }

    @Override
    public void enviar(final NotificacionPersistida notificacion) throws NotificacionNoEntregableException {
        Objects.requireNonNull(notificacion, "NotificacionPersistida es obligatoria.");
        if (!PlantillaPorDefectoCatalogo.TIPOS_CONOCIDOS.contains(notificacion.tipo())) {
            // CULM-001 A.1 (B-RED-09): un tipo retirado (p. ej. SOLICITUD_INGRESO_*) reencolado por el administrador no se
            // entrega con el asunto guardado: es un fallo definitivo que el despachador registra.
            LOGGER.warn("Notificacion de tipo no entregable: idNotificacion={}, tipo={}, outcome=permanent",
                    notificacion.id(), notificacion.tipo());
            throw new NotificacionNoEntregableException("El tipo de notificacion no esta entre los tipos entregables.");
        }

        final List<UUID> idsSolicitados = extraerIdsUsuarios(notificacion.destinatarios());
        final int totalSolicitados = idsSolicitados.size();

        final List<ContactoDestinatario> contactos = idsSolicitados.isEmpty()
                ? List.of()
                : contactoResolver.consultarContactos(idsSolicitados);

        final boolean exentoDeConfirmacion = Notificacion.TIPO_VERIFICACION_CORREO.equals(notificacion.tipo());
        final List<String> correosValidos = filtrarCorreosValidos(contactos, exentoDeConfirmacion);
        final int omitidos = Math.max(0, totalSolicitados - correosValidos.size());
        if (omitidos > 0) {
            recipientsOmittedCounter.increment(omitidos);
        }

        if (correosValidos.isEmpty()) {
            sendNoRecipientsCounter.increment();
            LOGGER.warn(
                    "Notificacion sin destinatarios con correo activo: idNotificacion={}, tipo={}, outcome=no_recipients",
                    notificacion.id(),
                    notificacion.tipo()
            );
            throw new NotificacionNoEntregableException(
                    "Ningun destinatario tiene correo electronico activo y valido."
            );
        }

        // IDENT-001: los marcadores dinamicos (enlace de verificacion) se emiten solo cuando hay a quien enviar.
        final Map<String, String> datos = combinarMarcadoresDinamicos(notificacion, idsSolicitados);

        final boolean modoPrueba = Boolean.TRUE.equals(properties.testMode().enabled());
        final ContenidoRenderizado contenido;
        final String asuntoSaneado;
        final List<String> destinatariosEfectivos;

        if (modoPrueba) {
            final String avisoModoPrueba = "MODO DE PRUEBAS: este correo no se envió a los destinatarios reales. "
                    + "Destinatarios originales omitidos: " + correosValidos.size() + ".";
            contenido = plantillaRenderer.renderizar(
                    notificacion.tipo(),
                    datos,
                    notificacion.asunto(),
                    notificacion.mensaje(),
                    avisoModoPrueba
            );
            asuntoSaneado = sanearAsunto(PREFIJO_ASUNTO_PRUEBA + sanearAsunto(contenido.asunto()));
            destinatariosEfectivos = properties.testMode().redirectTo();
        } else {
            contenido = plantillaRenderer.renderizar(
                    notificacion.tipo(),
                    datos,
                    notificacion.asunto(),
                    notificacion.mensaje()
            );
            asuntoSaneado = sanearAsunto(contenido.asunto());
            destinatariosEfectivos = correosValidos;
        }

        final List<List<String>> bloquesBcc = partirEnBloques(destinatariosEfectivos, properties.maxBcc());
        for (int idx = 0; idx < bloquesBcc.size(); idx++) {
            if (!topeEnvios.intentarConsumir()) {
                sendRetryCounter.increment();
                throw new IllegalStateException("Tope de envios SMTP agotado antes de enviar bloque.");
            }
            final List<String> bloque = bloquesBcc.get(idx);
            final String messageId = "<notif-" + notificacion.id() + "-" + idx + "@uco.edu.co>";
            final CorreoSaliente correo = new CorreoSaliente(
                    properties.fromAddress(),
                    properties.fromName(),
                    properties.fromAddress(),
                    bloque,
                    asuntoSaneado,
                    contenido.textoPlano(),
                    contenido.html(),
                    messageId
            );

            final long inicioNanos = System.nanoTime();
            try {
                correoTransport.enviar(correo);
                registrarDuracion("sent", System.nanoTime() - inicioNanos);
                sendSentCounter.increment();
                recipientsSentCounter.increment(bloque.size());
                if (modoPrueba) {
                    redirectedCounter.increment();
                }
            } catch (final NotificacionNoEntregableException ex) {
                registrarDuracion("permanent", System.nanoTime() - inicioNanos);
                sendPermanentCounter.increment();
                LOGGER.warn(
                        "Fallo permanente SMTP al enviar notificacion: idNotificacion={}, tipo={}, outcome=permanent",
                        notificacion.id(),
                        notificacion.tipo()
                );
                throw ex;
            } catch (final RuntimeException ex) {
                registrarDuracion("retry", System.nanoTime() - inicioNanos);
                sendRetryCounter.increment();
                LOGGER.warn(
                        "Fallo transitorio SMTP al enviar notificacion: idNotificacion={}, tipo={}, outcome=retry, causa={}",
                        notificacion.id(),
                        notificacion.tipo(),
                        ex.getCause() != null ? ex.getCause().getClass().getName() : ex.getClass().getName()
                );
                throw ex;
            }
        }

        LOGGER.info(
                "Notificacion enviada por SMTP: idNotificacion={}, tipo={}, destinatarios={}, origenPlantilla={}, outcome=sent",
                notificacion.id(),
                notificacion.tipo(),
                correosValidos.size(),
                contenido.origen()
        );
    }

    private Map<String, String> combinarMarcadoresDinamicos(final NotificacionPersistida notificacion, final List<UUID> idsUsuarios)
            throws NotificacionNoEntregableException {
        final Map<String, String> dinamicos = marcadoresDinamicos.resolver(notificacion.tipo(), idsUsuarios);
        if (dinamicos == null || dinamicos.isEmpty()) {
            return notificacion.datos();
        }
        final Map<String, String> combinados = new HashMap<>(notificacion.datos());
        combinados.putAll(dinamicos);
        return Map.copyOf(combinados);
    }

    private List<UUID> extraerIdsUsuarios(final List<DestinatarioRef> destinatarios) {
        final Set<UUID> unicos = new LinkedHashSet<>();
        for (final DestinatarioRef ref : destinatarios) {
            if (ref.idUsuario() != null) {
                unicos.add(ref.idUsuario());
            }
        }
        return List.copyOf(unicos);
    }

    private List<String> filtrarCorreosValidos(final List<ContactoDestinatario> contactos, final boolean exentoDeConfirmacion) {
        if (contactos == null || contactos.isEmpty()) {
            return List.of();
        }
        final Set<String> validos = new LinkedHashSet<>();
        for (final ContactoDestinatario c : contactos) {
            if (c != null
                    && (exentoDeConfirmacion || !Boolean.TRUE.equals(properties.requireConfirmedEmail()) || c.correoConfirmado())
                    && esCorreoSeguro(c.correo())) {
                validos.add(c.correo().trim());
            }
        }
        return List.copyOf(validos);
    }

    static boolean esCorreoSeguro(final String correo) {
        return SmtpNotificationProperties.esCorreoSeguro(correo);
    }

    static String sanearAsunto(final String asunto) {
        if (asunto == null) {
            return "";
        }
        final String limpio = asunto.replace("\r", " ").replace("\n", " ").trim();
        if (limpio.length() > MAX_SUBJECT_LENGTH) {
            return limpio.substring(0, MAX_SUBJECT_LENGTH);
        }
        return limpio;
    }

    private static List<List<String>> partirEnBloques(final List<String> elementos, final int tamanoMaximo) {
        final List<List<String>> bloques = new ArrayList<>();
        for (int i = 0; i < elementos.size(); i += tamanoMaximo) {
            bloques.add(elementos.subList(i, Math.min(elementos.size(), i + tamanoMaximo)));
        }
        return bloques;
    }

    private void registrarDuracion(final String outcome, final long nanos) {
        Timer.builder("notification.email.send.duration")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }
}
