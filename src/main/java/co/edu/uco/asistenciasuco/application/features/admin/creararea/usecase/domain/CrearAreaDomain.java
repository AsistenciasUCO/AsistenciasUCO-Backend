package co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.domain;

import java.util.UUID;

public record CrearAreaDomain(
        UUID id,
        String codigo,
        String nombre,
        UUID actorUsuarioId
) {
}
