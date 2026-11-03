package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "app.adapters.message-catalog")
public record MessageCatalogAdapterProperties(Provider provider) {

    public MessageCatalogAdapterProperties {
        Objects.requireNonNull(provider, "app.adapters.message-catalog.provider es obligatorio.");
    }

    public enum Provider {
        SQLSERVER,
        AZURE,
        /** Componente externo MessageUcoLab (equipo de mensajeria). */
        MESSAGEUCOLAB
    }
}
