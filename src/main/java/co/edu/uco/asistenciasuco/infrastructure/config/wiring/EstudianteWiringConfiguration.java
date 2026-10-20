package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.ConsultarEstudiantePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.interactor.ConsultarEstudiantePorIdInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.ConsultarEstudiantePorIdUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.impl.ConsultarEstudiantePorIdUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.ConsultarEstudiantesInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.interactor.ConsultarEstudiantesInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.ConsultarEstudiantesUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.impl.ConsultarEstudiantesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.ConsultarHorariosEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.interactor.ConsultarHorariosEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.ConsultarHorariosEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.impl.ConsultarHorariosEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.ConsultarMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.interactor.ConsultarMateriasEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.ConsultarMateriasEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.impl.ConsultarMateriasEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.ConsultarSesionesMateriaEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.interactor.ConsultarSesionesMateriaEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.ConsultarSesionesMateriaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.impl.ConsultarSesionesMateriaEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaSesionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root (wiring) para el feature Estudiante: ensambla UseCase + Interactor a partir
 * de los Application Ports, sin conocer ninguna tecnologia concreta.
 *
 * <p>Consolida lo que antes eran dos clases distintas ({@code EstudianteBeansConfig} y
 * {@code EstudianteFeatureConfiguration}) sin cambiar ningun bean: mismos nombres, mismos tipos,
 * mismas dependencias.</p>
 */
@Configuration(proxyBeanMethods = false)
public class EstudianteWiringConfiguration {

    @Bean
    public ConsultarEstudiantesUseCase consultarEstudiantesUseCase(
            final EstudianteRepositoryPort estudianteRepositoryPort
    ) {
        return new ConsultarEstudiantesUseCaseImpl(estudianteRepositoryPort);
    }

    @Bean
    public ConsultarEstudiantesInputPort consultarEstudiantesInputPort(
            final ConsultarEstudiantesUseCase consultarEstudiantesUseCase
    ) {
        return new ConsultarEstudiantesInteractor(consultarEstudiantesUseCase);
    }

    @Bean
    public ConsultarEstudiantePorIdUseCase consultarEstudiantePorIdUseCase(
            final EstudianteRepositoryPort estudianteRepositoryPort
    ) {
        return new ConsultarEstudiantePorIdUseCaseImpl(estudianteRepositoryPort);
    }

    @Bean
    public ConsultarEstudiantePorIdInputPort consultarEstudiantePorIdInputPort(
            final ConsultarEstudiantePorIdUseCase consultarEstudiantePorIdUseCase
    ) {
        return new ConsultarEstudiantePorIdInteractor(consultarEstudiantePorIdUseCase);
    }

    @Bean
    ConsultarMateriasEstudianteUseCase consultarMateriasEstudianteUseCase(final InstitutionalScopePort scopePort, final MateriaEstudianteQueryPort queryPort) {
        return new ConsultarMateriasEstudianteUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarMateriasEstudianteInputPort consultarMateriasEstudianteInputPort(final ConsultarMateriasEstudianteUseCase useCase) {
        return new ConsultarMateriasEstudianteInteractor(useCase);
    }

    @Bean
    ConsultarHorariosEstudianteUseCase consultarHorariosEstudianteUseCase(final InstitutionalScopePort scopePort, final HorarioEstudianteQueryPort queryPort) {
        return new ConsultarHorariosEstudianteUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarHorariosEstudianteInputPort consultarHorariosEstudianteInputPort(final ConsultarHorariosEstudianteUseCase useCase) {
        return new ConsultarHorariosEstudianteInteractor(useCase);
    }

    @Bean
    ConsultarSesionesMateriaEstudianteUseCase consultarSesionesMateriaEstudianteUseCase(final InstitutionalScopePort scopePort, final SesionMateriaEstudianteQueryPort queryPort, final AsistenciaSesionEstudianteQueryPort asistenciaPort) {
        return new ConsultarSesionesMateriaEstudianteUseCaseImpl(scopePort, queryPort, asistenciaPort);
    }

    @Bean
    ConsultarSesionesMateriaEstudianteInputPort consultarSesionesMateriaEstudianteInputPort(final ConsultarSesionesMateriaEstudianteUseCase useCase) {
        return new ConsultarSesionesMateriaEstudianteInteractor(useCase);
    }
}
