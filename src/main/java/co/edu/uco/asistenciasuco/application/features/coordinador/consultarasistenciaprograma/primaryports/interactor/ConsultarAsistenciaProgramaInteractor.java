package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.ConsultarAsistenciaProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.AsistenciaSesionDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.GrupoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.SesionGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.ConsultarAsistenciaProgramaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.Pagina;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public final class ConsultarAsistenciaProgramaInteractor implements ConsultarAsistenciaProgramaInputPort {

    private final ConsultarAsistenciaProgramaUseCase useCase;

    public ConsultarAsistenciaProgramaInteractor(final ConsultarAsistenciaProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ConsultarAsistenciaProgramaUseCase es obligatorio.");
    }

    @Override
    public PaginaDTO<GrupoProgramaDTO> consultarGrupos(final UUID usuario, final UUID idPeriodoAcademico,
                                                        final String busqueda, final Integer pagina,
                                                        final Integer tamano) {
        return aDTO(useCase.consultarGrupos(usuario, idPeriodoAcademico, busqueda, pagina, tamano), pagina, tamano,
                g -> new GrupoProgramaDTO(g.id(), g.codigo(), g.nombre(), g.idAsignatura(), g.nombreAsignatura(),
                        g.idDocente(), g.capacidadMaxima(), g.estudiantesActivos(), g.cuposDisponibles(),
                        g.habilitado(), g.inicioPeriodo(), g.finPeriodo()));
    }

    @Override
    public PaginaDTO<SesionGrupoDTO> consultarSesionesGrupo(final UUID usuario, final UUID idGrupo,
                                                             final LocalDate desde, final LocalDate hasta,
                                                             final Integer pagina, final Integer tamano) {
        return aDTO(useCase.consultarSesionesGrupo(usuario, idGrupo, desde, hasta, pagina, tamano), pagina, tamano,
                s -> new SesionGrupoDTO(s.id(), s.idGrupo(), s.codigoGrupo(), s.nombreGrupo(), s.nombre(), s.numero(),
                        s.codigo(), s.numeroSemana(), s.fechaHoraInicio(), s.fechaHoraFin(), s.fechaHoraInicioUtc(),
                        s.fechaHoraFinUtc(), s.estadoTemporal().name()));
    }

    @Override
    public PaginaDTO<AsistenciaSesionDTO> consultarAsistenciasSesion(final UUID usuario, final UUID idSesion,
                                                                      final Boolean asistio, final Integer pagina,
                                                                      final Integer tamano) {
        return aDTO(useCase.consultarAsistenciasSesion(usuario, idSesion, asistio, pagina, tamano), pagina, tamano,
                a -> new AsistenciaSesionDTO(a.idDetalleAsistencia(), a.idSesion(), a.nombreSesion(), a.idEstudiante(),
                        a.numeroIdentificacion(), a.nombreCompleto(), a.asistio(), a.codigoRazonCausa(),
                        a.nombreRazonCausa(), a.fechaHoraInicio(), a.fechaHoraFin()));
    }

    private static <P, D> PaginaDTO<D> aDTO(final Pagina<P> pagina, final Integer numero, final Integer tamano,
                                           final Function<P, D> mapper) {
        return new PaginaDTO<>(pagina.elementos().stream().map(mapper).toList(), pagina.total(),
                numero == null ? 1 : numero, tamano == null ? 10 : tamano);
    }
}
