package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.autoconfigure.MessageUcoLabAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Evidencia contra el MessageUcoLab REAL (starter oficial + HTTP real a traves del gateway). Opt-in: se omite salvo que
 * existan {@code MESSAGEUCOLAB_LIVE_APPLICATION_ID} y {@code MESSAGEUCOLAB_LIVE_ENVIRONMENT_ID} (y opcionalmente
 * {@code MESSAGEUCOLAB_LIVE_URL}, por defecto {@code http://localhost:8000}).
 *
 * <p>Requiere en el ambiente indicado los mensajes MSG_GEN001, MSG_GEN003 y MSG_VAL001, y que MSG_VAL005 no exista.
 * Un mock no certifica el componente externo; esta prueba si. Un {@code NOT_RUN} no es {@code PASS}.</p>
 */
class MessageUcoLabLiveIT {

    private static final String APP = System.getenv("MESSAGEUCOLAB_LIVE_APPLICATION_ID");
    private static final String ENV = System.getenv("MESSAGEUCOLAB_LIVE_ENVIRONMENT_ID");
    private static final String URL = System.getenv().getOrDefault("MESSAGEUCOLAB_LIVE_URL", "http://localhost:8000");

    private MessageCatalogPort adapter(final ApplicationContextRunnerHolder holder, final String environmentId) {
        return holder.build(environmentId);
    }

    /** Construye el adaptador sobre el MessageReader real que registra el starter. */
    private static final class ApplicationContextRunnerHolder {
        MessageCatalogPort build(final String environmentId) {
            final MessageCatalogPort[] holder = new MessageCatalogPort[1];
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(WebClientAutoConfiguration.class, MessageUcoLabAutoConfiguration.class))
                    .withPropertyValues(
                            "messageucolab.url=" + URL,
                            "messageucolab.application-id=" + APP,
                            "messageucolab.environment-id=" + environmentId)
                    .run(context -> holder[0] = new MessageUcoLabMessageCatalogAdapter(
                            context.getBean(MessageReader.class),
                            new MessageUcoLabCodeMapper("MSG_", Map.of()),
                            Duration.ofSeconds(10),
                            Duration.ZERO,
                            null));
            return holder[0];
        }
    }

    private static void requireLive() {
        assumeTrue(APP != null && !APP.isBlank() && ENV != null && !ENV.isBlank(),
                "MESSAGEUCOLAB_LIVE_APPLICATION_ID / MESSAGEUCOLAB_LIVE_ENVIRONMENT_ID no definidos: NOT_RUN");
    }

    @Test
    void resuelve_un_mensaje_real_con_su_marcador_sin_reemplazar() {
        requireLive();

        final MessageCatalogPort port = adapter(new ApplicationContextRunnerHolder(), ENV);

        assertEquals(Optional.of("El {} especificado no existe en el sistema."), port.findUserMessage("GEN_001"));
        assertEquals(Optional.of("El número de identificación es obligatorio."), port.findUserMessage("VAL_001"));
    }

    @Test
    void resuelve_un_mensaje_importado_en_bloque_desde_excel() {
        requireLive();

        final MessageCatalogPort port = adapter(new ApplicationContextRunnerHolder(), ENV);

        assertEquals(Optional.of("Los nombres y apellidos son obligatorios."), port.findUserMessage("VAL_003"));
        assertEquals(Optional.of("El identificador de correlación no está presente o está vacío."),
                port.findUserMessage("CORR_001"));
    }

    @Test
    void aplica_los_argumentos_posicionales_al_texto_real() {
        requireLive();

        final MessageCatalogPort port = adapter(new ApplicationContextRunnerHolder(), ENV);

        assertEquals("Ya existe un usuario registrado con este correo.", port.getUserMessage("GEN_003", "usuario", "correo"));
    }

    @Test
    void un_codigo_inexistente_en_messageucolab_no_lanza_y_sin_contingencia_devuelve_vacio() {
        requireLive();

        final MessageCatalogPort port = adapter(new ApplicationContextRunnerHolder(), ENV);

        assertEquals(Optional.empty(), port.findUserMessage("VAL_005"));
        assertEquals("VAL_005", port.getUserMessage("VAL_005"));
    }

    @Test
    void un_ambiente_inexistente_degrada_sin_lanzar_excepcion() {
        requireLive();

        final MessageCatalogPort port = adapter(new ApplicationContextRunnerHolder(), UUID.randomUUID().toString());

        assertEquals(Optional.empty(), port.findUserMessage("GEN_001"));
    }

    @Test
    void un_codigo_canonico_largo_sin_alias_no_se_consulta() {
        requireLive();

        final MessageCatalogPort port = adapter(new ApplicationContextRunnerHolder(), ENV);

        assertEquals(Optional.empty(), port.findUserMessage("ERR_CRUCE_HORARIO_DOCENTE"));
    }
}
