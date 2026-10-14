package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.pep;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PepEnforcementFilterTest {

    private static final String PEP_UNREACHABLE_URL = "http://127.0.0.1:59999";
    private static final String APP_NAME = "asistencias-backend";
    private static final String ENV = "local";
    private static final String TOKEN = "test-token";
    private static final List<String> PUBLIC_PATHS = List.of("/actuator/health", "/swagger-ui/**");

    @Test
    void options_request_bypasses_pep_enforcement() throws ServletException, IOException {
        final PepEnforcementFilter filter = new PepEnforcementFilter(
                PEP_UNREACHABLE_URL, APP_NAME, ENV, TOKEN, PUBLIC_PATHS
        );

        final MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/sesiones");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void public_path_bypasses_pep_enforcement() throws ServletException, IOException {
        final PepEnforcementFilter filter = new PepEnforcementFilter(
                PEP_UNREACHABLE_URL, APP_NAME, ENV, TOKEN, PUBLIC_PATHS
        );

        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void pep_unreachable_fails_closed_with_503() throws ServletException, IOException {
        final PepEnforcementFilter filter = new PepEnforcementFilter(
                PEP_UNREACHABLE_URL, APP_NAME, ENV, TOKEN, PUBLIC_PATHS
        );

        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/sesiones");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, response.getStatus());
    }
}
