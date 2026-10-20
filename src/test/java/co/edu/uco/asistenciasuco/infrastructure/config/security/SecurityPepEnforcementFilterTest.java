package co.edu.uco.asistenciasuco.infrastructure.config.security;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityPepEnforcementFilterTest {

    private static final String TOKEN = "Bearer test-only-token";
    private static final String CORRELATION_ID = "11111111-2222-3333-4444-555555555555";
    private static final String UUID_PATH = "/api/v1/facultades/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @AfterEach
    void clearCorrelationContext() {
        CorrelationIdContext.clear();
    }

    @Test
    void disabled_filter_does_not_call_pep() throws Exception {
        final MockHttpServletRequest request = request("GET", "/api/v1/facultades");
        request.addHeader("Authorization", TOKEN);

        final Outcome outcome = run(new SecurityPepEnforcementFilter(
                false, "http://127.0.0.1:1", "app", "test", "credential", "/api/v1/public/**"), request);

        assertTrue(outcome.chainContinued());
        assertEquals(200, outcome.response().getStatus());
    }

    @Test
    void omite_options_rutas_no_api_event_grid_y_rutas_publicas() throws Exception {
        final String publicPath = "/api/v1/public/status";
        final String[] methods = {"OPTIONS", "GET", "POST", "GET"};
        final String[] paths = {"/api/v1/facultades", "/health", "/api/v1/internal/azure-events", publicPath};
        final MockHttpServletRequest eventGrid = request("POST", paths[2]);
        eventGrid.addHeader("aeg-sas-token", "configured-test-only-token");

        for (int index = 0; index < paths.length; index++) {
            final MockHttpServletRequest request = index == 2 ? eventGrid : request(methods[index], paths[index]);
            final Outcome outcome = run(new SecurityPepEnforcementFilter(
                    true, "http://127.0.0.1:1", "app", "test", "credential", publicPath), request);

            assertTrue(outcome.chainContinued(), paths[index]);
            assertEquals(200, outcome.response().getStatus(), paths[index]);
        }
    }

    @Test
    void deja_a_spring_security_los_requests_sin_bearer() throws Exception {
        final Outcome outcome = run(new SecurityPepEnforcementFilter(
                true, "http://127.0.0.1:1", "app", "test", "credential", null),
                request("GET", "/api/v1/facultades"));

        assertTrue(outcome.chainContinued());
        assertEquals(200, outcome.response().getStatus());
    }

    @Test
    void permite_si_pep_autoriza_y_envia_la_decision_normalizada() throws Exception {
        try (PepServer pep = new PepServer(204, "")) {
            CorrelationIdContext.set(java.util.UUID.fromString(CORRELATION_ID));
            final MockHttpServletRequest request = request("POST", UUID_PATH);
            request.addHeader("Authorization", TOKEN);
            request.addHeader("X-Correlation-Id", "header-correlation-is-overridden");

            final Outcome outcome = run(new SecurityPepEnforcementFilter(
                    true, pep.baseUrl() + "/", "app\"name", "qa", "app-credential", null), request);

            assertTrue(outcome.chainContinued());
            assertEquals(200, outcome.response().getStatus());
            assertEquals(TOKEN, pep.authorization().get());
            assertEquals("app-credential", pep.applicationCredential().get());
            assertEquals(CORRELATION_ID, pep.correlationId().get());
            assertTrue(pep.body().get().contains("\\\"name"));
            assertTrue(pep.body().get().contains("/api/v1/facultades/{id}"));
            assertTrue(pep.body().get().contains("POST"));
        }
    }

    @Test
    void rechaza_con_403_cuando_pep_deniega() throws Exception {
        final Outcome outcome = runAgainstPep(403, "denied");

        assertFalse(outcome.chainContinued());
        assertEquals(403, outcome.response().getStatus());
        assertTrue(outcome.response().getContentAsString().contains("ACCESO_DENEGADO_POR_SEGURIDAD"));
    }

    @Test
    void rechaza_con_401_cuando_pep_rechaza_el_token() throws Exception {
        final Outcome outcome = runAgainstPep(401, "invalid");

        assertFalse(outcome.chainContinued());
        assertEquals(401, outcome.response().getStatus());
        assertTrue(outcome.response().getContentAsString().contains("TOKEN_INVALIDO_O_REVOCADO"));
    }

    @Test
    void bloquea_con_403_los_codigos_inesperados() throws Exception {
        final Outcome outcome = runAgainstPep(500, "pep-error");

        assertFalse(outcome.chainContinued());
        assertEquals(403, outcome.response().getStatus());
        assertTrue(outcome.response().getContentAsString().contains("ACCESO_DENEGADO_POR_SEGURIDAD"));
    }

    @Test
    void responde_503_si_pep_no_esta_disponible() throws Exception {
        final Outcome outcome = run(new SecurityPepEnforcementFilter(
                true, "http://127.0.0.1:1", "app", "test", "credential", null), authorizedRequest());

        assertFalse(outcome.chainContinued());
        assertEquals(503, outcome.response().getStatus());
        assertTrue(outcome.response().getContentAsString().contains("SERVICIO_SEGURIDAD_NO_DISPONIBLE"));
    }

    private Outcome runAgainstPep(final int status, final String body) throws Exception {
        try (PepServer pep = new PepServer(status, body)) {
            return run(new SecurityPepEnforcementFilter(
                    true, pep.baseUrl(), "app", "test", "credential", null), authorizedRequest());
        }
    }

    private static MockHttpServletRequest authorizedRequest() {
        final MockHttpServletRequest request = request("GET", "/api/v1/facultades");
        request.addHeader("Authorization", TOKEN);
        return request;
    }

    private static MockHttpServletRequest request(final String method, final String path) {
        final MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        return request;
    }

    private static Outcome run(
            final SecurityPepEnforcementFilter filter,
            final MockHttpServletRequest request
    ) throws ServletException, IOException {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return new Outcome(response, chain.getRequest() != null);
    }

    private record Outcome(MockHttpServletResponse response, boolean chainContinued) {
    }

    private static final class PepServer implements AutoCloseable {

        private final HttpServer server;
        private final int status;
        private final String responseBody;
        private final AtomicReference<String> body = new AtomicReference<>();
        private final AtomicReference<String> authorization = new AtomicReference<>();
        private final AtomicReference<String> correlationId = new AtomicReference<>();
        private final AtomicReference<String> applicationCredential = new AtomicReference<>();

        private PepServer(final int status, final String responseBody) throws IOException {
            this.status = status;
            this.responseBody = responseBody;
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private AtomicReference<String> body() {
            return body;
        }

        private AtomicReference<String> authorization() {
            return authorization;
        }

        private AtomicReference<String> correlationId() {
            return correlationId;
        }

        private AtomicReference<String> applicationCredential() {
            return applicationCredential;
        }

        private void handle(final HttpExchange exchange) throws IOException {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            correlationId.set(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
            applicationCredential.set(exchange.getRequestHeaders().getFirst("X-Application-Credential"));
            final byte[] payload = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
