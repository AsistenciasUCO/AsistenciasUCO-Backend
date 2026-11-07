package co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.usecase.domain;

import java.util.UUID;

public record RetirarReclamoDomain(UUID idReclamo, UUID actorUsuarioId) {
}
