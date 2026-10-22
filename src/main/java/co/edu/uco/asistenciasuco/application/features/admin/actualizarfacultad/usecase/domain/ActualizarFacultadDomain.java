package co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.usecase.domain;

import java.util.UUID;

public record ActualizarFacultadDomain(
        UUID id, String nombre, UUID idDecano, UUID actorUsuarioId
) {
}
