package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Solicitud de revision de asistencia visible para el docente titular, con los datos minimos del
 * soporte adjunto (si existe). Los instantes son UTC; {@code procedenciaTemporalSesion} indica si la
 * hora de la sesion es verificablemente UTC ({@code UTC_V2}, {@code UTC_GENERADOR}, {@code UTC_OWNER})
 * o indeterminada ({@code null}, dato historico), en cuyo caso {@code fechaHoraInicioSesionUtc} es nula.
 */
public record SolicitudRevisionDocenteProjection(
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
        UUID idArchivoSoporte,
        String nombreSoporte,
        String tipoContenidoSoporte,
        Long tamanioSoporteBytes,
        Instant fechaRegistroSoporteUtc,
        int totalRegistros
) {
}
