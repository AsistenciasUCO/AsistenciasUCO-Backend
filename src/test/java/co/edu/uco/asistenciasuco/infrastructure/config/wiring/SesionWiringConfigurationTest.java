package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.interactor.ActualizarSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.impl.ActualizarSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.interactor.CerrarSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.impl.CerrarSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.interactor.ConsultarSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.impl.ConsultarSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.interactor.ConsultarSesionesPorGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.impl.ConsultarSesionesPorGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.interactor.ConsultarSesionV2Interactor;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.impl.ConsultarSesionV2UseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.interactor.ConsultarSesionesPorGrupoV2Interactor;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.impl.ConsultarSesionesPorGrupoV2UseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.interactor.CrearSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.impl.CrearSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.interactor.GenerarSesionesGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.impl.GenerarSesionesGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class SesionWiringConfigurationTest {

    private final SesionWiringConfiguration config = new SesionWiringConfiguration();
    private final SesionRepositoryPort sesionRepositoryPort = mock(SesionRepositoryPort.class);
    private final SesionProcedenciaQueryPort sesionProcedenciaQueryPort = mock(SesionProcedenciaQueryPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);

    @Test
    void todos_los_beans_se_construyen_con_el_adapter_esperado() {
        final var crearSesionUseCase = config.crearSesionUseCase(sesionRepositoryPort, scopePort);
        assertInstanceOf(CrearSesionUseCaseImpl.class, crearSesionUseCase);
        assertInstanceOf(CrearSesionInteractor.class, config.crearSesionInputPort(crearSesionUseCase));

        final var consultarSesionUseCase = config.consultarSesionUseCase(sesionRepositoryPort);
        assertInstanceOf(ConsultarSesionUseCaseImpl.class, consultarSesionUseCase);
        assertInstanceOf(ConsultarSesionInteractor.class, config.consultarSesionInputPort(consultarSesionUseCase));

        final var consultarSesionesPorGrupoUseCase = config.consultarSesionesPorGrupoUseCase(sesionRepositoryPort, scopePort);
        assertInstanceOf(ConsultarSesionesPorGrupoUseCaseImpl.class, consultarSesionesPorGrupoUseCase);
        assertInstanceOf(ConsultarSesionesPorGrupoInteractor.class,
                config.consultarSesionesPorGrupoInputPort(consultarSesionesPorGrupoUseCase));

        final var cerrarSesionUseCase = config.cerrarSesionUseCase(sesionRepositoryPort, scopePort);
        assertInstanceOf(CerrarSesionUseCaseImpl.class, cerrarSesionUseCase);
        assertInstanceOf(CerrarSesionInteractor.class, config.cerrarSesionInputPort(cerrarSesionUseCase));

        final var actualizarSesionUseCase = config.actualizarSesionUseCase(sesionRepositoryPort, scopePort);
        assertInstanceOf(ActualizarSesionUseCaseImpl.class, actualizarSesionUseCase);
        assertInstanceOf(ActualizarSesionInteractor.class, config.actualizarSesionInputPort(actualizarSesionUseCase));

        final var generarSesionesGrupoUseCase = config.generarSesionesGrupoUseCase(sesionRepositoryPort);
        assertInstanceOf(GenerarSesionesGrupoUseCaseImpl.class, generarSesionesGrupoUseCase);
        assertInstanceOf(GenerarSesionesGrupoInteractor.class,
                config.generarSesionesGrupoInputPort(generarSesionesGrupoUseCase));

        final var consultarSesionV2UseCase = config.consultarSesionV2UseCase(sesionProcedenciaQueryPort, scopePort);
        assertInstanceOf(ConsultarSesionV2UseCaseImpl.class, consultarSesionV2UseCase);
        assertInstanceOf(ConsultarSesionV2Interactor.class, config.consultarSesionV2InputPort(consultarSesionV2UseCase));

        final var consultarSesionesPorGrupoV2UseCase =
                config.consultarSesionesPorGrupoV2UseCase(sesionProcedenciaQueryPort, scopePort);
        assertInstanceOf(ConsultarSesionesPorGrupoV2UseCaseImpl.class, consultarSesionesPorGrupoV2UseCase);
        assertInstanceOf(ConsultarSesionesPorGrupoV2Interactor.class,
                config.consultarSesionesPorGrupoV2InputPort(consultarSesionesPorGrupoV2UseCase));
    }
}
