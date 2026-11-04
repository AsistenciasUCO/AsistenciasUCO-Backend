package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.autoconfigure.MessageUcoLabAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Evidencia contra el MessageUcoLab REAL (starter oficial + HTTP real a traves del gateway). Opt-in: se omite salvo que
 * existan {@code MESSAGEUCOLAB_LIVE_APPLICATION_ID} y {@code MESSAGEUCOLAB_LIVE_ENVIRONMENT_ID} (y opcionalmente
 * {@code MESSAGEUCOLAB_LIVE_URL}, por defecto {@code http://localhost:8000}).
 *
 * <p>Requiere en el ambiente indicado el catalogo de usuario completo importado desde el Excel (los mensajes de mas de 100
 * caracteres solo si el componente ya admite mas).
 * Un mock no certifica el componente externo; esta prueba si. Un {@code NOT_RUN} no es {@code PASS}.</p>
 */
class MessageUcoLabLiveIT {

    private static final String APP = System.getenv("MESSAGEUCOLAB_LIVE_APPLICATION_ID");
    private static final String ENV = System.getenv("MESSAGEUCOLAB_LIVE_ENVIRONMENT_ID");
    private static final long PAUSA_ENTRE_PETICIONES_MS = 600;
    private static final String URL =System.getenv().getOrDefault("MESSAGEUCOLAB_LIVE_URL", "http://localhost:8000");

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
                            new MessageUcoLabCodeMapper("MSG_", MessageUcoLabCatalogoTest.aliasesDeApplicationYml()),
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

    /**
     * Garantiza el ORIGEN: el adaptador de estas pruebas se construye sin contingencia, de modo que un acierto solo
     * puede venir de MessageUcoLab (Azure no puede enmascarar un mensaje ausente ni distinto). Usa los codigos
     * canonicos y los alias reales de application.yml, asi que prueba tambien el mapeo de los 27 codigos largos.
     * Los mensajes que superan el limite de contenido del componente ({@code MESSAGEUCOLAB_LIVE_CONTENT_LIMIT}, 100
     * por defecto) no pueden estar cargados y se excluyen.
     */
    @Test
    void todo_el_catalogo_que_cabe_se_sirve_desde_messageucolab_con_el_texto_exacto_del_seed() throws Exception {
        requireLive();

        final int limite = Integer.parseInt(System.getenv().getOrDefault("MESSAGEUCOLAB_LIVE_CONTENT_LIMIT", "100"));
        final MessageCatalogPort port = adapter(new ApplicationContextRunnerHolder(), ENV);
        final List<String> lines;
        try (var in = getClass().getResourceAsStream("/messageucolab/catalogo-mensajes-usuario.psv")) {
            lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .filter(l -> !l.isBlank() && !l.startsWith("#")).toList();
        }

        final List<String> discrepancias = new ArrayList<>();
        int verificados = 0;
        for (final String line : lines) {
            final String[] c = line.split("\\|", 6);
            if (c[5].length() > limite) {
                continue;
            }
            if (verificados > 0) {
                // El gateway limita a 120 peticiones por minuto por IP y responde 429; el adaptador sin cache pide
                // cada codigo, asi que se espacia (~100/min) para medir el catalogo y no el limite.
                Thread.sleep(PAUSA_ENTRE_PETICIONES_MS);
            }
            verificados++;
            final Optional<String> actual = port.findUserMessage(c[0]);
            if (!Optional.of(c[5]).equals(actual)) {
                discrepancias.add(c[0] + " -> " + actual.orElse("<vacio>"));
            }
        }

        assertEquals(List.of(), discrepancias);
        assertTrue(verificados >= 140, "se esperaban al menos 140 mensajes verificados y fueron " + verificados);
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

        assertEquals(Optional.empty(), port.findUserMessage("NOEXISTE9"));
        assertEquals("NOEXISTE9", port.getUserMessage("NOEXISTE9"));
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

        assertEquals(Optional.empty(), port.findUserMessage("ERR_CODIGO_LARGO_QUE_NO_ESTA_EN_EL_CATALOGO"));
    }
}
