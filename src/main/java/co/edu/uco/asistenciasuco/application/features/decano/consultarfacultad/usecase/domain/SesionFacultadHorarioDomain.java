package co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * HU145: sesion del cronograma de la facultad con su horario v1 intacto ({@code fechaHoraInicio}/{@code fechaHoraFin},
 * sin zona) y, solo si la procedencia temporal es CONFIRMADA (UTC-D06/D07), sus instantes UTC. Mismo criterio que el
 * cronograma del coordinador (APP-04): una sesion INDETERMINADA nunca expone instantes inferidos.
 */
public record SesionFacultadHorarioDomain(
        UUID id,
        String nombre,
        String codigo,
        Integer numeroSemana,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String nombreAsignatura,
        UUID idPrograma,
        String nombrePrograma,
        LocalDateTime fechaHoraInicioUtc,
        LocalDateTime fechaHoraFinUtc,
        EstadoHorarioSesion estadoTemporal
) {

    public SesionFacultadHorarioDomain {
        Objects.requireNonNull(id, "La sesion es obligatoria.");
        Objects.requireNonNull(estadoTemporal, "El estado temporal es obligatorio.");
        if (estadoTemporal != EstadoHorarioSesion.CONFIRMADA) {
            fechaHoraInicioUtc = null;
            fechaHoraFinUtc = null;
        }
    }
}
