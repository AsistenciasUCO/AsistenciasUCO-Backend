package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.ConsultarDetalleReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.DetalleReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.domain.ConsultarDetalleReclamoDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.domain.DetalleReclamoDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.mapper.ConsultarReclamosDocenteMapper;

public final class ConsultarDetalleReclamoDocenteMapper {

    private ConsultarDetalleReclamoDocenteMapper() {
    }

    public static ConsultarDetalleReclamoDocenteDomain toDomain(final ConsultarDetalleReclamoDocenteDTO dto) {
        return ConsultarDetalleReclamoDocenteDomain.of(
                dto == null ? null : dto.idSolicitud(),
                dto == null ? null : dto.actorUsuarioId());
    }

    public static DetalleReclamoDocenteDTO toDTO(final DetalleReclamoDocenteDomain d) {
        final ReclamoDocenteDTO.SoporteAdjuntoDTO soporte = d.idArchivoSoporte() == null ? null
                : new ReclamoDocenteDTO.SoporteAdjuntoDTO(
                        d.idArchivoSoporte(),
                        d.nombreSoporte(),
                        d.tipoContenidoSoporte(),
                        d.tamanioSoporteBytes() == null ? 0L : d.tamanioSoporteBytes(),
                        d.fechaRegistroSoporteUtc(),
                        ConsultarReclamosDocenteMapper.urlSoporte(d.idArchivoSoporte()));
        // Sin detalle de asistencia (LEFT JOIN vacio) no hay causal que informar.
        final DetalleReclamoDocenteDTO.CausalInasistenciaDTO causal =
                d.asistio() == null && d.codigoRazonCausa() == null ? null
                        : new DetalleReclamoDocenteDTO.CausalInasistenciaDTO(
                                d.codigoRazonCausa(), d.nombreRazonCausa(), d.asistio());
        return new DetalleReclamoDocenteDTO(
                d.idSolicitud(),
                d.nombreSolicitud(),
                d.fechaSolicitud(),
                d.codigoEstado(),
                ConsultarReclamosDocenteMapper.estadoLegible(d.codigoEstado()),
                d.nombreEstado(),
                d.justificacionSolicitud(),
                d.justificacionRespuesta(),
                d.idEstudiante(),
                d.nombreEstudiante(),
                d.idSesion(),
                d.numeroSesion(),
                d.fechaHoraInicioSesionUtc(),
                d.procedenciaTemporalSesion(),
                d.idGrupo(),
                d.codigoGrupo(),
                d.nombreGrupo(),
                d.idAsignatura(),
                d.codigoAsignatura(),
                d.nombreAsignatura(),
                causal,
                soporte);
    }
}
