package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.usecase.domain;

import java.util.UUID;

public record CambiarEstadoDecanoDomain(UUID idDecano, UUID actorUsuarioId) {
}
