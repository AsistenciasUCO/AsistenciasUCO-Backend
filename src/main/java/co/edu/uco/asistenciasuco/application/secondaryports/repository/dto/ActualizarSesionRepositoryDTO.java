package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import co.edu.uco.asistenciasuco.application.features.sesion.common.ContratoHorarioSesion;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActualizarSesionRepositoryDTO(
        UUID sesion,
        String nombre,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        UUID usuarioEjecutor,
        ContratoHorarioSesion contratoTemporal
) {

    public ActualizarSesionRepositoryDTO {
        contratoTemporal = contratoTemporal == null ? ContratoHorarioSesion.LOCAL_SIN_ZONA_V1 : contratoTemporal;
    }

    public ActualizarSesionRepositoryDTO(
            final UUID sesion,
            final String nombre,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final UUID usuarioEjecutor
    ) {
        this(sesion, nombre, fechaHoraInicio, fechaHoraFin, usuarioEjecutor, ContratoHorarioSesion.LOCAL_SIN_ZONA_V1);
    }
}
