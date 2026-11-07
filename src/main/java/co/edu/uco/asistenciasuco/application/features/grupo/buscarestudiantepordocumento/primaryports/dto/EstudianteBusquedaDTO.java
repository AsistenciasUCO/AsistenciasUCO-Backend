package co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.dto;

import java.util.UUID;

/** CULM-001 A.2-15 (DEC-027): datos minimos del estudiante encontrado; sin el correo real. */
public record EstudianteBusquedaDTO(
        UUID idEstudiante,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String nombreCompleto,
        Integer numeroIdentificacion,
        String correoEnmascarado,
        String codigoEstadoMatriculaGrupo
) {
}
