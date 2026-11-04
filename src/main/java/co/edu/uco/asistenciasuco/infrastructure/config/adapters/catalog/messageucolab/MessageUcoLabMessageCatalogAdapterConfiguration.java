package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.messageucolab;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure.AzureAppConfigMessageCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab.MessageUcoLabCodeMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab.MessageUcoLabMessageCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.AzureAppConfigProviderProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.MessageUcoLabProviderProperties;
import co.edu.uco.messageucolab.client.api.MessageReader;
import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root del catalogo de mensajes sobre MessageUcoLab.
 *
 * <p>Se activa unicamente cuando {@code app.adapters.message-catalog.provider=messageucolab}. El
 * {@link MessageReader} lo registra el starter oficial (configuracion {@code messageucolab.*}).</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.adapters.message-catalog", name = "provider", havingValue = "messageucolab")
@EnableConfigurationProperties({MessageUcoLabProviderProperties.class, AzureAppConfigProviderProperties.class})
public class MessageUcoLabMessageCatalogAdapterConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageUcoLabMessageCatalogAdapterConfiguration.class);

    // Sin @Primary: los tres proveedores (azure, sqlserver, messageucolab) son mutuamente excluyentes por
    // @ConditionalOnProperty, asi que en ejecucion solo existe un MessageCatalogPort. Un segundo @Primary (el de Azure ya lo
    // tiene) hace que el analisis estatico (SonarCloud) vea los puntos de inyeccion como ambiguos.
    @Bean
    public MessageCatalogPort messageUcoLabMessageCatalogPort(
            final MessageReader messageReader,
            final MessageUcoLabProviderProperties properties,
            final AzureAppConfigProviderProperties azureProperties
    ) {
        final MessageCatalogPort fallback = properties.fallback() == MessageUcoLabProviderProperties.Fallback.AZURE
                ? azureFallback(azureProperties)
                : null;
        LOGGER.info("Catalogo de mensajes activo: MessageUcoLab (timeout={}, cacheTtl={}, contingencia={}, alias={})",
                properties.timeout(), properties.cacheTtl(), properties.fallback(), properties.codeAliases().size());
        return new MessageUcoLabMessageCatalogAdapter(
                messageReader,
                new MessageUcoLabCodeMapper(properties.codePrefix(), properties.codeAliases()),
                properties.timeout(),
                properties.cacheTtl(),
                fallback
        );
    }

    private static MessageCatalogPort azureFallback(final AzureAppConfigProviderProperties azureProperties) {
        final ConfigurationClient client = new ConfigurationClientBuilder()
                .endpoint(azureProperties.endpoint())
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        return new AzureAppConfigMessageCatalogAdapter(client);
    }
}
