package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

public record ConsultarEstudiantesRepositoryDTO(
        UUID tipoIdentificacionId,
        Integer numeroIdentificacion,
        String nombre,
        String correo,
        UUID institucionId,
        UUID facultadId,
        UUID programaId,
        UUID grupoId,
        Boolean activo,
        int page,
        int size,
        UUID actorUsuarioId
) {
}
