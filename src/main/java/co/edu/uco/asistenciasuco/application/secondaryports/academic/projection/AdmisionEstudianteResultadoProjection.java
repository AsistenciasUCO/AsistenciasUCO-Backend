package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Persona admitida (CULM-001 D.4): ids confirmados por la base de datos y si el Usuario y el Estudiante se crearon ahora. */
public record AdmisionEstudianteResultadoProjection(UUID idUsuario, UUID idEstudiante, boolean esUsuarioNuevo, boolean esEstudianteNuevo) {
}
