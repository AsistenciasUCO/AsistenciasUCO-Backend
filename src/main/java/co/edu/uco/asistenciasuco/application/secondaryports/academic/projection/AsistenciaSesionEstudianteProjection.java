package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Hecho crudo de asistencia de una sesion del estudiante autenticado, tal como lo expone la DB
 * ({@code uv_asistencia} + {@code uv_detalle_asistencia} + {@code uv_solicitud_revision_asistencia}).
 * No interpreta nada: {@code asistio}, {@code codigoRazonCausa} y {@code codigoDetalle} son nulos cuando la
 * sesion tiene cabecera de asistencia pero ningun detalle registrado, y {@code idSolicitud} es nulo cuando
 * la asistencia no tiene solicitud de revision. Una sesion sin cabecera no genera fila.
 */
public record AsistenciaSesionEstudianteProjection(
        UUID idSesion,
        Integer codigoDetalle,
        Boolean asistio,
        String codigoRazonCausa,
        UUID idSolicitud,
        String estadoSolicitud,
        LocalDate fechaSolicitud
) {
}
