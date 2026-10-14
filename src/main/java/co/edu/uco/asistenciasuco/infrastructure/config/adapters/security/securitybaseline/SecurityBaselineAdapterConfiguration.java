package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security.securitybaseline;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.securitybaseline.SecurityBaselineJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation.AudienceValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation.InstitutionalIssuerValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation.RequiredUuidClaimValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.pep.PepEnforcementFilter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselineProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Composition Root para el proveedor de seguridad "Security Baseline" (Keycloak local + PDP/PEP).
 * Seleccionable mediante {@code app.adapters.security.provider=security-baseline}.
 * Registra el {@link JwtDecoder}, {@link JwtClaimsExtractor} y el filtro de autorización PEP.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.security",
        name = "provider",
        havingValue = "security-baseline"
)
@EnableConfigurationProperties(SecurityBaselineProperties.class)
public class SecurityBaselineAdapterConfiguration {

    @Bean
    public JwtClaimsExtractor jwtClaimsExtractor(final SecurityBaselineProperties properties) {
        return new SecurityBaselineJwtClaimsExtractor(properties.apiClientId(), properties.userIdClaim());
    }

    @Bean
    public JwtDecoder jwtDecoder(final SecurityBaselineProperties properties) {
        final NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(properties.issuerUri());

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

    @Bean
    @ConditionalOnProperty(
            prefix = "app.providers.security-baseline",
            name = "pep-enforcement-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public FilterRegistrationBean<PepEnforcementFilter> pepEnforcementFilterRegistration(
            final SecurityBaselineProperties properties
    ) {
        final PepEnforcementFilter filter = new PepEnforcementFilter(
                properties.pepUrl(),
                properties.applicationName(),
                properties.environment(),
                properties.registrationToken(),
                properties.publicPaths()
        );

        final FilterRegistrationBean<PepEnforcementFilter> registrationBean = new FilterRegistrationBean<>(filter);
        registrationBean.addUrlPatterns("/api/v1/*");
        // Prioridad alta para validar permisos antes de la ejecución del negocio
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        return registrationBean;
    }
}
