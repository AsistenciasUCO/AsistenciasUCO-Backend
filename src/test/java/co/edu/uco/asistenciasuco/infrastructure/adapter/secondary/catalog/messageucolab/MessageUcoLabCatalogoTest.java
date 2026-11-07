package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consistencia entre el catalogo de usuario (fixture generado desde el seed), los alias de {@code application.yml} y los
 * limites vigentes de MessageUcoLab (codigo 1..10 alfanumerico, titulo 10..50). No necesita el componente levantado.
 */
class MessageUcoLabCatalogoTest {

    private record Fila(String canonico, String remoto, String tipo, String categoria, String titulo, String contenido) { }

    private static List<Fila> catalogo() throws Exception {
        try (var in = MessageUcoLabCatalogoTest.class.getResourceAsStream("/messageucolab/catalogo-mensajes-usuario.psv")) {
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .filter(l -> !l.isBlank() && !l.startsWith("#"))
                    .map(l -> l.split("\\|", 6))
                    .map(c -> new Fila(c[0], c[1], c[2], c[3], c[4], c[5]))
                    .toList();
        }
    }

    static Map<String, String> aliasesDeApplicationYml() {
        final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("src/main/resources/application.yml"));
        final Properties props = yaml.getObject();
        final Pattern clave = Pattern.compile("^app\\.providers\\.messageucolab\\.code-aliases\\.?\\[(.+)]$");
        final Map<String, String> aliases = new TreeMap<>();
        for (final String name : props.stringPropertyNames()) {
            final Matcher m = clave.matcher(name);
            if (m.matches()) {
                aliases.put(m.group(1), props.getProperty(name));
            }
        }
        return aliases;
    }

    private static String sinSimbolos(final String canonico) {
        return canonico.replaceAll("[^A-Za-z0-9]", "");
    }

    @Test
    void el_catalogo_trae_los_128_codigos_de_usuario_de_los_seeds() throws Exception {
        // CULM-001 A.1: - SIG_001..SIG_015 y ERR_SOLICITUD_MATRICULA_NO_IMPLEMENTADA (seeds 23 y 04 retirados).
        assertEquals(128, catalogo().size());
    }

    @Test
    void todo_codigo_remoto_cumple_el_limite_y_es_unico() throws Exception {
        final Set<String> vistos = new HashSet<>();
        for (final Fila f : catalogo()) {
            assertTrue(f.remoto().matches("[A-Za-z0-9]{1,10}"), f.canonico() + " -> " + f.remoto());
            assertTrue(vistos.add(f.remoto()), "codigo remoto repetido: " + f.remoto());
        }
    }

    @Test
    void todo_titulo_cumple_10_a_50_y_conserva_el_nombre_canonico() throws Exception {
        for (final Fila f : catalogo()) {
            assertTrue(f.titulo().length() >= 10 && f.titulo().length() <= 50, f.canonico() + ": " + f.titulo().length());
            assertTrue(f.titulo().startsWith(f.canonico()), f.canonico() + " no esta en el titulo: " + f.titulo());
        }
    }

    @Test
    void los_alias_de_application_yml_son_exactamente_los_codigos_que_no_caben_en_diez_caracteres() throws Exception {
        final Map<String, String> esperados = new TreeMap<>();
        for (final Fila f : catalogo()) {
            if (sinSimbolos(f.canonico()).length() > 10) {
                esperados.put(f.canonico(), f.remoto());
            }
        }

        // CULM-001 A.1: - ERR_SOLICITUD_MATRICULA_NO_IMPLEMENTADA (ERR021 queda libre, no se renumera).
        assertEquals(26, esperados.size());
        assertEquals(esperados, aliasesDeApplicationYml());
    }

    @Test
    void el_mapeador_con_los_alias_reales_resuelve_cada_codigo_al_remoto_del_catalogo() throws Exception {
        final MessageUcoLabCodeMapper mapper = new MessageUcoLabCodeMapper("MSG_", aliasesDeApplicationYml());

        for (final Fila f : catalogo()) {
            assertEquals(Optional.of("MSG_" + f.remoto()), mapper.toRemoteCode(f.canonico()), f.canonico());
        }
    }
}
