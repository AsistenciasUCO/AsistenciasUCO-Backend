package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.actualizarparametro.primaryports.ActualizarParametroInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarparametro.usecase.impl.ActualizarParametroUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcambiosparametro.primaryports.ConsultarCambiosParametroInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcambiosparametro.usecase.impl.ConsultarCambiosParametroUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.ConsultarParametrosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.impl.ConsultarParametrosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParametroEdicionCapacidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite.ParametroEdicionCapacidadAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root de la edicion de parametros por el administrador (CULM-001 D.3, HU168). */
@Configuration(proxyBeanMethods = false)
public class ParametrosAdminWiringConfiguration {

    @Bean
    public ParametroEdicionCapacidadPort parametroEdicionCapacidadPort(
            @Value("${app.adapters.parameter-catalog.provider:azure_appconfig}") final String proveedor) {
        return new ParametroEdicionCapacidadAdapter(proveedor);
    }

    @Bean
    public ConsultarParametrosInputPort consultarParametrosInputPort(final ParametroAdminRepositoryPort repositorio) {
        return new ConsultarParametrosUseCaseImpl(repositorio);
    }

    @Bean
    public ActualizarParametroInputPort actualizarParametroInputPort(final ParametroAdminRepositoryPort repositorio,
                                                                     final ParametroEdicionCapacidadPort capacidad,
                                                                     final CatalogInvalidationPort invalidacion) {
        return new ActualizarParametroUseCaseImpl(repositorio, capacidad, invalidacion);
    }

    @Bean
    public ConsultarCambiosParametroInputPort consultarCambiosParametroInputPort(final ParametroAdminRepositoryPort repositorio) {
        return new ConsultarCambiosParametroUseCaseImpl(repositorio);
    }
}
