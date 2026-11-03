package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure.AzureAppConfigMessageCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab.MessageUcoLabMessageCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.azure.AzureAppConfigMessageCatalogAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.messageucolab.MessageUcoLabMessageCatalogAdapterConfiguration;
import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.autoconfigure.MessageUcoLabAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * El selector {@code app.adapters.message-catalog.provider} elige el adaptador sin tocar Application, y el starter
 * oficial de MessageUcoLab (que se autoconfigura siempre) no impide arrancar con otro provider.
 */
class MessageUcoLabMessageCatalogWiringTest {

    private static final String[] CONEXION = {
            "messageucolab.url=http://localhost:8000",
            "messageucolab.application-id=11111111-1111-1111-1111-111111111111",
            "messageucolab.environment-id=22222222-2222-2222-2222-222222222222",
            "app.providers.azure-appconfig.endpoint=https://uco-test.azconfig.io"
    };

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebClientAutoConfiguration.class, MessageUcoLabAutoConfiguration.class))
            .withUserConfiguration(
                    MessageUcoLabMessageCatalogAdapterConfiguration.class,
                    AzureAppConfigMessageCatalogAdapterConfiguration.class);

    @Test
    void provider_messageucolab_registra_el_adaptador_de_messageucolab_con_fallback_azure() {
        runner.withPropertyValues(CONEXION).withPropertyValues("app.adapters.message-catalog.provider=messageucolab")
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(MessageCatalogPort.class).size());
                    assertInstanceOf(MessageUcoLabMessageCatalogAdapter.class, context.getBean(MessageCatalogPort.class));
                    assertNotNull(context.getBean(MessageReader.class));
                });
    }

    @Test
    void provider_messageucolab_sin_fallback_tambien_arranca() {
        runner.withPropertyValues(CONEXION).withPropertyValues(
                        "app.adapters.message-catalog.provider=messageucolab",
                        "app.providers.messageucolab.fallback=none",
                        "app.providers.messageucolab.timeout=5s",
                        "app.providers.messageucolab.code-aliases.ERR_CORRELACION_REQUERIDA=CORR_REQ")
                .run(context -> assertInstanceOf(MessageUcoLabMessageCatalogAdapter.class, context.getBean(MessageCatalogPort.class)));
    }

    @Test
    void provider_azure_registra_azure_y_no_el_adaptador_de_messageucolab() {
        runner.withPropertyValues(CONEXION).withPropertyValues("app.adapters.message-catalog.provider=azure")
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(MessageCatalogPort.class).size());
                    assertInstanceOf(AzureAppConfigMessageCatalogAdapter.class, context.getBean(MessageCatalogPort.class));
                });
    }

    @Test
    void starter_con_valores_por_defecto_del_yml_arranca_aunque_el_provider_sea_azure() {
        runner.withPropertyValues(
                        "messageucolab.url=http://localhost:8000",
                        "messageucolab.application-id=",
                        "messageucolab.environment-id=",
                        "app.providers.azure-appconfig.endpoint=https://uco-test.azconfig.io",
                        "app.adapters.message-catalog.provider=azure")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertInstanceOf(AzureAppConfigMessageCatalogAdapter.class, context.getBean(MessageCatalogPort.class));
                });
    }
}
