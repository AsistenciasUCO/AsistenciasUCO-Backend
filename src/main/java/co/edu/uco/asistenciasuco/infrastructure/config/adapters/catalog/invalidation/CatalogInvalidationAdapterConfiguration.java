package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.invalidation;

import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.composite.ParametroConValorPorDefectoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.primaryports.ProcesarEventoAzureInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.usecase.ProcesarEventoAzureUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.usecase.impl.ProcesarEventoAzureUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite.CompositeCatalogInvalidationAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root para el adaptador de invalidación y el caso de uso de eventos Azure.
 */
@Configuration(proxyBeanMethods = false)
public class CatalogInvalidationAdapterConfiguration {

    @Bean
    public CatalogInvalidationPort catalogInvalidationPort(
            // CULM-001 A.2-6: la invalidacion de cache actua sobre el proveedor real, no sobre el decorador.
            @Qualifier(ParametroConValorPorDefectoConfiguration.DELEGADO) final ParameterCatalogPort parameterCatalogPort,
            final MessageCatalogPort messageCatalogPort,
            final SecretVaultPort secretVaultPort
    ) {
        return new CompositeCatalogInvalidationAdapter(parameterCatalogPort, messageCatalogPort, secretVaultPort);
    }

    @Bean
    public ProcesarEventoAzureInputPort procesarEventoAzureInputPort(
            final CatalogInvalidationPort catalogInvalidationPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new ProcesarEventoAzureUseCaseImpl(catalogInvalidationPort, realtimePublisherPort);
    }
}
