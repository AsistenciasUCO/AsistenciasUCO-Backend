package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.domain.ConsultarEstudiantesDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity.EstudiantePaginaEntity;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity.EstudianteResumenEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudiantePaginaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteResumenRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;

public final class ConsultarEstudiantesRepositoryMapper {

    private ConsultarEstudiantesRepositoryMapper() {
    }

    public static ConsultarEstudiantesRepositoryDTO toRepositoryDTO(final ConsultarEstudiantesDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio de consulta de estudiantes es obligatorio.");
        }
        return new ConsultarEstudiantesRepositoryDTO(
                domain.tipoIdentificacionId(),
                domain.numeroIdentificacion(),
                domain.nombre(),
                domain.correo(),
                domain.institucionId(),
                domain.facultadId(),
                domain.programaId(),
                domain.grupoId(),
                domain.activo(),
                domain.page(),
                domain.size(),
                domain.actorUsuarioId()
        );
    }

    public static EstudiantePaginaEntity toUseCaseEntity(final EstudiantePaginaRepositoryProjection entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("La pagina de estudiantes consultada es obligatoria.");
        }
        return new EstudiantePaginaEntity(
                toUseCaseEntities(entity.items()),
                entity.totalItems(),
                entity.totalPages(),
                entity.page(),
                entity.size()
        );
    }

    static List<EstudianteResumenEntity> toUseCaseEntities(final List<EstudianteResumenRepositoryProjection> entities) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }
        return entities.stream().map(ConsultarEstudiantesRepositoryMapper::toUseCaseEntity).toList();
    }

    public static EstudianteResumenEntity toUseCaseEntity(final EstudianteResumenRepositoryProjection entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("El estudiante consultado es obligatorio.");
        }
        return new EstudianteResumenEntity(
                entity.id(),
                entity.idUsuario(),
                entity.tipoIdentificacionId(),
                entity.numeroIdentificacion(),
                entity.primerApellido(),
                entity.segundoApellido(),
                entity.primerNombre(),
                entity.segundoNombre(),
                entity.nombreCompleto(),
                entity.correo(),
                entity.estaActivoUsuario()
        );
    }
}
