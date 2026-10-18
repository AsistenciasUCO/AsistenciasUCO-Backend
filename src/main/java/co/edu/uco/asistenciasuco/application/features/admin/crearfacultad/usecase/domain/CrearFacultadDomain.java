package co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.usecase.domain;

import java.util.UUID;

public record CrearFacultadDomain(
        UUID id,
        String nombre,
        UUID idInstitucion,
        UUID idDecano,
        UUID actorUsuarioId
) {
}
