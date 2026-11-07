package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
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
        EstadoHorarioSesion estadoTemporal,
        CancelacionSesionDTO cancelacion
) {

    /** Sesion programada (sin cancelacion). */
    public SesionGrupoHorarioDomain(final UUID id, final UUID idGrupo, final Integer codigoGrupo, final String nombreGrupo,
                                    final String nombre, final Integer numero, final String codigo,
                                    final Integer numeroSemana, final LocalDateTime fechaHoraInicio,
                                    final LocalDateTime fechaHoraFin, final LocalDateTime fechaHoraInicioUtc,
                                    final LocalDateTime fechaHoraFinUtc, final EstadoHorarioSesion estadoTemporal) {
        this(id, idGrupo, codigoGrupo, nombreGrupo, nombre, numero, codigo, numeroSemana, fechaHoraInicio, fechaHoraFin,
                fechaHoraInicioUtc, fechaHoraFinUtc, estadoTemporal, null);
    }

    public SesionGrupoHorarioDomain {
        Objects.requireNonNull(id, "La sesion es obligatoria.");
        Objects.requireNonNull(estadoTemporal, "El estado temporal es obligatorio.");
        if (estadoTemporal != EstadoHorarioSesion.CONFIRMADA) {
            fechaHoraInicioUtc = null;
            fechaHoraFinUtc = null;
        }
    }
}
