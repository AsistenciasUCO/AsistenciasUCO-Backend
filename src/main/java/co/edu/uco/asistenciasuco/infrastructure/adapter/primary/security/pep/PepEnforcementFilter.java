package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.pep;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Filtro de cumplimiento de políticas PEP (Policy Enforcement Point).
 * Consulta la decisión de acceso antes de permitir que la petición continúe hacia Spring Security y los controladores.
 * Aplica política fail-closed (503 si el servicio PEP no responde).
 */
public final class PepEnforcementFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PepEnforcementFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final String pepUrl;
    private final String applicationName;
    private final String environment;
    private final String registrationToken;
    private final List<String> publicPaths;
    private final HttpClient httpClient;

    public PepEnforcementFilter(
            final String pepUrl,
            final String applicationName,
            final String environment,
            final String registrationToken,
            final List<String> publicPaths
    ) {
        this.pepUrl = pepUrl.endsWith("/") ? pepUrl.substring(0, pepUrl.length() - 1) : pepUrl;
        this.applicationName = applicationName;
        this.environment = environment;
        this.registrationToken = registrationToken != null ? registrationToken : "";
        this.publicPaths = publicPaths != null ? publicPaths : List.of();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        // Permitir solicitudes de preflight CORS (OPTIONS)
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String path = request.getRequestURI();

        // Rutas técnicas públicas (health, swagger, webhook, etc.)
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Consultar decisión de acceso al PEP
        try {
            final String endpoint = pepUrl + "/internal/v1/embedded-access-decisions";
            final String requestBody = String.format(
                    "{\"applicationName\":\"%s\",\"environment\":\"%s\",\"path\":\"%s\",\"method\":\"%s\"}",
                    escapeJson(applicationName),
                    escapeJson(environment),
                    escapeJson(path),
                    escapeJson(request.getMethod())
            );

            final HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(4))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Application-Credential", registrationToken)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && !authHeader.isBlank()) {
                reqBuilder.header(HttpHeaders.AUTHORIZATION, authHeader);
            }

            final String cookieHeader = request.getHeader("Cookie");
            if (cookieHeader != null && !cookieHeader.isBlank()) {
                reqBuilder.header("Cookie", cookieHeader);
            }

            final HttpResponse<String> pepResponse = httpClient.send(
                    reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            final int statusCode = pepResponse.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                // Decisión permitida por PEP -> continuar en la cadena
                filterChain.doFilter(request, response);
                return;
            }

            if (statusCode == HttpServletResponse.SC_UNAUTHORIZED) {
                final String body = pepResponse.body().isBlank()
                        ? "{\"detail\":\"No autorizado por el servicio de seguridad PEP\",\"status\":401,\"title\":\"UNAUTHORIZED\",\"code\":\"PEP_UNAUTHORIZED\"}"
                        : pepResponse.body();
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, body);
                return;
            }

            if (statusCode == HttpServletResponse.SC_FORBIDDEN) {
                final String body = pepResponse.body().isBlank()
                        ? "{\"detail\":\"Acceso denegado por políticas de seguridad PEP\",\"status\":403,\"title\":\"FORBIDDEN\",\"code\":\"PEP_FORBIDDEN\"}"
                        : pepResponse.body();
                writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, body);
                return;
            }

            // Código inesperado -> Fail-closed
            log.warn("PEP respondió con código inesperado {} para la ruta {}", statusCode, path);
            writeErrorResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "{\"detail\":\"Servicio de autorización PEP respondió con estado inesperado\",\"status\":503,\"title\":\"SERVICE_UNAVAILABLE\",\"code\":\"PEP_UNAVAILABLE\"}");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Evaluación PEP interrumpida: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "{\"detail\":\"Servicio de autorización interrumpido\",\"status\":503,\"title\":\"SERVICE_UNAVAILABLE\",\"code\":\"PEP_UNAVAILABLE\"}");
        } catch (Exception e) {
            // Error de conexión o PEP inaccesible -> Fail-closed
            log.error("Fallo de comunicación con PEP (fail-closed): {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "{\"detail\":\"Servicio de autorización PEP no disponible (fail-closed)\",\"status\":503,\"title\":\"SERVICE_UNAVAILABLE\",\"code\":\"PEP_CONNECTION_FAILED\"}");
        }
    }

    private boolean isPublicPath(final String path) {
        for (final String pattern : publicPaths) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static void writeErrorResponse(
            final HttpServletResponse response,
            final int status,
            final String jsonBody
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonBody);
        response.getWriter().flush();
    }

    private static String escapeJson(final String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
