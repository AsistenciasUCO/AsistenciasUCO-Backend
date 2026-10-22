package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamoDocenteDTO;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Detalle de una solicitud de revision para el docente titular (HU036). {@code fechaSesionUtc} es un instante UTC
 * (nulo si la hora historica de la sesion no tiene procedencia verificable). {@code causalInasistencia} es nula si
 * la asistencia no tiene detalle registrado; {@code soporteAdjunto} es nulo si no hay archivo adjunto.
 */
public record DetalleReclamoDocenteDTO(
        UUID id,
        String nombre,
        LocalDate fechaSolicitud,
        String codigoEstado,
        String estadoSolicitud,
        String nombreEstado,
        String justificacionSolicitud,
        String justificacionRespuesta,
        UUID estudianteId,
        String estudianteNombre,
        UUID sesionId,
        Integer sesionNumero,
        Instant fechaSesionUtc,
        String procedenciaTemporalSesion,
        UUID grupoId,
        Integer grupoCodigo,
        String grupoNombre,
        UUID materiaId,
        String materiaCodigo,
        String materiaNombre,
        CausalInasistenciaDTO causalInasistencia,
        ReclamoDocenteDTO.SoporteAdjuntoDTO soporteAdjunto
) {

    /** Causa registrada de la inasistencia ({@code RazonCausa}) y si el estudiante figura como asistente. */
    public record CausalInasistenciaDTO(String codigo, String nombre, Boolean asistio) {
    }
}
