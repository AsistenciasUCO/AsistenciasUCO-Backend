package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * Configuración opt-in del PEP de Security Baseline. Cuando está desactivada no se exige ningún
 * dato del proveedor y el backend conserva exclusivamente su camino JWT actual.
 */
@ConfigurationProperties(prefix = "app.providers.security-baseline-pep")
public record SecurityBaselinePepProperties(
        boolean enabled,
        URI pepUrl,
        String applicationName,
        String environment,
        String applicationCredential,
        Duration connectTimeout,
        Duration requestTimeout
) {

    public SecurityBaselinePepProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(5) : requestTimeout;
        if (enabled) {
            requireHttpUri(pepUrl, "app.providers.security-baseline-pep.pep-url");
            requireNonBlank(applicationName, "app.providers.security-baseline-pep.application-name");
            requireNonBlank(environment, "app.providers.security-baseline-pep.environment");
            requireNonBlank(applicationCredential, "app.providers.security-baseline-pep.application-credential");
            requirePositive(connectTimeout, "app.providers.security-baseline-pep.connect-timeout");
            requirePositive(requestTimeout, "app.providers.security-baseline-pep.request-timeout");
        }
    }

    public URI embeddedDecisionEndpoint() {
        return pepUrl.resolve("/internal/v1/embedded-access-decisions");
    }

    private static void requireHttpUri(final URI value, final String property) {
        if (value == null || value.getScheme() == null
                || (!"http".equalsIgnoreCase(value.getScheme()) && !"https".equalsIgnoreCase(value.getScheme()))
                || value.getHost() == null) {
            throw new IllegalStateException(property + " debe ser una URL HTTP(S) absoluta.");
        }
    }

    private static void requireNonBlank(final String value, final String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " es obligatorio cuando el flag está activo.");
        }
    }

    private static void requirePositive(final Duration value, final String property) {
        if (value.isZero() || value.isNegative()) {
            throw new IllegalStateException(property + " debe ser positiva.");
        }
    }
}
