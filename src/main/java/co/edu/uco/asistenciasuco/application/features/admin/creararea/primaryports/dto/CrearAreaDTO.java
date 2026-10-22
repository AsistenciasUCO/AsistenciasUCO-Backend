package co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.dto;

import java.util.UUID;

public record CrearAreaDTO(
        UUID id,
        String codigo,
        String nombre,
        UUID actorUsuarioId
) {
}
