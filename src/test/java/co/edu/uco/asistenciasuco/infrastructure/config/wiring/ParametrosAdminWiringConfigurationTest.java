package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.actualizarparametro.usecase.impl.ActualizarParametroUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcambiosparametro.usecase.impl.ConsultarCambiosParametroUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.impl.ConsultarParametrosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ParametrosAdminWiringConfigurationTest {

    private final ParametrosAdminWiringConfiguration config = new ParametrosAdminWiringConfiguration();

    @Test
    void cablea_los_tres_casos_de_uso_y_la_capacidad_segun_el_proveedor() {
        final ParametroAdminRepositoryPort repositorio = mock(ParametroAdminRepositoryPort.class);
        final var capacidad = config.parametroEdicionCapacidadPort("sqlserver");

        assertTrue(capacidad.admiteEdicion());
        assertFalse(config.parametroEdicionCapacidadPort("azure_appconfig").admiteEdicion());
        assertInstanceOf(ConsultarParametrosUseCaseImpl.class, config.consultarParametrosInputPort(repositorio));
        assertInstanceOf(ActualizarParametroUseCaseImpl.class,
                config.actualizarParametroInputPort(repositorio, capacidad, mock(CatalogInvalidationPort.class)));
        assertInstanceOf(ConsultarCambiosParametroUseCaseImpl.class, config.consultarCambiosParametroInputPort(repositorio));
    }
}
