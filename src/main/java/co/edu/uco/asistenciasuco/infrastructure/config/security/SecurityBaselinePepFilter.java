package co.edu.uco.asistenciasuco.infrastructure.config.security;

import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.PepDecision;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.SecurityBaselinePepClient;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePepProperties;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Enforcement servlet opt-in. El filtro agrega una decisión PEP; no autentica ni concede roles.
 */
public final class SecurityBaselinePepFilter extends OncePerRequestFilter {

    private static final String DECISION_ID = "X-Decision-Id";
    private static final String CORRELATION_ID = "X-Correlation-Id";

    private final SecurityBaselinePepClient client;
    private final SecurityBaselinePepProperties properties;
    private final SecurityErrorResponseWriter responseWriter;

    public SecurityBaselinePepFilter(
            final SecurityBaselinePepClient client,
            final SecurityBaselinePepProperties properties,
            final SecurityErrorResponseWriter responseWriter
    ) {
        this.client = client;
        this.properties = properties;
        this.responseWriter = responseWriter;
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return !properties.enabled() || !isProtectedPath(request.getRequestURI())
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        final PepDecision decision = client.evaluate(request, correlationId(request));
        switch (decision.outcome()) {
            case ALLOW -> {
                if (decision.decisionId() != null && !decision.decisionId().isBlank()) {
                    response.setHeader(DECISION_ID, decision.decisionId());
                }
                filterChain.doFilter(request, response);
            }
            case UNAUTHORIZED -> responseWriter.write(
                    request, response, org.springframework.http.HttpStatus.UNAUTHORIZED,
                    SecurityErrorCode.UNAUTHORIZED);
            case FORBIDDEN -> responseWriter.write(
                    request, response, org.springframework.http.HttpStatus.FORBIDDEN,
                    SecurityErrorCode.FORBIDDEN);
            case UNAVAILABLE -> responseWriter.write(
                    request, response, org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    CommonErrorCode.INTERNAL_ERROR);
        }
    }

    private boolean isAuthenticated() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String correlationId(final HttpServletRequest request) {
        final String header = request.getHeader(CORRELATION_ID);
        if (header != null && !header.isBlank()) {
            return header;
        }
        final String contextValue = CorrelationIdContext.getAsString();
        return contextValue == null || contextValue.isBlank() ? UUID.randomUUID().toString() : contextValue;
    }

    private boolean isProtectedPath(final String path) {
        return path != null && (path.startsWith("/api/v1/") || "/api/v1".equals(path)
                || path.startsWith("/api/v2/sesiones/") || "/api/v2/sesiones".equals(path));
    }
}
