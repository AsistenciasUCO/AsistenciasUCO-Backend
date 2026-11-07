package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CrearUsuarioRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto secundario para persistencia relacionada con usuarios.
 */
public interface UsuarioRepositoryPort {

    /**
     * CULM-001 A.2-15: correo real del estudiante resuelto por {@code usp_buscar_estudiante_por_documento_grupo}. Uso interno
     * de la matricula manual: nunca se expone por HTTP ni se consulta a partir de un documento libre.
     */
    Optional<String> consultarCorreoPorEstudiante(UUID idEstudiante);

    CrearUsuarioRepositoryProjection crearUsuario(CrearUsuarioRepositoryDTO dto);

    Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorCorreo(String correo);

    Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorId(UUID idUsuario);

    Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorIdentificacion(
            UUID tipoIdentificacionId,
            Integer numeroIdentificacion
    );

    /**
     * Consulta por el documento sin forzar un tipo de identificación. Los flujos de
     * administración de coordinadores reciben el documento, pero no reciben el UUID
     * del catálogo de tipos; la implementación debe conservar la unicidad institucional.
     */
    default Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorNumeroIdentificacion(
            final Integer numeroIdentificacion
    ) {
        return Optional.empty();
    }
}
