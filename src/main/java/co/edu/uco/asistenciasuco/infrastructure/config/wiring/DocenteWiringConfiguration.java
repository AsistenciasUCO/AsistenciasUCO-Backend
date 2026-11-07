package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.ConsultarAsignacionesAcademicasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.interactor.ConsultarAsignacionesAcademicasDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.ConsultarAsignacionesAcademicasDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.impl.ConsultarAsignacionesAcademicasDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.interactor.ConsultarAsignaturasDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.ConsultarAsignaturasDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.impl.ConsultarAsignaturasDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.ConsultarOfertaCreacionGruposDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.interactor.ConsultarOfertaCreacionGruposDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.ConsultarOfertaCreacionGruposDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.impl.ConsultarOfertaCreacionGruposDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.OfertaCreacionGruposDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.ConsultarDocentesInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.interactor.ConsultarDocentesInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.ConsultarDocentesUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.impl.ConsultarDocentesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.ConsultarDocentePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.interactor.ConsultarDocentePorIdInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.ConsultarDocentePorIdUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.impl.ConsultarDocentePorIdUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.ConsultarDetalleReclamoDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.interactor.ConsultarDetalleReclamoDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.ConsultarDetalleReclamoDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.impl.ConsultarDetalleReclamoDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.ConsultarReclamosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.interactor.ConsultarReclamosDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.ConsultarReclamosDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.impl.ConsultarReclamosDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.interactor.ConsultarHorariosDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.ConsultarHorariosDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.impl.ConsultarHorariosDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.RetiroEstudianteGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.RetirarEstudianteGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.interactor.RetirarEstudianteGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.RetirarEstudianteGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.impl.RetirarEstudianteGrupoUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root (wiring) para el feature Docente: ensambla UseCase + Interactor a partir de
 * los Application Ports, sin conocer ninguna tecnologia concreta.
 *
 * <p>Consolida lo que antes eran dos clases distintas ({@code DocenteBeansConfig} y
 * {@code DocenteFeatureConfiguration}) sin cambiar ningun bean: mismos nombres, mismos tipos,
 * mismas dependencias.</p>
 */
@Configuration(proxyBeanMethods = false)
public class DocenteWiringConfiguration {

    @Bean
    public ConsultarDocentesUseCase consultarDocentesUseCase(final DocenteRepositoryPort docenteRepositoryPort) {
        return new ConsultarDocentesUseCaseImpl(docenteRepositoryPort);
    }

    @Bean
    public ConsultarDocentesInputPort consultarDocentesInputPort(
            final ConsultarDocentesUseCase consultarDocentesUseCase
    ) {
        return new ConsultarDocentesInteractor(consultarDocentesUseCase);
    }

    @Bean
    public ConsultarDocentePorIdUseCase consultarDocentePorIdUseCase(
            final DocenteRepositoryPort docenteRepositoryPort
    ) {
        return new ConsultarDocentePorIdUseCaseImpl(docenteRepositoryPort);
    }

    @Bean
    public ConsultarDocentePorIdInputPort consultarDocentePorIdInputPort(
            final ConsultarDocentePorIdUseCase consultarDocentePorIdUseCase
    ) {
        return new ConsultarDocentePorIdInteractor(consultarDocentePorIdUseCase);
    }

    @Bean
    public ConsultarAsignacionesAcademicasDocenteUseCase consultarAsignacionesAcademicasDocenteUseCase(
            final DocenteRepositoryPort docenteRepositoryPort
    ) {
        return new ConsultarAsignacionesAcademicasDocenteUseCaseImpl(docenteRepositoryPort);
    }

    @Bean
    public ConsultarAsignacionesAcademicasDocenteInputPort consultarAsignacionesAcademicasDocenteInputPort(
            final ConsultarAsignacionesAcademicasDocenteUseCase consultarAsignacionesAcademicasDocenteUseCase
    ) {
        return new ConsultarAsignacionesAcademicasDocenteInteractor(consultarAsignacionesAcademicasDocenteUseCase);
    }

    @Bean
    ConsultarReclamosDocenteUseCase consultarReclamosDocenteUseCase(
            final InstitutionalScopePort scopePort,
            final SolicitudRevisionDocenteQueryPort queryPort
    ) {
        return new ConsultarReclamosDocenteUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarReclamosDocenteInputPort consultarReclamosDocenteInputPort(final ConsultarReclamosDocenteUseCase useCase) {
        return new ConsultarReclamosDocenteInteractor(useCase);
    }

    @Bean
    ConsultarDetalleReclamoDocenteUseCase consultarDetalleReclamoDocenteUseCase(
            final InstitutionalScopePort scopePort,
            final SolicitudRevisionDocenteQueryPort queryPort
    ) {
        return new ConsultarDetalleReclamoDocenteUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarDetalleReclamoDocenteInputPort consultarDetalleReclamoDocenteInputPort(
            final ConsultarDetalleReclamoDocenteUseCase useCase
    ) {
        return new ConsultarDetalleReclamoDocenteInteractor(useCase);
    }

    @Bean
    ConsultarHorariosDocenteUseCase consultarHorariosDocenteUseCase(final InstitutionalScopePort scopePort, final HorarioDocenteQueryPort queryPort) {
        return new ConsultarHorariosDocenteUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarHorariosDocenteInputPort consultarHorariosDocenteInputPort(final ConsultarHorariosDocenteUseCase useCase) {
        return new ConsultarHorariosDocenteInteractor(useCase);
    }

    @Bean
    ConsultarAsignaturasDocenteUseCase consultarAsignaturasDocenteUseCase(final InstitutionalScopePort scopePort, final AsignaturaDocenteQueryPort queryPort) {
        return new ConsultarAsignaturasDocenteUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarAsignaturasDocenteInputPort consultarAsignaturasDocenteInputPort(final ConsultarAsignaturasDocenteUseCase useCase) {
        return new ConsultarAsignaturasDocenteInteractor(useCase);
    }

    @Bean
    ConsultarOfertaCreacionGruposDocenteUseCase consultarOfertaCreacionGruposDocenteUseCase(final InstitutionalScopePort scopePort, final OfertaCreacionGruposDocenteQueryPort queryPort) {
        return new ConsultarOfertaCreacionGruposDocenteUseCaseImpl(scopePort, queryPort);
    }

    @Bean
    ConsultarOfertaCreacionGruposDocenteInputPort consultarOfertaCreacionGruposDocenteInputPort(final ConsultarOfertaCreacionGruposDocenteUseCase useCase) {
        return new ConsultarOfertaCreacionGruposDocenteInteractor(useCase);
    }

    @Bean
    RetirarEstudianteGrupoUseCase retirarEstudianteGrupoUseCase(final InstitutionalScopePort scopePort, final RetiroEstudianteGrupoPort retiroPort, final NotificacionPort notificacionPort) {
        return new RetirarEstudianteGrupoUseCaseImpl(scopePort, retiroPort, notificacionPort);
    }

    @Bean
    RetirarEstudianteGrupoInputPort retirarEstudianteGrupoInputPort(final RetirarEstudianteGrupoUseCase useCase) {
        return new RetirarEstudianteGrupoInteractor(useCase);
    }
}
