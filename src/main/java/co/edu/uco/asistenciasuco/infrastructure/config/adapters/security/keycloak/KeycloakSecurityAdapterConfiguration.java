package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security.keycloak;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation.AudienceValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation.InstitutionalIssuerValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation.RequiredUuidClaimValidator;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.KeycloakSecurityProviderProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Composition Root: selecciona Keycloak como proveedor de runtime security (validación de
 * JWT + resolución de claims). No registra reglas HTTP — eso sigue siendo responsabilidad de
 * {@code SecurityConfig}, que es neutral respecto al proveedor.
 *
 * <p>Para migrar a otro IdP en runtime: implementar {@link JwtClaimsExtractor}, crear sus
 * properties y una configuración equivalente a esta condicionada a
 * {@code app.adapters.security.provider=<nuevo>}. {@code SecurityConfig},
 * {@code InstitutionalJwtAuthenticationConverter} y {@code AuthenticatedUserResolver} no
 * cambian.</p>
 *
 * <p>El {@code JwtDecoder} que aquí se registra es, deliberadamente, el único dueño de la
 * política de validación (issuer + audience + claim UUID requerido) cuando el provider es
 * {@code keycloak}: NO lleva {@code @ConditionalOnMissingBean}, para que ningún otro bean
 * pueda sustituirlo silenciosamente y saltarse esa política. Un test que necesite reemplazar
 * el {@code JwtDecoder} debe hacerlo sin cargar esta clase, o habilitando explícitamente
 * {@code spring.main.allow-bean-definition-overriding=true} en su propio contexto.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.security",
        name = "provider",
        havingValue = "keycloak",
        matchIfMissing = true
)
@EnableConfigurationProperties(KeycloakSecurityProviderProperties.class)
public class KeycloakSecurityAdapterConfiguration {

    @Bean
    public JwtClaimsExtractor jwtClaimsExtractor(final KeycloakSecurityProviderProperties properties) {
        return new KeycloakJwtClaimsExtractor(properties.apiClientId(), properties.userIdClaim());
    }

    @Bean
    public JwtDecoder jwtDecoder(final KeycloakSecurityProviderProperties properties) {
        final String jwkSetUri;
        if (properties.issuerUri().contains("keycloak.localhost")) {
            jwkSetUri = properties.issuerUri().replace("keycloak.localhost", "localhost")
                    + "/protocol/openid-connect/certs";
        } else {
            jwkSetUri = properties.issuerUri().replaceAll("/+$", "") + "/protocol/openid-connect/certs";
        }
        final NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        final OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
        final OAuth2TokenValidator<Jwt> issuerValidator =
                new InstitutionalIssuerValidator(properties.issuerUri());
        final OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(properties.expectedAudience());
        final OAuth2TokenValidator<Jwt> userIdClaimValidator =
                new RequiredUuidClaimValidator(properties.userIdClaim());

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                issuerValidator,
                audienceValidator,
                userIdClaimValidator
        ));
        return jwtDecoder;
    }
}
