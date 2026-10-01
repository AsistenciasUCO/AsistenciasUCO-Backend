package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.testdouble;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador mock temporal para persistencia de estudiantes en grupos.
 */
public final class GrupoRepositoryMockAdapter implements GrupoRepositoryPort {

    @Override
    public GrupoCommandRepositoryProjection crearGrupo(final CrearGrupoRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para crear grupo es obligatorio.");
        }
        return new GrupoCommandRepositoryProjection(dto.idGrupo(), "Grupo registrado correctamente.");
    }

    @Override
    public GrupoCommandRepositoryProjection actualizarGrupo(final ActualizarGrupoRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para actualizar grupo es obligatorio.");
        }
        return new GrupoCommandRepositoryProjection(dto.idGrupo(), "Grupo actualizado correctamente.");
    }

    @Override
    public RegistrarEstudianteRepositoryProjection registrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para registrar estudiante en grupo es obligatorio.");
        }
        return new RegistrarEstudianteRepositoryProjection("Estudiante registrado correctamente.");
    }

    @Override
    public List<GrupoRepositoryProjection> consultarGrupos() {
        return List.of();
    }

    @Override
    public List<GrupoRepositoryProjection> consultarGrupos(final UUID usuarioEjecutor) {
        return List.of();
    }

    @Override
    public List<EstudianteGrupoRepositoryProjection> consultarEstudiantesGrupo(final UUID grupoId) {
        return List.of();
    }

    @Override
    public List<EstudianteGrupoRepositoryProjection> consultarEstudiantesGrupo(final UUID grupoId, final UUID usuarioEjecutor) {
        return List.of();
    }
}
