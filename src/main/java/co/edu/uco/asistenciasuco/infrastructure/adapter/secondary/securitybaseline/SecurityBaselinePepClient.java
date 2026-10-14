package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePepProperties;
import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

/**
 * Adaptador secundario del endpoint PEP embebido de Security Baseline. No conoce PDP, roles ni
 * identidad institucional: solo traduce el contrato HTTP del proveedor a un resultado interno.
 */
public class SecurityBaselinePepClient {

    private static final String AUTHORIZATION = "Authorization";
    private static final String CORRELATION_ID = "X-Correlation-Id";
    private static final String REQUEST_ID = "X-Request-Id";
    private static final String APPLICATION_CREDENTIAL = "X-Application-Credential";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SecurityBaselinePepProperties properties;

    public SecurityBaselinePepClient(
            final HttpClient httpClient,
            final ObjectMapper objectMapper,
            final SecurityBaselinePepProperties properties
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public PepDecision evaluate(final HttpServletRequest request, final String correlationId) {
        try {
            final String requestId = UUID.randomUUID().toString();
            final String effectiveCorrelationId = correlationId == null || correlationId.isBlank()
                    ? requestId : correlationId;
            final String body = objectMapper.writeValueAsString(new EmbeddedAccessDecisionRawRequest(
                    properties.applicationName(),
                    properties.environment(),
                    request.getRequestURI(),
                    request.getMethod()
            ));
            final HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(properties.embeddedDecisionEndpoint())
                    .timeout(properties.requestTimeout())
                    .header("Content-Type", "application/json")
                    .header(APPLICATION_CREDENTIAL, properties.applicationCredential())
                    .header(REQUEST_ID, requestId)
                    .header(CORRELATION_ID, effectiveCorrelationId);
            final String authorization = request.getHeader(AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                builder.header(AUTHORIZATION, authorization);
            }

            final HttpResponse<String> response = httpClient.send(
                    builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            return mapResponse(response);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            return unavailable();
        } catch (final IOException | RuntimeException exception) {
            return unavailable();
        }
    }

    private PepDecision mapResponse(final HttpResponse<String> response) {
        final int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return new PepDecision(PepDecision.Outcome.ALLOW,
                    response.headers().firstValue("X-Decision-Id").orElse(null));
        }
        if (status == 401) {
            return new PepDecision(PepDecision.Outcome.UNAUTHORIZED, null);
        }
        if (status == 403) {
            return new PepDecision(PepDecision.Outcome.FORBIDDEN, null);
        }
        return unavailable();
    }

    private PepDecision unavailable() {
        return new PepDecision(PepDecision.Outcome.UNAVAILABLE, null);
    }

    private record EmbeddedAccessDecisionRawRequest(
            String applicationName,
            String environment,
            String path,
            String method
    ) {
    }
}
