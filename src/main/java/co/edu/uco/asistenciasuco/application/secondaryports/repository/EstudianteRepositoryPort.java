package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteDetalleRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudiantePaginaRepositoryProjection;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto secundario para persistencia relacionada con estudiantes.
 */
public interface EstudianteRepositoryPort {

    EstudiantePaginaRepositoryProjection consultarEstudiantes(ConsultarEstudiantesRepositoryDTO dto);

    /** CULM-001 A.2-1: el ambito lo aplica el SP con el ejecutor; fuera de ambito no hay filas. */
    Optional<EstudianteDetalleRepositoryProjection> consultarEstudiantePorId(UUID estudianteId, UUID actorUsuarioId);
}
