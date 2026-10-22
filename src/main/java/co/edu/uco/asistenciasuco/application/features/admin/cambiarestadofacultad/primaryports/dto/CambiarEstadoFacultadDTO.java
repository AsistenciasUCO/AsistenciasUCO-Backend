package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.dto;

import java.util.UUID;

public record CambiarEstadoFacultadDTO(
        UUID id, Boolean activa, UUID actorUsuarioId
) {
}
