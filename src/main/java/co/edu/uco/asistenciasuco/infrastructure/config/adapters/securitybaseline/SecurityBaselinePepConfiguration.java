package co.edu.uco.asistenciasuco.infrastructure.config.adapters.securitybaseline;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline.SecurityBaselinePepClient;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePepProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityBaselinePepFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

/** Composition Root aislado del enforcement experimental Security Baseline. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.providers.security-baseline-pep",
        name = "enabled",
        havingValue = "true"
)
@EnableConfigurationProperties(SecurityBaselinePepProperties.class)
public class SecurityBaselinePepConfiguration {

    @Bean
    public SecurityBaselinePepClient securityBaselinePepClient(
            final SecurityBaselinePepProperties properties,
            final tools.jackson.databind.ObjectMapper objectMapper
    ) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        return new SecurityBaselinePepClient(httpClient, objectMapper, properties);
    }

    @Bean
    public SecurityBaselinePepFilter securityBaselinePepFilter(
            final SecurityBaselinePepClient client,
            final SecurityBaselinePepProperties properties,
            final SecurityErrorResponseWriter responseWriter
    ) {
        return new SecurityBaselinePepFilter(client, properties, responseWriter);
    }
}
