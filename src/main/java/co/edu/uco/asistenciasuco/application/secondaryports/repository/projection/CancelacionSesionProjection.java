package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.time.LocalDateTime;

/**
 * CULM-001 B.1: columnas de cancelacion de {@code uv_sesion_estado}. Solo existe cuando la sesion esta cancelada.
 *
 * @param codigoTipo          {@code codigoTipoCancelacion} del catalogo {@code TipoCancelacionSesion}
 * @param fechaCancelacionUtc DATETIME2 en UTC sin zona, tal como lo guarda la DB
 */
public record CancelacionSesionProjection(String codigoTipo, String motivo, String nombreFestivo,
                                          LocalDateTime fechaCancelacionUtc) {
}
