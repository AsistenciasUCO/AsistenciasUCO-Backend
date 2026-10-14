package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Errores de framework (parametros ausentes o de tipo incorrecto) traducidos al contrato de error
 * estandar: 400 + codigo y detalle por campo segun el tipo esperado, sin exponer clases internas.
 */
class GlobalExceptionHandlerFrameworkErrorsTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(codigo -> Optional.empty());

    private static MockHttpServletRequest request() {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prueba");
        request.setRequestURI("/api/v1/prueba");
        return request;
    }

    private static MethodArgumentTypeMismatchException mismatch(final String name, final Class<?> requiredType) {
        return new MethodArgumentTypeMismatchException("valor-invalido", requiredType, name, (MethodParameter) null, null);
    }

    @Test
    void parametro_de_query_ausente_responde_400_con_detalle_de_campo_requerido() {
        final ResponseEntity<ApiErrorResponse> response = handler.handleMissingRequestParameter(
                new MissingServletRequestParameterException("page", "Integer"), request());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(List.of(new ApiFieldError(
                        "page", ApiFieldErrorCode.FIELD_REQUIRED.name(), "El parametro 'page' es obligatorio.")),
                response.getBody().details());
        assertEquals("/api/v1/prueba", response.getBody().path());
    }

    @Test
    void tipo_uuid_invalido_responde_400_con_detalle_de_uuid() {
        final ResponseEntity<ApiErrorResponse> response =
                handler.handleMethodArgumentTypeMismatch(mismatch("grupoId", UUID.class), request());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_REQUEST", response.getBody().code());
        assertEquals(List.of(new ApiFieldError("grupoId", ApiFieldErrorCode.FIELD_INVALID_UUID.name(),
                "El identificador 'grupoId' debe tener un formato UUID valido.")), response.getBody().details());
    }

    @Test
    void tipos_numericos_invalidos_responden_400_con_detalle_de_tipo_numerico() {
        for (final Class<?> numeric : new Class<?>[]{Integer.class, int.class, Long.class, long.class}) {
            final ResponseEntity<ApiErrorResponse> response =
                    handler.handleMethodArgumentTypeMismatch(mismatch("limite", numeric), request());

            assertEquals(List.of(new ApiFieldError("limite", ApiFieldErrorCode.FIELD_INVALID_TYPE.name(),
                            "El campo 'limite' debe contener un valor numerico valido.")),
                    response.getBody().details(), numeric.getName());
        }
    }

    @Test
    void tipos_booleanos_invalidos_responden_400_con_detalle_de_booleano() {
        for (final Class<?> bool : new Class<?>[]{Boolean.class, boolean.class}) {
            final ResponseEntity<ApiErrorResponse> response =
                    handler.handleMethodArgumentTypeMismatch(mismatch("activo", bool), request());

            assertEquals(List.of(new ApiFieldError("activo", ApiFieldErrorCode.FIELD_INVALID_TYPE.name(),
                            "El campo 'activo' debe contener true o false.")),
                    response.getBody().details(), bool.getName());
        }
    }

    @Test
    void tipos_sin_detalle_conocido_responden_400_sin_detalles() {
        final ResponseEntity<ApiErrorResponse> otroTipo =
                handler.handleMethodArgumentTypeMismatch(mismatch("fecha", java.time.LocalDate.class), request());
        final ResponseEntity<ApiErrorResponse> sinTipo =
                handler.handleMethodArgumentTypeMismatch(mismatch("fecha", null), request());

        assertEquals(HttpStatus.BAD_REQUEST, otroTipo.getStatusCode());
        assertTrue(otroTipo.getBody().details().isEmpty());
        assertEquals(HttpStatus.BAD_REQUEST, sinTipo.getStatusCode());
        assertTrue(sinTipo.getBody().details().isEmpty());
        assertNull(sinTipo.getBody().correlationId());
    }

    @Test
    void metodo_no_soportado_responde_405_json_conservando_el_encabezado_allow() {
        final ResponseEntity<ApiErrorResponse> response = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST")), request());

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(Set.of(HttpMethod.GET, HttpMethod.POST), response.getHeaders().getAllow());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertEquals(405, response.getBody().status());
    }

    @Test
    void metodo_no_soportado_sin_metodos_soportados_responde_405_json_sin_allow() {
        final ResponseEntity<ApiErrorResponse> response = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("DELETE"), request());

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertTrue(response.getHeaders().getAllow().isEmpty());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }
}
