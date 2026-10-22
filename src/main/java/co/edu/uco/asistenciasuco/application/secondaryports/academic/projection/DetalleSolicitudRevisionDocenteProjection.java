package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Detalle de una solicitud de revision de asistencia visible para el docente titular (HU036): datos de la
 * solicitud, estudiante, sesion, grupo y asignatura, causal de la asistencia ({@code asistio} y {@code RazonCausa}) y
 * metadatos del soporte adjunto. Causal y soporte son nulos si no existe detalle de asistencia o archivo adjunto.
 * Los instantes son UTC; {@code fechaHoraInicioSesionUtc} es nula si {@code procedenciaTemporalSesion} es nula.
 */
public record DetalleSolicitudRevisionDocenteProjection(
        UUID idSolicitud,
        String nombreSolicitud,
        LocalDate fechaSolicitud,
        String codigoEstado,
        String nombreEstado,
        String justificacionSolicitud,
        String justificacionRespuesta,
        UUID idEstudiante,
        String nombreEstudiante,
        UUID idSesion,
        Integer numeroSesion,
        Instant fechaHoraInicioSesionUtc,
        String procedenciaTemporalSesion,
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String codigoAsignatura,
        String nombreAsignatura,
        Boolean asistio,
        String codigoRazonCausa,
        String nombreRazonCausa,
        UUID idArchivoSoporte,
        String nombreSoporte,
        String tipoContenidoSoporte,
        Long tamanioSoporteBytes,
        Instant fechaRegistroSoporteUtc
) {
}
