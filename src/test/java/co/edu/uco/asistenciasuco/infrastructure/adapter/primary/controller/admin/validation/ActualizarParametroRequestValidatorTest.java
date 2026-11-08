package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.AdminParametroController.ActualizarParametroRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CULM-001 D.3 (CONTRACT v2 §4.3): forma del cuerpo del PUT de parametros; el motivo minimo y el rango los valida la DB. */
class ActualizarParametroRequestValidatorTest {

    private final ActualizarParametroRequestValidator validator = new ActualizarParametroRequestValidator();

    private static ActualizarParametroRequest request(final String valor, final String fecha, final String motivo) {
        return new ActualizarParametroRequest(valor, fecha, motivo);
    }

    @Test
    void un_cuerpo_completo_es_valido_con_o_sin_milisegundos() {
        assertTrue(validator.validate(request("20", "2026-11-07T15:04:05.007Z", "Ajuste de prueba")).isValid());
        assertTrue(validator.validate(request("20", "2026-11-07T15:04:05Z", "Ajuste de prueba")).isValid());
        assertTrue(validator.validate(request("true", "2026-11-07T15:04:05.007Z", "m".repeat(500))).isValid());
    }

    @Test
    void el_cuerpo_nulo_y_cada_campo_obligatorio_se_rechazan_por_su_nombre() {
        assertEquals("valor", campo(validator.validate(null)));
        assertEquals("valor", campo(validator.validate(request(null, "2026-11-07T15:04:05.007Z", "Ajuste de prueba"))));
        assertEquals("valor", campo(validator.validate(request("  ", "2026-11-07T15:04:05.007Z", "Ajuste de prueba"))));
        assertEquals("fechaModificacionEsperada", campo(validator.validate(request("20", null, "Ajuste de prueba"))));
        assertEquals("motivo", campo(validator.validate(request("20", "2026-11-07T15:04:05.007Z", null))));
        assertEquals("motivo", campo(validator.validate(request("20", "2026-11-07T15:04:05.007Z", "   "))));
    }

    @Test
    void la_fecha_debe_ser_iso_8601_en_utc_y_los_textos_tienen_tope() {
        assertEquals("fechaModificacionEsperada", campo(validator.validate(request("20", "ayer", "Ajuste de prueba"))));
        assertEquals("fechaModificacionEsperada", campo(validator.validate(request("20", "2026-11-07", "Ajuste de prueba"))));
        assertEquals("motivo", campo(validator.validate(request("20", "2026-11-07T15:04:05.007Z", "m".repeat(501)))));
        assertEquals("valor", campo(validator.validate(request("v".repeat(4001), "2026-11-07T15:04:05.007Z", "Ajuste de prueba"))));
        assertTrue(validator.validate(request("v".repeat(4000), "2026-11-07T15:04:05.007Z", "Ajuste de prueba")).isValid());
    }

    private static String campo(final ValidationResult resultado) {
        assertTrue(resultado.hasErrors());
        return resultado.issues().getFirst().field();
    }
}
