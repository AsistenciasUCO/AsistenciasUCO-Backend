package co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.usecase.domain;

import java.util.UUID;

public record BuscarEstudiantePorDocumentoDomain(UUID grupoId, UUID tipoIdentificacionId, Integer numeroIdentificacion, UUID actorUsuarioId) {
}
