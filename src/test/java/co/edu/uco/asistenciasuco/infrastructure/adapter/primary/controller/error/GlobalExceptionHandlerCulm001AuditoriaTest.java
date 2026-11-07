package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DbExceptionTranslator;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CULM-001 A, auditoria en pantalla (F-A-S02, F-A-S04): un rechazo de negocio de la BD llega al cliente con el codigo y el
 * texto institucional del catalogo, no con el generico de {@code CONFLICT} o {@code VALIDATION_ERROR}. El HTTP no cambia:
 * {@code DEC_002} es 409 y {@code SES_017} es 400 (el OpenAPI de las sesiones declara 400, contrato §8.1).
 */
class GlobalExceptionHandlerCulm001AuditoriaTest {

    private static final String CORRELATION_ID = "93641bab-e3cd-485c-b275-47e7b731e18c";

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void dec_002_responde_409_con_su_codigo_y_el_texto_del_catalogo() {
        final ResponseEntity<ApiErrorResponse> response = responder(
                rechazo("DEC_002", "cambiarEstadoDecano"), new GlobalExceptionHandler(codigo -> Optional.empty()), "/api/v1/admin/decanos/x/toggle");

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("DEC_002", response.getBody().code());
        assertEquals("No se puede desactivar al decano de una facultad con programas activos.", response.getBody().message());
        assertEquals(CORRELATION_ID, response.getBody().correlationId());
    }

    @Test
    void ses_017_responde_400_con_su_codigo_y_el_texto_del_catalogo_en_v1_y_v2() {
        for (final String operacion : new String[] {"crearSesion", "crearSesionV2", "actualizarSesion"}) {
            final ResponseEntity<ApiErrorResponse> response = responder(
                    rechazo("SES_017", operacion), new GlobalExceptionHandler(codigo -> Optional.empty()), "/api/v2/sesiones");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), operacion);
            assertEquals("SES_017", response.getBody().code(), operacion);
            assertEquals("La sesión se cruza con otra sesión del grupo o del docente en ese horario.",
                    response.getBody().message(), operacion);
            assertFalse(response.getBody().message().contains("DBCODE"), operacion);
        }
    }

    @Test
    void el_texto_vigente_del_catalogo_externo_prevalece_sobre_el_de_contingencia() {
        final GlobalExceptionHandler conCatalogo = new GlobalExceptionHandler(
                codigo -> "SES_017".equals(codigo) ? Optional.of("Texto vigente del catalogo.") : Optional.empty());

        final ResponseEntity<ApiErrorResponse> response = responder(rechazo("SES_017", "crearSesion"), conCatalogo, "/api/v1/sesiones");

        assertEquals("SES_017", response.getBody().code());
        assertEquals("Texto vigente del catalogo.", response.getBody().message());
    }

    private static ApplicationException rechazo(final String dbCode, final String operacion) {
        return assertThrows(ApplicationException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje usuario", "DBCODE=" + dbCode + "|detalle tecnico", CORRELATION_ID, operacion));
    }

    private static ResponseEntity<ApiErrorResponse> responder(
            final ApplicationException exception, final GlobalExceptionHandler handler, final String ruta) {
        CorrelationIdContext.set(UUID.fromString(CORRELATION_ID));
        final MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.POST.name(), ruta);
        return handler.handleApplication(exception, request);
    }
}
