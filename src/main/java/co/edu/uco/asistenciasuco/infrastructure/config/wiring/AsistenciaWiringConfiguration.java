package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.ConsultarAsistenciasPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.interactor.ConsultarAsistenciasPorGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.ConsultarAsistenciasPorGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.impl.ConsultarAsistenciasPorGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.RegistrarAsistenciaAutonomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.interactor.RegistrarAsistenciaAutonomaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.RegistrarAsistenciaAutonomaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.impl.RegistrarAsistenciaAutonomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.RegistrarAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.interactor.RegistrarAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.RegistrarAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.impl.RegistrarAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.RegistrarAsistenciasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.interactor.RegistrarAsistenciasSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.RegistrarAsistenciasSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.impl.RegistrarAsistenciasSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.ResolverSolicitudRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.interactor.ResolverSolicitudRevisionAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.ResolverSolicitudRevisionAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.impl.ResolverSolicitudRevisionAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.SolicitarRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.interactor.SolicitarRevisionAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.SolicitarRevisionAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.impl.SolicitarRevisionAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class AsistenciaWiringConfiguration {

    @Bean
    public RegistrarAsistenciaUseCase registrarAsistenciaUseCase(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new RegistrarAsistenciaUseCaseImpl(asistenciaRepositoryPort, realtimePublisherPort);
    }

    @Bean
    public RegistrarAsistenciaInputPort registrarAsistenciaInputPort(
            final RegistrarAsistenciaUseCase registrarAsistenciaUseCase
    ) {
        return new RegistrarAsistenciaInteractor(registrarAsistenciaUseCase);
    }

    @Bean
    public ConsultarAsistenciasPorGrupoUseCase consultarAsistenciasPorGrupoUseCase(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new ConsultarAsistenciasPorGrupoUseCaseImpl(asistenciaRepositoryPort, institutionalScopePort);
    }

    @Bean
    public ConsultarAsistenciasPorGrupoInputPort consultarAsistenciasPorGrupoInputPort(
            final ConsultarAsistenciasPorGrupoUseCase consultarAsistenciasPorGrupoUseCase
    ) {
        return new ConsultarAsistenciasPorGrupoInteractor(consultarAsistenciasPorGrupoUseCase);
    }

    @Bean
    public RegistrarAsistenciasSesionUseCase registrarAsistenciasSesionUseCase(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final SesionProcedenciaQueryPort sesionProcedenciaQueryPort,
            final InstitutionalScopePort institutionalScopePort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        // SES-01: el reloj autoritativo de la ventana de registro es el del servidor en UTC.
        return new RegistrarAsistenciasSesionUseCaseImpl(
                asistenciaRepositoryPort,
                sesionProcedenciaQueryPort,
                institutionalScopePort,
                realtimePublisherPort,
                Clock.systemUTC()
        );
    }

    @Bean
    public RegistrarAsistenciasSesionInputPort registrarAsistenciasSesionInputPort(
            final RegistrarAsistenciasSesionUseCase registrarAsistenciasSesionUseCase
    ) {
        return new RegistrarAsistenciasSesionInteractor(registrarAsistenciasSesionUseCase);
    }

    @Bean
    public RegistrarAsistenciaAutonomaUseCase registrarAsistenciaAutonomaUseCase(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new RegistrarAsistenciaAutonomaUseCaseImpl(asistenciaRepositoryPort, institutionalScopePort);
    }

    @Bean
    public RegistrarAsistenciaAutonomaInputPort registrarAsistenciaAutonomaInputPort(
            final RegistrarAsistenciaAutonomaUseCase registrarAsistenciaAutonomaUseCase
    ) {
        return new RegistrarAsistenciaAutonomaInteractor(registrarAsistenciaAutonomaUseCase);
    }

    @Bean
    public SolicitarRevisionAsistenciaUseCase solicitarRevisionAsistenciaUseCase(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new SolicitarRevisionAsistenciaUseCaseImpl(asistenciaRepositoryPort, institutionalScopePort);
    }

    @Bean
    public SolicitarRevisionAsistenciaInputPort solicitarRevisionAsistenciaInputPort(
            final SolicitarRevisionAsistenciaUseCase solicitarRevisionAsistenciaUseCase
    ) {
        return new SolicitarRevisionAsistenciaInteractor(solicitarRevisionAsistenciaUseCase);
    }

    @Bean
    public ResolverSolicitudRevisionAsistenciaUseCase resolverSolicitudRevisionAsistenciaUseCase(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new ResolverSolicitudRevisionAsistenciaUseCaseImpl(asistenciaRepositoryPort, institutionalScopePort);
    }

    @Bean
    public ResolverSolicitudRevisionAsistenciaInputPort resolverSolicitudRevisionAsistenciaInputPort(
            final ResolverSolicitudRevisionAsistenciaUseCase resolverSolicitudRevisionAsistenciaUseCase
    ) {
        return new ResolverSolicitudRevisionAsistenciaInteractor(resolverSolicitudRevisionAsistenciaUseCase);
    }
}
