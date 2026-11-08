package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure.AzureAppConfigMessageCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.InvalidableParameterCatalog;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab.MessageUcoLabMessageCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.azure.AzureKeyVaultAdapter;

/**
 * Adaptador compuesto que implementa {@link CatalogInvalidationPort} delegando en los adaptadores
 * activos de parámetros, mensajes y secretos si estos soportan invalidación.
 */
public class CompositeCatalogInvalidationAdapter implements CatalogInvalidationPort {

    private final ParameterCatalogPort parameterCatalogPort;
    private final MessageCatalogPort messageCatalogPort;
    private final SecretVaultPort secretVaultPort;

    public CompositeCatalogInvalidationAdapter(
            final ParameterCatalogPort parameterCatalogPort,
            final MessageCatalogPort messageCatalogPort,
            final SecretVaultPort secretVaultPort
    ) {
        this.parameterCatalogPort = parameterCatalogPort;
        this.messageCatalogPort = messageCatalogPort;
        this.secretVaultPort = secretVaultPort;
    }

    @Override
    public void invalidateParameter(final String group, final String key) {
        if (parameterCatalogPort instanceof InvalidableParameterCatalog adapter) {
            adapter.invalidateParameter(group, key);
        }
    }

    @Override
    public void invalidateMessage(final String code) {
        if (messageCatalogPort instanceof AzureAppConfigMessageCatalogAdapter adapter) {
            adapter.invalidateMessage(code);
        } else if (messageCatalogPort instanceof MessageUcoLabMessageCatalogAdapter adapter) {
            adapter.invalidateMessage(code);
        }
    }

    @Override
    public void invalidateSecret(final String secretName) {
        if (secretVaultPort instanceof AzureKeyVaultAdapter adapter) {
            adapter.invalidateSecret(secretName);
        }
    }

    @Override
    public void invalidateAll() {
        if (parameterCatalogPort instanceof InvalidableParameterCatalog adapter) {
            adapter.invalidateAll();
        }
        if (messageCatalogPort instanceof AzureAppConfigMessageCatalogAdapter adapter) {
            adapter.invalidateAll();
        } else if (messageCatalogPort instanceof MessageUcoLabMessageCatalogAdapter adapter) {
            adapter.invalidateAll();
        }
        if (secretVaultPort instanceof AzureKeyVaultAdapter adapter) {
            adapter.invalidateAll();
        }
    }
}
