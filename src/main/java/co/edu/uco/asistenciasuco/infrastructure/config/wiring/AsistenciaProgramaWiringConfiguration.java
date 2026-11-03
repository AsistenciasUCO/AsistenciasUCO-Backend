package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.ConsultarAsistenciaProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.interactor.ConsultarAsistenciaProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.ConsultarAsistenciaProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.impl.ConsultarAsistenciaProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Consultas de solo lectura del coordinador (HU064/HU060/HU061). Archivo propio para no tocar wirings compartidos. */
@Configuration(proxyBeanMethods = false)
public class AsistenciaProgramaWiringConfiguration {

    @Bean ConsultarAsistenciaProgramaUseCase consultarAsistenciaProgramaUseCase(
            final AsistenciaProgramaQueryPort queryPort, final InstitutionalScopePort scopePort,
            final SesionProcedenciaQueryPort procedenciaPort) {
        return new ConsultarAsistenciaProgramaUseCaseImpl(queryPort, scopePort, procedenciaPort);
    }

    @Bean ConsultarAsistenciaProgramaInputPort consultarAsistenciaProgramaInputPort(
            final ConsultarAsistenciaProgramaUseCase useCase) {
        return new ConsultarAsistenciaProgramaInteractor(useCase);
    }
}
