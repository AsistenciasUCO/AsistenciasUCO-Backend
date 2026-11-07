package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.azure;

import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.composite.ParametroConValorPorDefectoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure.AzureAppConfigParameterCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.AzureAppConfigProviderProperties;
import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root para el proveedor Azure App Configuration.
 *
 * <p>Se activa unicamente cuando {@code app.adapters.parameter-catalog.provider=azure_appconfig}.</p>
 * <p>Utiliza {@link DefaultAzureCredentialBuilder} respetando la directiva institucional.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.parameter-catalog",
        name = "provider",
        havingValue = "azure_appconfig",
        matchIfMissing = true
)
@EnableConfigurationProperties(AzureAppConfigProviderProperties.class)
public class AzureAppConfigAdapterConfiguration {

    /** CULM-001 A.2-6: proveedor real; los consumidores reciben el decorador con valores por defecto (DEC-018). */
    @Bean
    @Qualifier(ParametroConValorPorDefectoConfiguration.DELEGADO)
    public ParameterCatalogPort parameterCatalogPort(final AzureAppConfigProviderProperties properties) {
        final ConfigurationClient client = new ConfigurationClientBuilder()
                .endpoint(properties.endpoint())
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        return new AzureAppConfigParameterCatalogAdapter(client);
    }
}
