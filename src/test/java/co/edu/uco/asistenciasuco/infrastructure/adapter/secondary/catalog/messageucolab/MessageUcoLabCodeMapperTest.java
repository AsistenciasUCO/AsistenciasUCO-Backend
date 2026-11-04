package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageUcoLabCodeMapperTest {

    private final MessageUcoLabCodeMapper mapper = new MessageUcoLabCodeMapper("MSG_", Map.of(
            "ERR_CORRELACION_REQUERIDA", "CORR REQ"
    ));

    @Test
    void quita_el_guion_bajo_y_antepone_el_prefijo() {
        // MessageUcoLab solo admite letras, numeros y espacios en el codigo (^[A-Za-z0-9 ]{1,10}$).
        assertEquals(Optional.of("MSG_VAL001"), mapper.toRemoteCode("VAL_001"));
        assertEquals(Optional.of("MSG_VAL001"), mapper.toRemoteCode("  VAL_001 "));
        assertEquals(Optional.of("MSG_GEN004"), mapper.toRemoteCode("GEN_004"));
    }

    @Test
    void el_codigo_resultante_nunca_contiene_caracteres_que_messageucolab_rechaza() {
        for (final String canonical : new String[]{"VAL_001", "usu_001", "A-B_C.D"}) {
            final String remote = mapper.toRemoteCode(canonical).orElseThrow();
            assertEquals(true, remote.substring("MSG_".length()).matches("[A-Z0-9]{1,10}"), remote);
        }
    }

    @Test
    void acepta_exactamente_diez_caracteres_tras_el_prefijo() {
        assertEquals(Optional.of("MSG_ABCDEFGHIJ"), mapper.toRemoteCode("ABCDEFGHIJ"));
        assertEquals(Optional.of("MSG_ABCDE12345"), mapper.toRemoteCode("ABCDE_12345"));
    }

    @Test
    void codigo_de_mas_de_diez_caracteres_sin_alias_no_se_trunca_ni_se_consulta() {
        assertEquals(Optional.empty(), mapper.toRemoteCode("ABCDEFGHIJK"));
        assertEquals(Optional.empty(), mapper.toRemoteCode("ERR_TIPO_IDENTIFICACION_NOMBRE_LONGITUD_INVALIDA"));
    }

    @Test
    void codigo_largo_con_alias_usa_el_alias_normalizado() {
        assertEquals(Optional.of("MSG_CORRREQ"), mapper.toRemoteCode("ERR_CORRELACION_REQUERIDA"));
    }

    @Test
    void alias_que_tambien_excede_el_limite_o_queda_vacio_no_es_resoluble() {
        final MessageUcoLabCodeMapper malo = new MessageUcoLabCodeMapper("MSG_", Map.of(
                "A_LARGO", "ALIAS_DEMASIADO_LARGO", "B_VACIO", " _ ", "C_SIMBOLOS", "-.-"));

        assertEquals(Optional.empty(), malo.toRemoteCode("A_LARGO"));
        assertEquals(Optional.empty(), malo.toRemoteCode("B_VACIO"));
        assertEquals(Optional.empty(), malo.toRemoteCode("C_SIMBOLOS"));
    }

    @Test
    void codigo_nulo_vacio_o_solo_simbolos_no_es_resoluble() {
        assertEquals(Optional.empty(), mapper.toRemoteCode(null));
        assertEquals(Optional.empty(), mapper.toRemoteCode("  "));
        assertEquals(Optional.empty(), mapper.toRemoteCode("___"));
    }

    @Test
    void prefijo_y_alias_nulos_se_toleran() {
        final MessageUcoLabCodeMapper sinConfig = new MessageUcoLabCodeMapper(null, null);

        assertEquals(Optional.of("VAL001"), sinConfig.toRemoteCode("VAL_001"));
    }
}
