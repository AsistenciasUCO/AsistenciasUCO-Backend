package co.edu.uco.asistenciasuco.application.features.sesion.common.dto;

import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;

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
        String procedenciaTemporal,
        CancelacionSesionDTO cancelacion
) {

    /** Sesion programada (sin cancelacion). */
    public SesionV2ConsultadaDTO(
            final UUID sesion,
            final UUID grupo,
            final String nombre,
            final Integer numero,
            final String codigo,
            final Integer numeroSemana,
            final String codigoGrupo,
            final String nombreGrupo,
            final LocalDateTime fechaHoraInicioUtc,
            final LocalDateTime fechaHoraFinUtc,
            final String estadoTemporal,
            final String procedenciaTemporal
    ) {
        this(sesion, grupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo, fechaHoraInicioUtc,
                fechaHoraFinUtc, estadoTemporal, procedenciaTemporal, null);
    }

    /** CULM-001 B.1 (§3.2.4): {@code PROGRAMADA} o {@code CANCELADA}. */
    public String estado() {
        return EstadoSesion.de(cancelacion != null).name();
    }
}
