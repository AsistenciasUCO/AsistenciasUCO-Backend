package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.ConsultarFacultadDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.interactor.ConsultarFacultadDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.ConsultarFacultadDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.impl.ConsultarFacultadDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Consultas de solo lectura del decano (HU104/HU105/HU109/HU119/HU145). Archivo propio para no tocar wirings compartidos. */
@Configuration(proxyBeanMethods = false)
public class FacultadDecanoWiringConfiguration {

    @Bean ConsultarFacultadDecanoUseCase consultarFacultadDecanoUseCase(
            final FacultadDecanoQueryPort queryPort, final InstitutionalScopePort scopePort,
            final SesionProcedenciaQueryPort procedenciaPort) {
        return new ConsultarFacultadDecanoUseCaseImpl(queryPort, scopePort, procedenciaPort);
    }

    @Bean ConsultarFacultadDecanoInputPort consultarFacultadDecanoInputPort(final ConsultarFacultadDecanoUseCase useCase) {
        return new ConsultarFacultadDecanoInteractor(useCase);
    }
}
