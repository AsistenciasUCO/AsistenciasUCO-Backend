package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarEstudiantesDomainTest {

    @Test
    void crear_aplica_paginacion_por_defecto_y_normaliza_correo() {
        final ConsultarEstudiantesDomain domain = ConsultarEstudiantesDomain.crear(
                null,
                null,
                " Ana ",
                " ANA.PEREZ@UCO.EDU.CO ",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertEquals(0, domain.page());
        assertEquals(20, domain.size());
        assertEquals("Ana", domain.nombre());
        assertEquals("ana.perez@uco.edu.co", domain.correo());
    }

    @Test
    void crear_rechaza_page_negativo_y_size_fuera_de_rango() {
        assertValidationCode("ERR_PAGE_INVALIDA", -1, 20);
        assertValidationCode("ERR_SIZE_INVALIDO", 0, 0);
        assertValidationCode("ERR_SIZE_INVALIDO", 0, 101);
    }

    @Test
    void crear_rechaza_uuid_vacio_en_filtros() {
        final UUID emptyUuid = new UUID(0L, 0L);

        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> ConsultarEstudiantesDomain.crear(
                        emptyUuid,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        20,
                null
                )
        );

        assertEquals("ERR_TIPO_IDENTIFICACION_INVALIDA", exception.getCode());
    }

    private void assertValidationCode(final String expectedCode, final Integer page, final Integer size) {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> ConsultarEstudiantesDomain.crear(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        page,
                        size,
                null
                )
        );

        assertEquals(expectedCode, exception.getCode());
    }
}
