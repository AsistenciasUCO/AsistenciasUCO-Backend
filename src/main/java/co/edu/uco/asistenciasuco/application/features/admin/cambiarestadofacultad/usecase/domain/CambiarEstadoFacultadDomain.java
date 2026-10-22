package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.domain;

import java.util.UUID;

public record CambiarEstadoFacultadDomain(
        UUID id, Boolean activa, UUID actorUsuarioId
) {
}
