package co.edu.uco.asistenciasuco.application.features.sesion.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sesion v2 expuesta por los puertos de entrada. Las horas son instantes UTC (DATETIME2 confirmado)
 * o null cuando {@code estadoTemporal} es INDETERMINADA.
 */
public record SesionV2ConsultadaDTO(
        UUID sesion,
        UUID grupo,
        String nombre,
        Integer numero,
        String codigo,
        Integer numeroSemana,
        String codigoGrupo,
        String nombreGrupo,
        LocalDateTime fechaHoraInicioUtc,
        LocalDateTime fechaHoraFinUtc,
        String estadoTemporal,
        String procedenciaTemporal
) {
}
