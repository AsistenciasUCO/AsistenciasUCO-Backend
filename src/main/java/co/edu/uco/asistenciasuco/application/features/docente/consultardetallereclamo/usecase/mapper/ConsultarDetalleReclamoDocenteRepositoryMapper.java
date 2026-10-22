package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.domain.DetalleReclamoDocenteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DetalleSolicitudRevisionDocenteProjection;

public final class ConsultarDetalleReclamoDocenteRepositoryMapper {

    private ConsultarDetalleReclamoDocenteRepositoryMapper() {
    }

    public static DetalleReclamoDocenteDomain toDomain(final DetalleSolicitudRevisionDocenteProjection p) {
        return new DetalleReclamoDocenteDomain(
                p.idSolicitud(), p.nombreSolicitud(), p.fechaSolicitud(), p.codigoEstado(), p.nombreEstado(),
                p.justificacionSolicitud(), p.justificacionRespuesta(), p.idEstudiante(), p.nombreEstudiante(),
                p.idSesion(), p.numeroSesion(), p.fechaHoraInicioSesionUtc(), p.procedenciaTemporalSesion(),
                p.idGrupo(), p.codigoGrupo(), p.nombreGrupo(), p.idAsignatura(), p.codigoAsignatura(),
                p.nombreAsignatura(), p.asistio(), p.codigoRazonCausa(), p.nombreRazonCausa(),
                p.idArchivoSoporte(), p.nombreSoporte(), p.tipoContenidoSoporte(), p.tamanioSoporteBytes(),
                p.fechaRegistroSoporteUtc());
    }
}
