package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fila JPQL de asistencia propia por sesion. Los campos de detalle y de solicitud son nulos cuando la cabecera de
 * asistencia no tiene detalle o no tiene solicitud (left join); no se sustituyen por valores por defecto.
 */
public record AsistenciaSesionEstudianteQueryRow(UUID idSesion, Integer codigoDetalle, Boolean asistio,
                                                 String codigoRazonCausa, UUID idSolicitud, String estadoSolicitud,
                                                 LocalDate fechaSolicitud) {
}
