package co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.primaryports.dto;

import java.util.UUID;

/** CULM-001 A.2-7: retiro del reclamo propio; el ejecutor sale del token. */
public record RetirarReclamoDTO(UUID idReclamo, UUID actorUsuarioId) {
}
