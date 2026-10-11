package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.dto;

import java.util.UUID;

/**
 * @param usuarioEjecutor Usuario.id resuelto desde el JWT; nunca desde el cliente
 */
public record ConsultarSesionV2DTO(UUID sesion, UUID usuarioEjecutor) {
}
