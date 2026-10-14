package co.edu.uco.asistenciasuco.infrastructure.config.securitybaseline;

import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.PepDecision;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.SecurityBaselinePepClient;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePepProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityBaselinePepFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
class SecurityBaselinePepFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void solo_continua_con_decision_allow_y_expone_decision_id() throws Exception {
        final StubPepClient client = new StubPepClient(new PepDecision(PepDecision.Outcome.ALLOW, "decision-1"));
        final SecurityBaselinePepFilter filter = filter(client);
        final MockHttpServletRequest request = authenticated("/api/v1/protegido");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicInteger chainCalls = new AtomicInteger();
        final FilterChain chain = (requestValue, responseValue) -> chainCalls.incrementAndGet();

        filter.doFilter(request, response, chain);

        assertEquals(1, chainCalls.get());
        assertEquals("decision-1", response.getHeader("X-Decision-Id"));
        assertEquals(200, response.getStatus());
    }

    @Test
    void indisponibilidad_del_pep_responde_503_y_no_invoca_el_handler() throws Exception {
        final StubPepClient client = new StubPepClient(new PepDecision(PepDecision.Outcome.UNAVAILABLE, null));
        final SecurityBaselinePepFilter filter = filter(client);
        final MockHttpServletRequest request = authenticated("/api/v1/protegido");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicInteger chainCalls = new AtomicInteger();
        final FilterChain chain = (requestValue, responseValue) -> chainCalls.incrementAndGet();

        filter.doFilter(request, response, chain);

        assertEquals(503, response.getStatus());
        assertTrue(response.getContentAsString().contains(CommonErrorCode.INTERNAL_ERROR.code()));
        assertEquals(0, chainCalls.get());
    }

    @Test
    void sin_autenticacion_o_fuera_de_scope_no_llama_al_pep() throws Exception {
        final StubPepClient client = new StubPepClient(new PepDecision(PepDecision.Outcome.ALLOW, null));
        final SecurityBaselinePepFilter filter = filter(client);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicInteger chainCalls = new AtomicInteger();
        final FilterChain chain = (requestValue, responseValue) -> chainCalls.incrementAndGet();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/protegido"), response, chain);
        filter.doFilter(authenticated("/actuator/health"), response, chain);

        assertEquals(2, chainCalls.get());
        assertEquals(0, client.calls.get());
        assertFalse(response.isCommitted());
    }

    private SecurityBaselinePepFilter filter(final SecurityBaselinePepClient client) {
        return new SecurityBaselinePepFilter(client, new SecurityBaselinePepProperties(
                true,
                java.net.URI.create("http://127.0.0.1:18080"),
                "asistencias-uco",
                "lab",
                "credential",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)),
                new SecurityErrorResponseWriter());
    }

    private MockHttpServletRequest authenticated(final String path) {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("institutional-id", "credentials", "ROLE_DOCENTE"));
        return request;
    }

    private static final class StubPepClient extends SecurityBaselinePepClient {

        private final PepDecision decision;
        private final AtomicInteger calls = new AtomicInteger();

        private StubPepClient(final PepDecision decision) {
            super(null, null, new SecurityBaselinePepProperties(
                    false, null, null, null, null, Duration.ofSeconds(1), Duration.ofSeconds(1)));
            this.decision = decision;
        }

        @Override
        public PepDecision evaluate(final jakarta.servlet.http.HttpServletRequest request,
                                    final String correlationId) {
            calls.incrementAndGet();
            return decision;
        }
    }
}
