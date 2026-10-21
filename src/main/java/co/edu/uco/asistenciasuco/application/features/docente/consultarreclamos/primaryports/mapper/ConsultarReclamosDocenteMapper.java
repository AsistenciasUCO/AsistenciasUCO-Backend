package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ConsultarReclamosDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamosDocentePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.domain.ConsultarReclamosDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.domain.ReclamoDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.domain.ReclamosDocentePaginaDomain;

public final class ConsultarReclamosDocenteMapper {

    private static final String URL_PREFIX = "/api/v1/archivos/";

    private ConsultarReclamosDocenteMapper() {
    }

    public static ConsultarReclamosDocenteDomain toDomain(final ConsultarReclamosDocenteDTO dto) {
        return ConsultarReclamosDocenteDomain.of(
                dto == null ? null : dto.actorUsuarioId(),
                dto == null ? null : dto.estado(),
                dto == null ? null : dto.pagina(),
                dto == null ? null : dto.tamano());
    }

    public static ReclamosDocentePaginaDTO toDTO(final ReclamosDocentePaginaDomain domain) {
        return new ReclamosDocentePaginaDTO(
                domain.reclamos().stream().map(ConsultarReclamosDocenteMapper::toDTO).toList(),
                domain.totalRegistros());
    }

    private static ReclamoDocenteDTO toDTO(final ReclamoDocenteDomain p) {
        final ReclamoDocenteDTO.SoporteAdjuntoDTO soporte = p.idArchivoSoporte() == null ? null
                : new ReclamoDocenteDTO.SoporteAdjuntoDTO(
                        p.idArchivoSoporte(),
                        p.nombreSoporte(),
                        p.tipoContenidoSoporte(),
                        p.tamanioSoporteBytes() == null ? 0L : p.tamanioSoporteBytes(),
                        p.fechaRegistroSoporteUtc(),
                        URL_PREFIX + p.idArchivoSoporte());
        return new ReclamoDocenteDTO(
                p.idSolicitud(),
                p.nombreSolicitud(),
                p.fechaSolicitud(),
                estadoLegible(p.codigoEstado()),
                p.justificacionSolicitud(),
                p.justificacionRespuesta(),
                p.idEstudiante(),
                p.nombreEstudiante(),
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
                soporte);
    }

    private static String estadoLegible(final String codigo) {
        if (codigo == null) {
            return null;
        }
        return switch (codigo) {
            case "P" -> "PENDIENTE";
            case "A" -> "APROBADA";
            case "R" -> "RECHAZADA";
            default -> codigo;
        };
    }
}
