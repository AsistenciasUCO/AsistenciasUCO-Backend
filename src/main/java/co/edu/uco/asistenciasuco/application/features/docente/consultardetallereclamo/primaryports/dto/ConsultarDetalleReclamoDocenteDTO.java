package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto;

import java.util.UUID;

/** Entrada: la solicitud sale de la ruta y el docente solo del usuario autenticado (nunca del request). */
public record ConsultarDetalleReclamoDocenteDTO(UUID idSolicitud, UUID actorUsuarioId) {
}
