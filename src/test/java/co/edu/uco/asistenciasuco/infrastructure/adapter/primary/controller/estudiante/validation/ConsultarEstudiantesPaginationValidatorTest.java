package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.validation;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.request.ConsultarEstudiantesRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsultarEstudiantesPaginationValidatorTest {
    private final ConsultarEstudiantesRequestValidator validator = new ConsultarEstudiantesRequestValidator();

    @Test
    void pagina_y_tamano_validos_aceptan_el_borde_inferior_y_un_desplazamiento_grande() {
        assertTrue(validator.validate(request(0, 20)).isValid());
        assertTrue(validator.validate(request(Integer.MAX_VALUE, 1)).isValid());
    }

    @Test
    void pagina_y_tamano_individualmente_validos_pero_con_producto_fuera_de_rango_se_rechazan() {
        final var result = validator.validate(request(Integer.MAX_VALUE, 100));
        assertTrue(result.hasErrors());
        assertTrue(result.issues().stream().anyMatch(issue -> "page".equals(issue.field())));
    }

    @Test
    void tamaño_cero_o_superior_al_maximo_se_rechaza_mediante_la_regla_existente() {
        assertTrue(validator.validate(request(0, 0)).hasErrors());
        assertTrue(validator.validate(request(0, 101)).hasErrors());
    }

    private static ConsultarEstudiantesRequest request(final int page, final int size) {
        return new ConsultarEstudiantesRequest(
                null, null, null, null, null, null, null, null, null, page, size);
    }
}
