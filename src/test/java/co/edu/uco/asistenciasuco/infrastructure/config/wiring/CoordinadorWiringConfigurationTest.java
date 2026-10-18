package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.primaryports.interactor.ConsultarAsignaturasInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.impl.ConsultarAsignaturasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.primaryports.interactor.ConsultarAsignaturasPlanInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.impl.ConsultarAsignaturasPlanUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.interactor.ConsultarEstudiantesProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.impl.ConsultarEstudiantesProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.interactor.ConsultarPeriodosAcademicosInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.impl.ConsultarPeriodosAcademicosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.interactor.ConsultarPlanesEstudioInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.impl.ConsultarPlanesEstudioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.interactor.GestionarAsignaturaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.impl.GestionarAsignaturaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.interactor.GestionarPlanEstudioInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.impl.GestionarPlanEstudioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.EstudianteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class CoordinadorWiringConfigurationTest {

    private final CoordinadorWiringConfiguration config = new CoordinadorWiringConfiguration();
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);

    @Test
    void todos_los_beans_se_construyen_con_el_adapter_esperado() {
        final var consultarPlanesEstudioUseCase = config.consultarPlanesEstudioUseCase(scopePort, mock(PlanEstudioQueryPort.class));
        assertInstanceOf(ConsultarPlanesEstudioUseCaseImpl.class, consultarPlanesEstudioUseCase);
        assertInstanceOf(ConsultarPlanesEstudioInteractor.class,
                config.consultarPlanesEstudioInputPort(consultarPlanesEstudioUseCase));

        final var consultarAsignaturasUseCase = config.consultarAsignaturasUseCase(scopePort, mock(AsignaturaQueryPort.class));
        assertInstanceOf(ConsultarAsignaturasUseCaseImpl.class, consultarAsignaturasUseCase);
        assertInstanceOf(ConsultarAsignaturasInteractor.class,
                config.consultarAsignaturasInputPort(consultarAsignaturasUseCase));

        final var consultarAsignaturasPlanUseCase = config.consultarAsignaturasPlanUseCase(scopePort, mock(AsignaturaQueryPort.class));
        assertInstanceOf(ConsultarAsignaturasPlanUseCaseImpl.class, consultarAsignaturasPlanUseCase);
        assertInstanceOf(ConsultarAsignaturasPlanInteractor.class,
                config.consultarAsignaturasPlanInputPort(consultarAsignaturasPlanUseCase));

        final var consultarPeriodosAcademicosUseCase = config.consultarPeriodosAcademicosUseCase(mock(PeriodoAcademicoQueryPort.class));
        assertInstanceOf(ConsultarPeriodosAcademicosUseCaseImpl.class, consultarPeriodosAcademicosUseCase);
        assertInstanceOf(ConsultarPeriodosAcademicosInteractor.class,
                config.consultarPeriodosAcademicosInputPort(consultarPeriodosAcademicosUseCase));

        final var consultarEstudiantesProgramaUseCase =
                config.consultarEstudiantesProgramaUseCase(scopePort, mock(EstudianteProgramaQueryPort.class));
        assertInstanceOf(ConsultarEstudiantesProgramaUseCaseImpl.class, consultarEstudiantesProgramaUseCase);
        assertInstanceOf(ConsultarEstudiantesProgramaInteractor.class,
                config.consultarEstudiantesProgramaInputPort(consultarEstudiantesProgramaUseCase));

        final var gestionarPlanEstudioUseCase = config.gestionarPlanEstudioUseCase(mock(PlanEstudioCommandPort.class), scopePort);
        assertInstanceOf(GestionarPlanEstudioUseCaseImpl.class, gestionarPlanEstudioUseCase);
        assertInstanceOf(GestionarPlanEstudioInteractor.class,
                config.gestionarPlanEstudioInputPort(gestionarPlanEstudioUseCase));

        final var gestionarAsignaturaUseCase = config.gestionarAsignaturaUseCase(
                mock(AsignaturaCommandPort.class), mock(AsignaturaQueryPort.class),
                mock(PlanEstudioQueryPort.class), scopePort);
        assertInstanceOf(GestionarAsignaturaUseCaseImpl.class, gestionarAsignaturaUseCase);
        assertInstanceOf(GestionarAsignaturaInteractor.class,
                config.gestionarAsignaturaInputPort(gestionarAsignaturaUseCase));
    }
}
