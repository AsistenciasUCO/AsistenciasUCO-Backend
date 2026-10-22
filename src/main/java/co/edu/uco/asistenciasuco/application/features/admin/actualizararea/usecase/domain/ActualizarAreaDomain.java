package co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.domain;

import java.util.UUID;

public record ActualizarAreaDomain(
        UUID id,
        String codigo,
        String nombre,
        UUID actorUsuarioId
) {
}
