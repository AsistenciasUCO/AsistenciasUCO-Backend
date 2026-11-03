package co.edu.uco.asistenciasuco.infrastructure.config.properties;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.MessageCatalogAdapterProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.ParameterCatalogAdapterProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.VaultAdapterProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.AzureAppConfigProviderProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.AzureKeyVaultProviderProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.MessageUcoLabProviderProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las properties fallan al arrancar (fail-fast) cuando falta la seleccion de proveedor o el
 * endpoint cloud: es preferible que el contexto no levante a operar con un catalogo/vault indefinido.
 */
class ProviderAndAdapterPropertiesTest {

    @Test
    void message_catalog_exige_proveedor_y_lo_conserva() {
        assertThrows(NullPointerException.class, () -> new MessageCatalogAdapterProperties(null));

        assertEquals(MessageCatalogAdapterProperties.Provider.AZURE,
                new MessageCatalogAdapterProperties(MessageCatalogAdapterProperties.Provider.AZURE).provider());
        assertEquals(MessageCatalogAdapterProperties.Provider.SQLSERVER,
                new MessageCatalogAdapterProperties(MessageCatalogAdapterProperties.Provider.SQLSERVER).provider());
        assertEquals(MessageCatalogAdapterProperties.Provider.MESSAGEUCOLAB,
                new MessageCatalogAdapterProperties(MessageCatalogAdapterProperties.Provider.MESSAGEUCOLAB).provider());
    }

    @Test
    void messageucolab_aplica_valores_por_defecto_y_rechaza_timeout_invalido() {
        final MessageUcoLabProviderProperties defaults = new MessageUcoLabProviderProperties(null, null, null, null, null);

        assertEquals(java.time.Duration.ofSeconds(3), defaults.timeout());
        assertEquals(java.time.Duration.ofMinutes(5), defaults.cacheTtl());
        assertThrows(IllegalStateException.class,
                () -> new MessageUcoLabProviderProperties(null, java.time.Duration.ofSeconds(-1), null, null, null));
        assertEquals(MessageUcoLabProviderProperties.Fallback.AZURE, defaults.fallback());
        assertEquals("MSG_", defaults.codePrefix());
        assertTrue(defaults.codeAliases().isEmpty());
        assertThrows(IllegalStateException.class,
                () -> new MessageUcoLabProviderProperties(java.time.Duration.ZERO, null, null, null, null));
        assertThrows(IllegalStateException.class,
                () -> new MessageUcoLabProviderProperties(java.time.Duration.ofSeconds(-1), null, null, null, null));
    }

    @Test
    void parameter_catalog_exige_proveedor_y_lo_conserva() {
        assertThrows(NullPointerException.class, () -> new ParameterCatalogAdapterProperties(null));

        for (final ParameterCatalogAdapterProperties.Provider provider : ParameterCatalogAdapterProperties.Provider.values()) {
            assertEquals(provider, new ParameterCatalogAdapterProperties(provider).provider());
        }
    }

    @Test
    void vault_exige_proveedor_y_lo_conserva() {
        assertThrows(NullPointerException.class, () -> new VaultAdapterProperties(null));

        for (final VaultAdapterProperties.Provider provider : VaultAdapterProperties.Provider.values()) {
            assertEquals(provider, new VaultAdapterProperties(provider).provider());
        }
    }

    @Test
    void azure_app_config_exige_endpoint_no_vacio() {
        assertEquals("https://uco.azconfig.io", new AzureAppConfigProviderProperties("https://uco.azconfig.io").endpoint());

        for (final String invalid : new String[]{null, "", "   "}) {
            final IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> new AzureAppConfigProviderProperties(invalid));
            assertTrue(ex.getMessage().contains("azure-appconfig.endpoint"));
        }
    }

    @Test
    void azure_key_vault_exige_endpoint_no_vacio() {
        assertEquals("https://uco.vault.azure.net", new AzureKeyVaultProviderProperties("https://uco.vault.azure.net").endpoint());

        for (final String invalid : new String[]{null, "", "   "}) {
            final IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> new AzureKeyVaultProviderProperties(invalid));
            assertTrue(ex.getMessage().contains("azure-keyvault.endpoint"));
        }
    }
}
