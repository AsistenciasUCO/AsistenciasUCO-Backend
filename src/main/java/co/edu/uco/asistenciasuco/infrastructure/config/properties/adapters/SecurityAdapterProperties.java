package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "app.adapters.security")
public record SecurityAdapterProperties(Provider provider) {

    public SecurityAdapterProperties {
        Objects.requireNonNull(provider, "app.adapters.security.provider es obligatorio.");
    }

    public enum Provider {
        KEYCLOAK,
        SECURITY_BASELINE
    }
}
