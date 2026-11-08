package co.edu.uco.asistenciasuco.infrastructure.audit.web;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditEvent;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CULM-001 D.6 (CONTRACT v2 §7.2): metadatos de auditoria con valores, denegacion por nombre, tope y publicador que falla. */
class AuditInterceptorAdminMetadataTest {

    private final List<AuditEvent> eventos = new ArrayList<>();
    private AuditInterceptor interceptor = new AuditInterceptor(eventos::add, new ClientIpResolver(), new RequestActorResolver());

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    public record Cuerpo(String motivo, String password, String clave_inicial, String claveInicial, String token, String pin, String secret,
                         String clave, String largo) {
    }

    public record Datos(String grupo, String clave, String valorAnterior, String valorNuevo) {
    }

    public record Envoltorio(boolean exitoso, Datos datos) {
    }

    @SuppressWarnings("unused")
    @AuditableOperation(action = "ACTUALIZAR_PARAMETRO", resourceType = "PARAMETRO",
            metadataRequestFields = {"motivo", "password", "clave_inicial", "claveInicial", "token", "pin", "secret", "clave", "largo"},
            metadataResponseFields = {"grupo", "clave", "valorAnterior", "valorNuevo", "inexistente"})
    public void operacion() {
    }

    @SuppressWarnings("unused")
    @AuditableOperation(action = "SIN_CAMPOS", resourceType = "X")
    public void sinCampos() {
    }

    private HandlerMethod handler(final String nombre) throws NoSuchMethodException {
        final Method metodo = AuditInterceptorAdminMetadataTest.class.getMethod(nombre);
        return new HandlerMethod(this, metodo);
    }

    private AuditEvent ejecutar(final Object cuerpoPeticion, final Object cuerpoRespuesta, final String operacion) throws Exception {
        CorrelationIdContext.set(UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c"));
        final MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/admin/parametros/x");
        AuditRequestAttributes.storeRequestBody(request, cuerpoPeticion);
        AuditRequestAttributes.storeResponseBody(request, cuerpoRespuesta);
        interceptor.afterCompletion(request, new MockHttpServletResponse(), handler(operacion), null);
        return eventos.isEmpty() ? null : eventos.getFirst();
    }

    @Test
    void copia_motivo_y_anterior_nuevo_del_cuerpo_de_la_respuesta_incluso_dentro_de_datos() throws Exception {
        final AuditEvent evento = ejecutar(new Cuerpo("Ajuste de prueba", null, null, null, null, null, null, null, null),
                new Envoltorio(true, new Datos("ASISTENCIA", "TOLERANCIA_MINUTOS", "15", "20")), "operacion");

        assertEquals("HTTP", evento.metadata().get("handlerType"));
        assertEquals("Ajuste de prueba", evento.metadata().get("motivo"));
        assertEquals("ASISTENCIA", evento.metadata().get("grupo"));
        assertEquals("TOLERANCIA_MINUTOS", evento.metadata().get("clave"));
        assertEquals("15", evento.metadata().get("valorAnterior"));
        assertEquals("20", evento.metadata().get("valorNuevo"));
        assertFalse(evento.metadata().containsKey("inexistente"));
    }

    @Test
    void los_nombres_sensibles_nunca_se_copian_y_clave_de_parametro_si() throws Exception {
        final AuditEvent evento = ejecutar(new Cuerpo("Motivo", "secreto1", "ci-2", "ci-3", "tok", "123456", "sec", "TOLERANCIA", null),
                null, "operacion");

        final Map<String, String> metadata = evento.metadata();
        for (final String denegado : List.of("password", "clave_inicial", "claveInicial", "token", "pin", "secret")) {
            assertFalse(metadata.containsKey(denegado), denegado + " no debe copiarse");
        }
        assertFalse(metadata.values().stream().anyMatch(v -> v.contains("secreto1") || v.contains("ci-2") || v.contains("123456")));
        assertEquals("TOLERANCIA", metadata.get("clave"));
    }

    @Test
    void trunca_cada_valor_a_240_y_corta_el_total_a_4_kb() throws Exception {
        final AuditEvent evento = ejecutar(new Cuerpo("m".repeat(500), null, null, null, null, null, null, null, "x".repeat(3000)), null, "operacion");

        assertEquals(240, evento.metadata().get("motivo").length());
        assertTrue(evento.metadata().get("largo").length() <= 240);
        assertTrue(evento.metadata().toString().length() <= 4200);
    }

    @Test
    void sin_campos_pedidos_o_sin_cuerpo_solo_queda_handlerType() throws Exception {
        assertEquals(Map.of("handlerType", "HTTP"), ejecutar(null, null, "sinCampos").metadata());
        eventos.clear();
        assertEquals(Map.of("handlerType", "HTTP"), ejecutar(null, null, "operacion").metadata());
    }

    @Test
    void un_publicador_que_falla_no_rompe_la_peticion() throws Exception {
        interceptor = new AuditInterceptor(evento -> {
            throw new IllegalStateException("publicador caido");
        }, new ClientIpResolver(), new RequestActorResolver());

        assertDoesNotThrow(() -> ejecutar(null, null, "operacion"));
    }

    @Test
    void esCampoDenegado_distingue_mayusculas_y_nulos() {
        assertTrue(AuditInterceptor.esCampoDenegado("Password"));
        assertTrue(AuditInterceptor.esCampoDenegado("claveInicial"));
        assertFalse(AuditInterceptor.esCampoDenegado("clave"));
        assertFalse(AuditInterceptor.esCampoDenegado(null));
    }
}
