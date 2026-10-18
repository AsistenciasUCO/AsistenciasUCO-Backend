package co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.dto;

import java.util.UUID;

public record CrearFacultadDTO(
        UUID id,
        String nombre,
        UUID idInstitucion,
        UUID idDecano,
        UUID actorUsuarioId
) {
}
