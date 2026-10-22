package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.DetalleReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response.DetalleReclamoDocenteResponse;

/** Adapta el DTO de detalle de Application a HTTP, codificando los instantes UTC con el codec UTC v2. */
public final class DetalleReclamoDocenteHttpMapper {

    private DetalleReclamoDocenteHttpMapper() {
    }

    public static DetalleReclamoDocenteResponse toResponse(final DetalleReclamoDocenteDTO dto) {
        final DetalleReclamoDocenteDTO.CausalInasistenciaDTO causal = dto.causalInasistencia();
        return new DetalleReclamoDocenteResponse(
                dto.id(),
                dto.nombre(),
                dto.fechaSolicitud(),
                dto.codigoEstado(),
                dto.estadoSolicitud(),
                dto.nombreEstado(),
                dto.justificacionSolicitud(),
                dto.justificacionRespuesta(),
                dto.estudianteId(),
                dto.estudianteNombre(),
                dto.sesionId(),
                dto.sesionNumero(),
                ReclamoDocenteHttpMapper.utc(dto.fechaSesionUtc()),
                dto.procedenciaTemporalSesion(),
                dto.grupoId(),
                dto.grupoCodigo(),
                dto.grupoNombre(),
                dto.materiaId(),
                dto.materiaCodigo(),
                dto.materiaNombre(),
                causal == null ? null : new DetalleReclamoDocenteResponse.CausalInasistenciaResponse(
                        causal.codigo(), causal.nombre(), causal.asistio()),
                ReclamoDocenteHttpMapper.soporte(dto.soporteAdjunto())
        );
    }
}
