package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure.AzureAppConfigMessageCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure.AzureAppConfigParameterCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab.MessageUcoLabMessageCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.azure.AzureKeyVaultAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class CompositeCatalogInvalidationAdapterTest {

    private AzureAppConfigParameterCatalogAdapter azureParameters;
    private AzureAppConfigMessageCatalogAdapter azureMessages;
    private AzureKeyVaultAdapter azureVault;

    private final ParameterCatalogPort otherParameters = Mockito.mock(ParameterCatalogPort.class);
    private final MessageCatalogPort otherMessages = Mockito.mock(MessageCatalogPort.class);
    private final SecretVaultPort otherVault = Mockito.mock(SecretVaultPort.class);

    @BeforeEach
    void setUp() {
        azureParameters = Mockito.mock(AzureAppConfigParameterCatalogAdapter.class);
        azureMessages = Mockito.mock(AzureAppConfigMessageCatalogAdapter.class);
        azureVault = Mockito.mock(AzureKeyVaultAdapter.class);
    }

    private CompositeCatalogInvalidationAdapter withAzureAdapters() {
        return new CompositeCatalogInvalidationAdapter(azureParameters, azureMessages, azureVault);
    }

    private CompositeCatalogInvalidationAdapter withNonAzureAdapters() {
        return new CompositeCatalogInvalidationAdapter(otherParameters, otherMessages, otherVault);
    }

    @Test
    void invalidateParameter_delega_en_el_adaptador_azure() {
        withAzureAdapters().invalidateParameter("grupo", "clave");

        verify(azureParameters).invalidateParameter("grupo", "clave");
        verifyNoInteractions(azureMessages, azureVault);
    }

    @Test
    void invalidateParameter_no_hace_nada_si_el_proveedor_no_es_azure() {
        withNonAzureAdapters().invalidateParameter("grupo", "clave");

        verifyNoInteractions(otherParameters, otherMessages, otherVault);
    }

    @Test
    void invalidateMessage_delega_en_el_adaptador_azure() {
        withAzureAdapters().invalidateMessage("VAL-001");

        verify(azureMessages).invalidateMessage("VAL-001");
        verifyNoInteractions(azureParameters, azureVault);
    }

    @Test
    void invalidateMessage_no_hace_nada_si_el_proveedor_no_es_azure() {
        withNonAzureAdapters().invalidateMessage("VAL-001");

        verifyNoInteractions(otherParameters, otherMessages, otherVault);
    }

    @Test
    void invalidateSecret_delega_en_el_adaptador_azure() {
        withAzureAdapters().invalidateSecret("jwt-key");

        verify(azureVault).invalidateSecret("jwt-key");
        verifyNoInteractions(azureParameters, azureMessages);
    }

    @Test
    void invalidateSecret_no_hace_nada_si_el_proveedor_no_es_azure() {
        withNonAzureAdapters().invalidateSecret("jwt-key");

        verifyNoInteractions(otherParameters, otherMessages, otherVault);
    }

    @Test
    void invalidateAll_invalida_los_tres_adaptadores_azure() {
        withAzureAdapters().invalidateAll();

        verify(azureParameters).invalidateAll();
        verify(azureMessages).invalidateAll();
        verify(azureVault).invalidateAll();
        verifyNoMoreInteractions(azureParameters, azureMessages, azureVault);
    }

    @Test
    void invalidateAll_solo_invalida_los_adaptadores_que_son_azure() {
        new CompositeCatalogInvalidationAdapter(azureParameters, otherMessages, otherVault).invalidateAll();

        verify(azureParameters).invalidateAll();
        verifyNoInteractions(otherMessages, otherVault, azureMessages, azureVault);
    }

    @Test
    void invalidateAll_tolera_puertos_nulos() {
        new CompositeCatalogInvalidationAdapter(null, azureMessages, null).invalidateAll();

        verify(azureMessages).invalidateAll();
    }

    @Test
    void invalidacion_de_mensajes_delega_en_el_adaptador_messageucolab() {
        final MessageUcoLabMessageCatalogAdapter messageUcoLab = Mockito.mock(MessageUcoLabMessageCatalogAdapter.class);
        final CompositeCatalogInvalidationAdapter composite =
                new CompositeCatalogInvalidationAdapter(otherParameters, messageUcoLab, otherVault);

        composite.invalidateMessage("VAL_001");
        composite.invalidateAll();

        verify(messageUcoLab).invalidateMessage("VAL_001");
        verify(messageUcoLab).invalidateAll();
    }
}
