package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.UUID;

/**
 * CULM-001 A.2-15 (DEC-027): datos minimos del estudiante encontrado por documento exacto en el grupo del docente titular.
 * El correo real nunca sale: solo {@code correoEnmascarado} (primer caracter + *** + dominio, calculado en el SP).
 * {@code codigoEstadoMatriculaGrupo}: estado de su matricula en ESE grupo (A, I...) o {@code null} si no tiene.
 */
public record EstudianteBusquedaRepositoryProjection(
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
