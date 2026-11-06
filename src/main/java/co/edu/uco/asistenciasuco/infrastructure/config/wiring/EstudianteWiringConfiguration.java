package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
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
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.ConsultarReclamosEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.interactor.ConsultarReclamosEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.ConsultarReclamosEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.impl.ConsultarReclamosEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudIngresoGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.OfertaGruposEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.ConsultarOfertaGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.interactor.ConsultarOfertaGruposInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.ConsultarOfertaGruposUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.impl.ConsultarOfertaGruposUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.SolicitarIngresoGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.interactor.SolicitarIngresoGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.usecase.SolicitarIngresoGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.usecase.impl.SolicitarIngresoGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.RetirarSolicitudIngresoInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.interactor.RetirarSolicitudIngresoInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.usecase.RetirarSolicitudIngresoUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.usecase.impl.RetirarSolicitudIngresoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.ConsultarSolicitudesIngresoEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.interactor.ConsultarSolicitudesIngresoEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.ConsultarSolicitudesIngresoEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.impl.ConsultarSolicitudesIngresoEstudianteUseCaseImpl;
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
    ConsultarSesionesMateriaEstudianteUseCase consultarSesionesMateriaEstudianteUseCase(final InstitutionalScopePort scopePort, final SesionMateriaEstudianteQueryPort queryPort, final AsistenciaSesionEstudianteQueryPort asistenciaPort, final SesionProcedenciaQueryPort procedenciaPort) {
        return new ConsultarSesionesMateriaEstudianteUseCaseImpl(scopePort, queryPort, asistenciaPort, procedenciaPort);
    }

    @Bean
    ConsultarSesionesMateriaEstudianteInputPort consultarSesionesMateriaEstudianteInputPort(final ConsultarSesionesMateriaEstudianteUseCase useCase) {
        return new ConsultarSesionesMateriaEstudianteInteractor(useCase);
    }

    @Bean
    ConsultarReclamosEstudianteUseCase consultarReclamosEstudianteUseCase(final InstitutionalScopePort scopePort, final SolicitudRevisionEstudianteQueryPort queryPort) {
        return new ConsultarReclamosEstudianteUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarReclamosEstudianteInputPort consultarReclamosEstudianteInputPort(final ConsultarReclamosEstudianteUseCase useCase) {
        return new ConsultarReclamosEstudianteInteractor(useCase);
    }

    @Bean
    SolicitarIngresoGrupoUseCase solicitarIngresoGrupoUseCase(final InstitutionalScopePort scopePort, final SolicitudIngresoGrupoPort port, final NotificacionPort notificacionPort) {
        return new SolicitarIngresoGrupoUseCaseImpl(scopePort, port, notificacionPort);
    }

    @Bean
    SolicitarIngresoGrupoInputPort solicitarIngresoGrupoInputPort(final SolicitarIngresoGrupoUseCase useCase) {
        return new SolicitarIngresoGrupoInteractor(useCase);
    }

    @Bean
    RetirarSolicitudIngresoUseCase retirarSolicitudIngresoUseCase(final InstitutionalScopePort scopePort, final SolicitudIngresoGrupoPort port, final NotificacionPort notificacionPort) {
        return new RetirarSolicitudIngresoUseCaseImpl(scopePort, port, notificacionPort);
    }

    @Bean
    RetirarSolicitudIngresoInputPort retirarSolicitudIngresoInputPort(final RetirarSolicitudIngresoUseCase useCase) {
        return new RetirarSolicitudIngresoInteractor(useCase);
    }

    @Bean
    ConsultarSolicitudesIngresoEstudianteUseCase consultarSolicitudesIngresoEstudianteUseCase(final InstitutionalScopePort scopePort, final SolicitudIngresoGrupoPort port) {
        return new ConsultarSolicitudesIngresoEstudianteUseCaseImpl(scopePort, port);
    }

    @Bean
    ConsultarSolicitudesIngresoEstudianteInputPort consultarSolicitudesIngresoEstudianteInputPort(final ConsultarSolicitudesIngresoEstudianteUseCase useCase) {
        return new ConsultarSolicitudesIngresoEstudianteInteractor(useCase);
    }

    /** HU008: oferta de grupos (con cupos) de los programas del estudiante. */
    @Bean
    ConsultarOfertaGruposUseCase consultarOfertaGruposUseCase(final InstitutionalScopePort scopePort, final OfertaGruposEstudianteQueryPort port) {
        return new ConsultarOfertaGruposUseCaseImpl(scopePort, port);
    }

    @Bean
    ConsultarOfertaGruposInputPort consultarOfertaGruposInputPort(final ConsultarOfertaGruposUseCase useCase) {
        return new ConsultarOfertaGruposInteractor(useCase);
    }
}
