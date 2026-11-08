package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.InvalidableParameterCatalog;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** CULM-001 D.3: capacidad de edicion segun el proveedor ([SUPUESTO-D-06]) e invalidacion del proveedor sqlserver. */
class ParametroEdicionCapacidadAdapterTest {

    @Test
    void solo_el_proveedor_sqlserver_admite_la_edicion() {
        assertTrue(new ParametroEdicionCapacidadAdapter("sqlserver").admiteEdicion());
        assertTrue(new ParametroEdicionCapacidadAdapter(" SQLServer ").admiteEdicion());
        assertFalse(new ParametroEdicionCapacidadAdapter("azure_appconfig").admiteEdicion());
        assertFalse(new ParametroEdicionCapacidadAdapter(null).admiteEdicion());
    }

    interface CatalogoInvalidable extends ParameterCatalogPort, InvalidableParameterCatalog {
    }

    @Test
    void la_invalidacion_llega_al_proveedor_de_parametros_invalidable() {
        final CatalogoInvalidable catalogo = mock(CatalogoInvalidable.class);
        final CatalogInvalidationPort invalidacion = new CompositeCatalogInvalidationAdapter(catalogo, mock(MessageCatalogPort.class),
                mock(SecretVaultPort.class));

        invalidacion.invalidateParameter("ASISTENCIA", "TOLERANCIA_MINUTOS");
        invalidacion.invalidateAll();

        verify(catalogo).invalidateParameter("ASISTENCIA", "TOLERANCIA_MINUTOS");
        verify(catalogo).invalidateAll();
    }

    @Test
    void un_proveedor_no_invalidable_se_ignora_sin_error() {
        final ParameterCatalogPort plano = new ParameterCatalogPort() {
            @Override public Optional<String> getParameter(final String group, final String key) { return Optional.empty(); }
            @Override public String getRequiredParameter(final String group, final String key) { return ""; }
            @Override public <T> T getParameterAs(final String group, final String key, final Class<T> targetType) { return null; }
        };
        final CatalogInvalidationPort invalidacion = new CompositeCatalogInvalidationAdapter(plano, mock(MessageCatalogPort.class), mock(SecretVaultPort.class));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> invalidacion.invalidateParameter("A", "B"));
    }
}
