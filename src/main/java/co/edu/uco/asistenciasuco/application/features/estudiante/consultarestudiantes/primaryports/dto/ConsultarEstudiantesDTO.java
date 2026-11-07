package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto;

import java.util.UUID;

public record ConsultarEstudiantesDTO(
        UUID tipoIdentificacionId,
        Integer numeroIdentificacion,
        String nombre,
        String correo,
        UUID institucionId,
        UUID facultadId,
        UUID programaId,
        UUID grupoId,
        Boolean activo,
        Integer page,
        Integer size,
        UUID actorUsuarioId
) {
}
