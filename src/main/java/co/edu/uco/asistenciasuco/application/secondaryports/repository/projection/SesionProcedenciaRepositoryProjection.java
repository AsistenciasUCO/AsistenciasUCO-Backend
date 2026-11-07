package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila de {@code uv_sesion_v2}: las diez columnas de la vista v1 mas la procedencia temporal y (CULM-001 B.1) la
 * cancelacion leida de {@code uv_sesion_estado}.
 *
 * @param fechaHoraInicio valor DATETIME2 almacenado, sin interpretar
 * @param procedenciaTemporal NULL cuando la fila es historica o fue escrita por v1
 * @param cancelacion {@code null} si la sesion esta programada
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
        String procedenciaTemporal,
        CancelacionSesionProjection cancelacion
) {

    /** Sesion programada (sin cancelacion). */
    public SesionProcedenciaRepositoryProjection(
            final UUID sesion,
            final UUID grupo,
            final String nombre,
            final Integer numero,
            final String codigo,
            final Integer numeroSemana,
            final String codigoGrupo,
            final String nombreGrupo,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final String procedenciaTemporal
    ) {
        this(sesion, grupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin,
                procedenciaTemporal, null);
    }
}
