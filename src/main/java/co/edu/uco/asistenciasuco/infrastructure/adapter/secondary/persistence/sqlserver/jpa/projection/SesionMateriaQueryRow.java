package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila JPQL de sesiones de materia del estudiante sobre {@code uv_sesion} (LB-008 JPA-05).
 * Las fechas DATETIME2 se proyectan como {@link LocalDateTime} literal (MAINT-003K): jamas via java.util.Date, que aplica la zona de la JVM.
 */
public record SesionMateriaQueryRow(UUID id, String nombre, Integer numero, String codigo, Integer numeroSemana,
                                    UUID idGrupo, Integer codigoGrupo, String nombreGrupo, LocalDateTime fechaHoraInicio,
                                    LocalDateTime fechaHoraFin) {
}
