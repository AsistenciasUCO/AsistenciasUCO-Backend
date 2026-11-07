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

    /** CULM-001 A.2-2: listado completo dentro del ambito del ejecutor (usp_consultar_grupos_paginado). */
    List<GrupoRepositoryProjection> consultarGrupos(UUID usuarioEjecutor);

    /** CULM-001 A.2-2: grupo fuera de ambito o inexistente: ForbiddenException (403, SEC_002). */
    List<EstudianteGrupoRepositoryProjection> consultarEstudiantesGrupo(UUID grupoId, UUID usuarioEjecutor);

}
