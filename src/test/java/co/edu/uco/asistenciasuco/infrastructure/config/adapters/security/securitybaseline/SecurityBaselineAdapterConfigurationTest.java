package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security.securitybaseline;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.securitybaseline.SecurityBaselineJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.pep.PepEnforcementFilter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselineProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityBaselineAdapterConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    SecurityBaselineAdapterConfiguration.class,
                    FakeJwtDecoderConfiguration.class
            )
            .withAllowBeanDefinitionOverriding(true)
            .withPropertyValues(
                    "app.adapters.security.provider=security-baseline",
                    "app.providers.security-baseline.issuer-uri=http://keycloak.localhost:19090/realms/security-baseline",
                    "app.providers.security-baseline.api-client-id=security-baseline-bff",
                    "app.providers.security-baseline.expected-audience=security-baseline-pdp",
                    "app.providers.security-baseline.user-id-claim=sub",
                    "app.providers.security-baseline.pep-url=http://localhost:18081",
                    "app.providers.security-baseline.application-name=asistencias-backend",
                    "app.providers.security-baseline.registration-token=test-token"
            );

    @Test
    void registra_jwtClaimsExtractor_security_baseline() {
        contextRunner.run(context -> {
            assertEquals(1, context.getBeansOfType(JwtClaimsExtractor.class).size());
            assertInstanceOf(SecurityBaselineJwtClaimsExtractor.class, context.getBean(JwtClaimsExtractor.class));
        });
    }

    @Test
    void registra_properties_tipadas() {
        contextRunner.run(context -> {
            final SecurityBaselineProperties properties = context.getBean(SecurityBaselineProperties.class);
            assertEquals("asistencias-backend", properties.applicationName());
            assertEquals("sub", properties.userIdClaim());
            assertEquals("http://localhost:18081", properties.pepUrl());
            assertEquals("test-token", properties.registrationToken());
        });
    }

    @Test
    void registra_filtro_pep() {
        contextRunner.run(context -> {
            assertTrue(context.containsBean("pepEnforcementFilterRegistration"));
            final FilterRegistrationBean<?> regBean = context.getBean(FilterRegistrationBean.class);
            assertInstanceOf(PepEnforcementFilter.class, regBean.getFilter());
        });
    }

    @Test
    void provider_distinto_no_registra_beans() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        SecurityBaselineAdapterConfiguration.class,
                        FakeJwtDecoderConfiguration.class
                )
                .withAllowBeanDefinitionOverriding(true)
                .withPropertyValues(
                        "app.adapters.security.provider=keycloak"
                )
                .run(context -> {
                    assertTrue(context.getBeansOfType(SecurityBaselineProperties.class).isEmpty());
                    assertFalse(context.containsBean("pepEnforcementFilterRegistration"));
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class FakeJwtDecoderConfiguration {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("Fake JwtDecoder for test.");
            };
        }
    }
}
