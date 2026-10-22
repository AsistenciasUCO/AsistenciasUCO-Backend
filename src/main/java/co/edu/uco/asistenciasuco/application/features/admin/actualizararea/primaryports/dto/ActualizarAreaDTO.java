package co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.dto;

import java.util.UUID;

public record ActualizarAreaDTO(
        UUID id,
        String codigo,
        String nombre,
        UUID actorUsuarioId
) {
}
