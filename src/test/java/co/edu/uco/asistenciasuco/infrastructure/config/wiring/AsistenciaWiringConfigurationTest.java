package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.interactor.ConsultarAsistenciasPorGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.impl.ConsultarAsistenciasPorGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.interactor.RegistrarAsistenciaAutonomaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.impl.RegistrarAsistenciaAutonomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.interactor.RegistrarAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.impl.RegistrarAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.interactor.RegistrarAsistenciasSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.impl.RegistrarAsistenciasSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.interactor.ResolverSolicitudRevisionAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.impl.ResolverSolicitudRevisionAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.interactor.SolicitarRevisionAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.impl.SolicitarRevisionAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class AsistenciaWiringConfigurationTest {

    private final AsistenciaWiringConfiguration config = new AsistenciaWiringConfiguration();
    private final AsistenciaRepositoryPort asistenciaRepositoryPort = mock(AsistenciaRepositoryPort.class);
    private final SesionProcedenciaQueryPort sesionProcedenciaQueryPort = mock(SesionProcedenciaQueryPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final RealtimePublisherPort realtimePublisherPort = mock(RealtimePublisherPort.class);

    @Test
    void todos_los_beans_se_construyen_con_el_adapter_esperado() {
        final var registrarAsistenciaUseCase = config.registrarAsistenciaUseCase(asistenciaRepositoryPort, realtimePublisherPort);
        assertInstanceOf(RegistrarAsistenciaUseCaseImpl.class, registrarAsistenciaUseCase);
        assertInstanceOf(RegistrarAsistenciaInteractor.class, config.registrarAsistenciaInputPort(registrarAsistenciaUseCase));

        final var consultarAsistenciasPorGrupoUseCase = config.consultarAsistenciasPorGrupoUseCase(asistenciaRepositoryPort, scopePort);
        assertInstanceOf(ConsultarAsistenciasPorGrupoUseCaseImpl.class, consultarAsistenciasPorGrupoUseCase);
        assertInstanceOf(ConsultarAsistenciasPorGrupoInteractor.class,
                config.consultarAsistenciasPorGrupoInputPort(consultarAsistenciasPorGrupoUseCase));

        final var registrarAsistenciasSesionUseCase = config.registrarAsistenciasSesionUseCase(
                asistenciaRepositoryPort, sesionProcedenciaQueryPort, scopePort, realtimePublisherPort);
        assertInstanceOf(RegistrarAsistenciasSesionUseCaseImpl.class, registrarAsistenciasSesionUseCase);
        assertInstanceOf(RegistrarAsistenciasSesionInteractor.class,
                config.registrarAsistenciasSesionInputPort(registrarAsistenciasSesionUseCase));

        final var registrarAsistenciaAutonomaUseCase = config.registrarAsistenciaAutonomaUseCase(asistenciaRepositoryPort, scopePort);
        assertInstanceOf(RegistrarAsistenciaAutonomaUseCaseImpl.class, registrarAsistenciaAutonomaUseCase);
        assertInstanceOf(RegistrarAsistenciaAutonomaInteractor.class,
                config.registrarAsistenciaAutonomaInputPort(registrarAsistenciaAutonomaUseCase));

        final var solicitarRevisionAsistenciaUseCase = config.solicitarRevisionAsistenciaUseCase(asistenciaRepositoryPort, scopePort, providerVacio(), providerVacio());
        assertInstanceOf(SolicitarRevisionAsistenciaUseCaseImpl.class, solicitarRevisionAsistenciaUseCase);
        assertInstanceOf(SolicitarRevisionAsistenciaInteractor.class,
                config.solicitarRevisionAsistenciaInputPort(solicitarRevisionAsistenciaUseCase));

        final var resolverSolicitudUseCase = config.resolverSolicitudRevisionAsistenciaUseCase(asistenciaRepositoryPort, scopePort, providerVacio(), providerVacio());
        assertInstanceOf(ResolverSolicitudRevisionAsistenciaUseCaseImpl.class, resolverSolicitudUseCase);
        assertInstanceOf(ResolverSolicitudRevisionAsistenciaInteractor.class,
                config.resolverSolicitudRevisionAsistenciaInputPort(resolverSolicitudUseCase));
    }

    @SuppressWarnings("unchecked")
    private static <T> org.springframework.beans.factory.ObjectProvider<T> providerVacio() {
        return mock(org.springframework.beans.factory.ObjectProvider.class);
    }
}
