package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.ActualizarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.interactor.ActualizarSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.ActualizarSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.impl.ActualizarSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.interactor.CerrarSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.CerrarSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.impl.CerrarSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.interactor.ConsultarSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.ConsultarSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.impl.ConsultarSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.ConsultarSesionesPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.interactor.ConsultarSesionesPorGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.ConsultarSesionesPorGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.impl.ConsultarSesionesPorGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.interactor.CrearSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.CrearSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.impl.CrearSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.GenerarSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.interactor.GenerarSesionesGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.GenerarSesionesGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.impl.GenerarSesionesGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.ConsultarSesionesPorGrupoV2InputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.interactor.ConsultarSesionesPorGrupoV2Interactor;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.ConsultarSesionesPorGrupoV2UseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.impl.ConsultarSesionesPorGrupoV2UseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.ConsultarSesionV2InputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.interactor.ConsultarSesionV2Interactor;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.ConsultarSesionV2UseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.impl.ConsultarSesionV2UseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SesionWiringConfiguration {

    @Bean
    public CrearSesionUseCase crearSesionUseCase(
            final SesionRepositoryPort sesionRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new CrearSesionUseCaseImpl(sesionRepositoryPort, institutionalScopePort);
    }

    @Bean
    public CrearSesionInputPort crearSesionInputPort(final CrearSesionUseCase crearSesionUseCase) {
        return new CrearSesionInteractor(crearSesionUseCase);
    }

    @Bean
    public ConsultarSesionUseCase consultarSesionUseCase(final SesionRepositoryPort sesionRepositoryPort) {
        return new ConsultarSesionUseCaseImpl(sesionRepositoryPort);
    }

    @Bean
    public ConsultarSesionInputPort consultarSesionInputPort(final ConsultarSesionUseCase consultarSesionUseCase) {
        return new ConsultarSesionInteractor(consultarSesionUseCase);
    }

    @Bean
    public ConsultarSesionesPorGrupoUseCase consultarSesionesPorGrupoUseCase(
            final SesionRepositoryPort sesionRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new ConsultarSesionesPorGrupoUseCaseImpl(sesionRepositoryPort, institutionalScopePort);
    }

    @Bean
    public ConsultarSesionesPorGrupoInputPort consultarSesionesPorGrupoInputPort(
            final ConsultarSesionesPorGrupoUseCase consultarSesionesPorGrupoUseCase
    ) {
        return new ConsultarSesionesPorGrupoInteractor(consultarSesionesPorGrupoUseCase);
    }

    @Bean
    public CerrarSesionUseCase cerrarSesionUseCase(
            final SesionRepositoryPort sesionRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new CerrarSesionUseCaseImpl(sesionRepositoryPort, institutionalScopePort);
    }

    @Bean
    public CerrarSesionInputPort cerrarSesionInputPort(final CerrarSesionUseCase cerrarSesionUseCase) {
        return new CerrarSesionInteractor(cerrarSesionUseCase);
    }

    @Bean
    public ActualizarSesionUseCase actualizarSesionUseCase(
            final SesionRepositoryPort sesionRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new ActualizarSesionUseCaseImpl(sesionRepositoryPort, institutionalScopePort);
    }

    @Bean
    public ActualizarSesionInputPort actualizarSesionInputPort(final ActualizarSesionUseCase actualizarSesionUseCase) {
        return new ActualizarSesionInteractor(actualizarSesionUseCase);
    }

    @Bean
    public ConsultarSesionV2UseCase consultarSesionV2UseCase(
            final SesionProcedenciaQueryPort sesionProcedenciaQueryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new ConsultarSesionV2UseCaseImpl(sesionProcedenciaQueryPort, institutionalScopePort);
    }

    @Bean
    public ConsultarSesionV2InputPort consultarSesionV2InputPort(final ConsultarSesionV2UseCase consultarSesionV2UseCase) {
        return new ConsultarSesionV2Interactor(consultarSesionV2UseCase);
    }

    @Bean
    public ConsultarSesionesPorGrupoV2UseCase consultarSesionesPorGrupoV2UseCase(
            final SesionProcedenciaQueryPort sesionProcedenciaQueryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new ConsultarSesionesPorGrupoV2UseCaseImpl(sesionProcedenciaQueryPort, institutionalScopePort);
    }

    @Bean
    public ConsultarSesionesPorGrupoV2InputPort consultarSesionesPorGrupoV2InputPort(
            final ConsultarSesionesPorGrupoV2UseCase consultarSesionesPorGrupoV2UseCase
    ) {
        return new ConsultarSesionesPorGrupoV2Interactor(consultarSesionesPorGrupoV2UseCase);
    }

    @Bean
    public GenerarSesionesGrupoUseCase generarSesionesGrupoUseCase(final SesionRepositoryPort sesionRepositoryPort) {
        return new GenerarSesionesGrupoUseCaseImpl(sesionRepositoryPort);
    }

    @Bean
    public GenerarSesionesGrupoInputPort generarSesionesGrupoInputPort(
            final GenerarSesionesGrupoUseCase generarSesionesGrupoUseCase
    ) {
        return new GenerarSesionesGrupoInteractor(generarSesionesGrupoUseCase);
    }
}
