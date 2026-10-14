package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propiedades de configuración para el proveedor de seguridad "Security Baseline" (PDP/PEP + Keycloak local).
 */
@ConfigurationProperties(prefix = "app.providers.security-baseline")
public record SecurityBaselineProperties(
        String issuerUri,
        String apiClientId,
        String expectedAudience,
        String userIdClaim,
        String pepUrl,
        String applicationName,
        String environment,
        String registrationToken,
        boolean allowInsecureHttp,
        boolean pepEnforcementEnabled,
        List<String> publicPaths
) {

    public SecurityBaselineProperties {
        if (issuerUri == null || issuerUri.isBlank()) {
            issuerUri = "http://keycloak.localhost:19090/realms/security-baseline";
        }
        if (apiClientId == null || apiClientId.isBlank()) {
            apiClientId = "security-baseline-bff";
        }
        if (expectedAudience == null || expectedAudience.isBlank()) {
            expectedAudience = "security-baseline-pdp";
        }
        if (userIdClaim == null || userIdClaim.isBlank()) {
            userIdClaim = "sub";
        }
        if (pepUrl == null || pepUrl.isBlank()) {
            pepUrl = "http://localhost:18081";
        }
        if (applicationName == null || applicationName.isBlank()) {
            applicationName = "asistencias-backend";
        }
        if (environment == null || environment.isBlank()) {
            environment = "local";
        }
        if (publicPaths == null || publicPaths.isEmpty()) {
            publicPaths = List.of(
                    "/actuator/**",
                    "/swagger-ui",
                    "/swagger-ui/**",
                    "/openapi/**",
                    "/api/v1/internal/azure-events"
            );
        }
    }
}
