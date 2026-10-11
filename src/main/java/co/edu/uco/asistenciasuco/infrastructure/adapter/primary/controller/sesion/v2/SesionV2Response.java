package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * Wire de {@code SesionV2} (UTC-D07). {@code fechaHoraInicio}/{@code fechaHoraFin} son instantes UTC
 * con sufijo {@code Z} solo si {@code estadoTemporal = CONFIRMADA}; en una sesion INDETERMINADA se
 * serializan explicitamente como {@code null} junto con {@code procedenciaTemporal}.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SesionV2Response(
        UUID sesion,
        UUID grupo,
        String nombre,
        Integer numero,
        String codigo,
        Integer numeroSemana,
        String codigoGrupo,
        String nombreGrupo,
        String fechaHoraInicio,
        String fechaHoraFin,
        String estadoTemporal,
        String procedenciaTemporal
) {
}
