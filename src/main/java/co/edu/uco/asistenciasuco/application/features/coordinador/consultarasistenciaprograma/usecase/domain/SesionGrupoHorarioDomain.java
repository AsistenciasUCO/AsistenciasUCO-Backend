package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * APP-04: sesion del grupo con su horario v1 intacto ({@code fechaHoraInicio}/{@code fechaHoraFin}, sin zona) y,
 * solo si la procedencia temporal es CONFIRMADA (UTC-D06/D07), sus instantes UTC. Una sesion INDETERMINADA nunca
 * expone instantes inferidos.
 */
public record SesionGrupoHorarioDomain(
        UUID id,
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        String nombre,
        Integer numero,
        String codigo,
        Integer numeroSemana,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        LocalDateTime fechaHoraInicioUtc,
        LocalDateTime fechaHoraFinUtc,
        EstadoHorarioSesion estadoTemporal
) {

    public SesionGrupoHorarioDomain {
        Objects.requireNonNull(id, "La sesion es obligatoria.");
        Objects.requireNonNull(estadoTemporal, "El estado temporal es obligatorio.");
        if (estadoTemporal != EstadoHorarioSesion.CONFIRMADA) {
            fechaHoraInicioUtc = null;
            fechaHoraFinUtc = null;
        }
    }
}
