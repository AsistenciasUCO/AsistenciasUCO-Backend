package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.primaryports.dto;

import java.util.UUID;

/** CULM-001 A.2-8: cambio de estado (toggle) del decano por el administrador autenticado. */
public record CambiarEstadoDecanoDTO(UUID idDecano, UUID actorUsuarioId) {
}
