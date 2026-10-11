package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.dto;

import java.util.UUID;

/**
 * @param usuarioEjecutor Usuario.id resuelto desde el JWT; nunca desde el cliente
 */
public record ConsultarSesionesPorGrupoV2DTO(UUID grupo, UUID usuarioEjecutor) {
}
