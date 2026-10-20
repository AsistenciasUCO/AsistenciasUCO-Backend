package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;

import java.util.List;
import java.util.UUID;

/**
 * Lectura de la asistencia propia del estudiante en las sesiones de una asignatura.
 * El estudiante lo resuelve el backend a partir del usuario autenticado; jamas llega desde el cliente.
 */
public interface AsistenciaSesionEstudianteQueryPort {

    List<AsistenciaSesionEstudianteProjection> consultarAsistenciaSesiones(UUID idEstudiante, UUID idAsignatura);
}
