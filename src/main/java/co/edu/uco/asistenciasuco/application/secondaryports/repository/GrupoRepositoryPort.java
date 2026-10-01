package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;

import java.util.List;
import java.util.UUID;

/**
 * Puerto secundario para persistencia relacionada con grupos.
 */
public interface GrupoRepositoryPort {

    GrupoCommandRepositoryProjection crearGrupo(CrearGrupoRepositoryDTO dto);

    GrupoCommandRepositoryProjection actualizarGrupo(ActualizarGrupoRepositoryDTO dto);

    RegistrarEstudianteRepositoryProjection registrarEstudianteEnGrupo(RegistrarEstudianteRepositoryDTO dto);

    List<GrupoRepositoryProjection> consultarGrupos();

    default List<GrupoRepositoryProjection> consultarGrupos(UUID usuarioEjecutor) {
        return consultarGrupos();
    }

    List<EstudianteGrupoRepositoryProjection> consultarEstudiantesGrupo(UUID grupoId);

    default List<EstudianteGrupoRepositoryProjection> consultarEstudiantesGrupo(UUID grupoId, UUID usuarioEjecutor) {
        return consultarEstudiantesGrupo(grupoId);
    }
}
