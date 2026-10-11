package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila de {@code uv_sesion_v2}: las diez columnas de la vista v1 mas la procedencia temporal.
 *
 * @param fechaHoraInicio valor DATETIME2 almacenado, sin interpretar
 * @param procedenciaTemporal NULL cuando la fila es historica o fue escrita por v1
 */
public record SesionProcedenciaRepositoryProjection(
        UUID sesion,
        UUID grupo,
        String nombre,
        Integer numero,
        String codigo,
        Integer numeroSemana,
        String codigoGrupo,
        String nombreGrupo,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String procedenciaTemporal
) {
}
