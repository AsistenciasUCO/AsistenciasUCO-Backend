package co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.dto;

import java.util.UUID;

public record BuscarEstudiantePorDocumentoDTO(UUID grupoId, UUID tipoIdentificacionId, Integer numeroIdentificacion, UUID actorUsuarioId) {
}
