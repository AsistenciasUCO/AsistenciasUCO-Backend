package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.primaryports.ConsultarAsignaturasInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.primaryports.interactor.ConsultarAsignaturasInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.ConsultarAsignaturasUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.impl.ConsultarAsignaturasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.primaryports.ConsultarAsignaturasPlanInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.primaryports.interactor.ConsultarAsignaturasPlanInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.ConsultarAsignaturasPlanUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.impl.ConsultarAsignaturasPlanUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.ConsultarEstudiantesProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.interactor.ConsultarEstudiantesProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.ConsultarEstudiantesProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.impl.ConsultarEstudiantesProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.ConsultarPeriodosAcademicosInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.interactor.ConsultarPeriodosAcademicosInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.ConsultarPeriodosAcademicosUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.impl.ConsultarPeriodosAcademicosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.ConsultarPlanesEstudioInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.interactor.ConsultarPlanesEstudioInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.ConsultarPlanesEstudioUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.impl.ConsultarPlanesEstudioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.GestionarAsignaturaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.interactor.GestionarAsignaturaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.GestionarAsignaturaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.impl.GestionarAsignaturaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.GestionarPlanEstudioInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.interactor.GestionarPlanEstudioInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.GestionarPlanEstudioUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.impl.GestionarPlanEstudioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.EstudianteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CoordinadorWiringConfiguration {

    @Bean ConsultarPlanesEstudioUseCase consultarPlanesEstudioUseCase(final InstitutionalScopePort scopePort, final PlanEstudioQueryPort queryPort) { return new ConsultarPlanesEstudioUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarPlanesEstudioInputPort consultarPlanesEstudioInputPort(final ConsultarPlanesEstudioUseCase useCase) { return new ConsultarPlanesEstudioInteractor(useCase); }
    @Bean ConsultarAsignaturasUseCase consultarAsignaturasUseCase(final InstitutionalScopePort scopePort, final AsignaturaQueryPort queryPort) { return new ConsultarAsignaturasUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarAsignaturasInputPort consultarAsignaturasInputPort(final ConsultarAsignaturasUseCase useCase) { return new ConsultarAsignaturasInteractor(useCase); }
    @Bean ConsultarAsignaturasPlanUseCase consultarAsignaturasPlanUseCase(final InstitutionalScopePort scopePort, final AsignaturaQueryPort queryPort) { return new ConsultarAsignaturasPlanUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarAsignaturasPlanInputPort consultarAsignaturasPlanInputPort(final ConsultarAsignaturasPlanUseCase useCase) { return new ConsultarAsignaturasPlanInteractor(useCase); }
    @Bean ConsultarPeriodosAcademicosUseCase consultarPeriodosAcademicosUseCase(final PeriodoAcademicoQueryPort queryPort) { return new ConsultarPeriodosAcademicosUseCaseImpl(queryPort); }
    @Bean ConsultarPeriodosAcademicosInputPort consultarPeriodosAcademicosInputPort(final ConsultarPeriodosAcademicosUseCase useCase) { return new ConsultarPeriodosAcademicosInteractor(useCase); }
    @Bean ConsultarEstudiantesProgramaUseCase consultarEstudiantesProgramaUseCase(final InstitutionalScopePort scopePort, final EstudianteProgramaQueryPort queryPort) { return new ConsultarEstudiantesProgramaUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarEstudiantesProgramaInputPort consultarEstudiantesProgramaInputPort(final ConsultarEstudiantesProgramaUseCase useCase) { return new ConsultarEstudiantesProgramaInteractor(useCase); }
    @Bean GestionarPlanEstudioUseCase gestionarPlanEstudioUseCase(final PlanEstudioCommandPort commandPort, final InstitutionalScopePort scopePort) { return new GestionarPlanEstudioUseCaseImpl(commandPort, scopePort); }
    @Bean GestionarPlanEstudioInputPort gestionarPlanEstudioInputPort(final GestionarPlanEstudioUseCase useCase) { return new GestionarPlanEstudioInteractor(useCase); }
    @Bean GestionarAsignaturaUseCase gestionarAsignaturaUseCase(final AsignaturaCommandPort commandPort, final AsignaturaQueryPort queryPort, final PlanEstudioQueryPort planQueryPort, final InstitutionalScopePort scopePort) { return new GestionarAsignaturaUseCaseImpl(commandPort, queryPort, planQueryPort, scopePort); }
    @Bean GestionarAsignaturaInputPort gestionarAsignaturaInputPort(final GestionarAsignaturaUseCase useCase) { return new GestionarAsignaturaInteractor(useCase); }
}
