package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ConsultarReclamosEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ReclamoEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ReclamosEstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.domain.ConsultarReclamosEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.domain.ReclamoEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.domain.ReclamosEstudiantePaginaDomain;

public final class ConsultarReclamosEstudianteMapper {

    private static final String URL_PREFIX = "/api/v1/archivos/";

    private ConsultarReclamosEstudianteMapper() {
    }

    public static ConsultarReclamosEstudianteDomain toDomain(final ConsultarReclamosEstudianteDTO dto) {
        return ConsultarReclamosEstudianteDomain.of(
                dto == null ? null : dto.actorUsuarioId(),
                dto == null ? null : dto.estado(),
                dto == null ? null : dto.pagina(),
                dto == null ? null : dto.tamano());
    }

    public static ReclamosEstudiantePaginaDTO toDTO(final ReclamosEstudiantePaginaDomain domain) {
        return new ReclamosEstudiantePaginaDTO(
                domain.reclamos().stream().map(ConsultarReclamosEstudianteMapper::toDTO).toList(),
                domain.totalRegistros());
    }

    private static ReclamoEstudianteDTO toDTO(final ReclamoEstudianteDomain p) {
        return new ReclamoEstudianteDTO(
                p.idSolicitud(),
                p.nombreSolicitud(),
                p.fechaSolicitud(),
                estadoLegible(p.codigoEstado()),
                p.justificacionSolicitud(),
                p.justificacionRespuesta(),
                p.idSesion(),
                p.numeroSesion(),
                p.fechaHoraInicioSesionUtc(),
                p.procedenciaTemporalSesion(),
                p.idGrupo(),
                p.codigoGrupo(),
                p.nombreGrupo(),
                p.idAsignatura(),
                p.codigoAsignatura(),
                p.nombreAsignatura(),
                soporte(p));
    }

    /** Soporte adjunto con URL relativa autenticada; nulo sin archivo vinculado. */
    private static ReclamoEstudianteDTO.SoporteAdjuntoDTO soporte(final ReclamoEstudianteDomain p) {
        if (p.idArchivoSoporte() == null) {
            return null;
        }
        final long bytes = p.tamanioSoporteBytes() == null ? 0L : p.tamanioSoporteBytes();
        return new ReclamoEstudianteDTO.SoporteAdjuntoDTO(p.idArchivoSoporte(), p.nombreSoporte(),
                p.tipoContenidoSoporte(), bytes, p.fechaRegistroSoporteUtc(), URL_PREFIX + p.idArchivoSoporte());
    }

    /** Traduccion P/A/R a estado legible (mismo contrato que la bandeja docente). */
    private static String estadoLegible(final String codigo) {
        if (codigo == null) {
            return null;
        }
        return switch (codigo) {
            case "P" -> "PENDIENTE";
            case "A" -> "APROBADA";
            case "R" -> "RECHAZADA";
            // CULM-001 A.2-7: reclamo retirado por el estudiante (estado X de dbo.Estado).
            case "X" -> "RETIRADA";
            default -> codigo;
        };
    }
}
