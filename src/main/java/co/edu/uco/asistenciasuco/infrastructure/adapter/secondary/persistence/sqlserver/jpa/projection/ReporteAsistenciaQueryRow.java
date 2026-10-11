package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila JPQL del reporte de asistencia por grupo (LB-008 JPA-05). {@code asistio} y {@code nombreRazonCausa}
 * son NULL cuando el LEFT JOIN a {@code uv_detalle_asistencia} no encuentra fila.
 */
public record ReporteAsistenciaQueryRow(Integer codigoGrupo, String nombreGrupo, Integer numero, String nombre,
                                        LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, Integer numeroIdentificacion,
                                        String nombreCompleto, String correo, Boolean asistio,
                                        String nombreRazonCausa) {
}
