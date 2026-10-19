package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionDocenteProjection;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.domain.ReclamoDocenteDomain;

public final class ConsultarReclamosDocenteRepositoryMapper {

    private ConsultarReclamosDocenteRepositoryMapper() {
    }

    public static ReclamoDocenteDomain toDomain(final SolicitudRevisionDocenteProjection p) {
        return new ReclamoDocenteDomain(
                p.idSolicitud(), p.nombreSolicitud(), p.fechaSolicitud(), p.codigoEstado(),
                p.justificacionSolicitud(), p.justificacionRespuesta(), p.idEstudiante(), p.nombreEstudiante(),
                p.idSesion(), p.numeroSesion(), p.fechaHoraInicioSesionUtc(), p.procedenciaTemporalSesion(),
                p.idGrupo(), p.codigoGrupo(), p.nombreGrupo(), p.idAsignatura(), p.codigoAsignatura(),
                p.nombreAsignatura(), p.idArchivoSoporte(), p.nombreSoporte(), p.tipoContenidoSoporte(),
                p.tamanioSoporteBytes(), p.fechaRegistroSoporteUtc());
    }
}
