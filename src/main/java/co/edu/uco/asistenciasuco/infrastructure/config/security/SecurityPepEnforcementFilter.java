package co.edu.uco.asistenciasuco.infrastructure.config.security;

import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Filtro interceptor de seguridad para delegar decisiones de autorizacion
 * al Policy Enforcement Point (PEP) del Security Baseline UCO.
 */
public class SecurityPepEnforcementFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityPepEnforcementFilter.class);

    private final boolean enabled;
    private final String pepUrl;
    private final String applicationName;
    private final String environment;
    private final String applicationCredential;
    private final List<String> publicPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final HttpClient httpClient;

    public SecurityPepEnforcementFilter(
            final boolean enabled,
            final String pepUrl,
            final String applicationName,
            final String environment,
            final String applicationCredential,
            final String publicPathsConfig
    ) {
        this.enabled = enabled;
        this.pepUrl = (pepUrl != null && pepUrl.endsWith("/")) ? pepUrl.substring(0, pepUrl.length() - 1) : pepUrl;
        this.applicationName = (applicationName != null && !applicationName.isBlank()) ? applicationName : "asistencias-backend";
        this.environment = (environment != null && !environment.isBlank()) ? environment : "local";
        this.applicationCredential = applicationCredential;
        this.publicPaths = publicPathsConfig != null
                ? Arrays.stream(publicPathsConfig.split(","))
                        .map(String::trim)
                        .filter(p -> !p.isBlank())
                        .toList()
                : List.of();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        if (!enabled) {
            return true;
        }

        // Permitir preflight CORS sin inspeccion PEP
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        final String requestUri = request.getRequestURI();
        final String contextPath = request.getContextPath();
        final String path = (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath))
                ? requestUri.substring(contextPath.length())
                : requestUri;

        // Solo filtrar endpoints de la API (/api/v1/**, /api/v2/**)
        if (!path.startsWith("/api/")) {
            return true;
        }

        // Webhook interno de Azure Event Grid
        if (AzureEventGridAuthFilter.isWebhookRequest(request)) {
            return true;
        }

        // Rutas publicas explicitas
        for (final String pattern : publicPaths) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            // Si no hay token Bearer, dejar que la cadena de Spring Security maneje la autenticacion (401)
            filterChain.doFilter(request, response);
            return;
        }

        final String requestUri = request.getRequestURI();
        final String method = request.getMethod();
        final String correlationId = getCorrelationId(request);
        final String requestId = UUID.randomUUID().toString();
        final String normalizedPath = normalizePath(requestUri);

        final String requestBody = """
                {"applicationName":"%s","environment":"%s","path":"%s","method":"%s"}
                """.formatted(
                escapeJson(applicationName),
                escapeJson(environment),
                escapeJson(normalizedPath),
                escapeJson(method)
        );

        final String endpoint = pepUrl + "/internal/v1/embedded-access-decisions";

        try {
            final HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .header("Authorization", authHeader)
                    .header("X-Correlation-Id", correlationId)
                    .header("X-Request-Id", requestId)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (applicationCredential != null && !applicationCredential.isBlank()) {
                reqBuilder.header("X-Application-Credential", applicationCredential);
            }

            log.info("Consultando PEP: endpoint={}, requestBody={}", endpoint, requestBody);
            final HttpResponse<String> pepResponse = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            final int status = pepResponse.statusCode();

            if (status >= 200 && status < 300) {
                // Decision favorable: PEP autoriza el consumo
                log.info("PEP autorizo acceso a {} {} (HTTP {})", method, requestUri, status);
                filterChain.doFilter(request, response);
                return;
            }

            if (status == HttpServletResponse.SC_FORBIDDEN) {
                log.warn("PEP denego el acceso a la ruta {} {} (HTTP 403 Forbidden). Response: {}", method, requestUri, pepResponse.body());
                writeForbiddenResponse(response);
                return;
            }

            if (status == HttpServletResponse.SC_UNAUTHORIZED) {
                log.warn("PEP rechazo el token para {} {} (HTTP 401 Unauthorized). Response: {}", method, requestUri, pepResponse.body());
                writeUnauthorizedResponse(response);
                return;
            }

            log.error("PEP retorno codigo inesperado {} para {} {}. Body: {}. Bloqueando por seguridad (fail-closed).", status, method, requestUri, pepResponse.body());
            writeForbiddenResponse(response);

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Llamada al PEP interrumpida para {} {}: {}", method, requestUri, e.getMessage());
            writeServiceUnavailableResponse(response);
        } catch (final Exception e) {
            log.error("Error al consultar el PEP en {}: {}. Bloqueando por seguridad (fail-closed).", endpoint, e.getMessage());
            writeServiceUnavailableResponse(response);
        }
    }

    private String getCorrelationId(final HttpServletRequest request) {
        final String contextCorrelation = CorrelationIdContext.getAsString();
        if (contextCorrelation != null && !contextCorrelation.isBlank()) {
            return contextCorrelation;
        }
        final String headerCorrelation = request.getHeader("X-Correlation-Id");
        if (headerCorrelation != null && !headerCorrelation.isBlank()) {
            return headerCorrelation;
        }
        return UUID.randomUUID().toString();
    }

    private void writeForbiddenResponse(final HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
            {"exitoso":false,"codigo":"ACCESO_DENEGADO_POR_SEGURIDAD","mensajeUsuario":"No tienes permisos para acceder a este recurso segun la politica institucional de seguridad.","mensajeTecnico":"Acceso denegado por el Policy Enforcement Point (PEP)."}
        """.trim());
    }

    private void writeUnauthorizedResponse(final HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
            {"exitoso":false,"codigo":"TOKEN_INVALIDO_O_REVOCADO","mensajeUsuario":"Sesion invalida o revocada en el sistema de seguridad institucional.","mensajeTecnico":"Token rechazado por PEP/PDP."}
        """.trim());
    }

    private void writeServiceUnavailableResponse(final HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
            {"exitoso":false,"codigo":"SERVICIO_SEGURIDAD_NO_DISPONIBLE","mensajeUsuario":"El servicio de seguridad institucional no esta disponible temporalmente.","mensajeTecnico":"No fue posible comunicarse con el PEP/PDP."}
        """.trim());
    }

    private static final java.util.regex.Pattern UUID_PATTERN =
            java.util.regex.Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private static String normalizePath(final String uri) {
        if (uri == null) {
            return "";
        }
        return UUID_PATTERN.matcher(uri).replaceAll("{id}");
    }

    private static String escapeJson(final String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
