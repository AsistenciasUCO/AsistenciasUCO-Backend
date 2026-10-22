package co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.dto;

import java.util.UUID;

public record ActualizarFacultadDTO(
        UUID id, String nombre, UUID idDecano, UUID actorUsuarioId
) {
}
