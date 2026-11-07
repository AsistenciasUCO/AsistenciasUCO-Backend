package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.ConsultarEstudiantesDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudianteResumenDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.domain.ConsultarEstudiantesDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity.EstudiantePaginaEntity;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity.EstudianteResumenEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;

public final class ConsultarEstudiantesMapper {

    private ConsultarEstudiantesMapper() {
    }

    public static ConsultarEstudiantesDomain toDomain(final ConsultarEstudiantesDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO de consulta de estudiantes es obligatorio.");
        }
        return ConsultarEstudiantesDomain.crear(
                dto.tipoIdentificacionId(),
                dto.numeroIdentificacion(),
                dto.nombre(),
                dto.correo(),
                dto.institucionId(),
                dto.facultadId(),
                dto.programaId(),
                dto.grupoId(),
                dto.activo(),
                dto.page(),
                dto.size(),
                dto.actorUsuarioId()
        );
    }

    public static EstudiantePaginaDTO toDTO(final EstudiantePaginaEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("La pagina de estudiantes es obligatoria.");
        }
        return new EstudiantePaginaDTO(
                toDTOs(entity.items()),
                entity.totalItems(),
                entity.totalPages(),
                entity.page(),
                entity.size()
        );
    }

    private static List<EstudianteResumenDTO> toDTOs(final List<EstudianteResumenEntity> entities) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }
        return entities.stream().map(ConsultarEstudiantesMapper::toDTO).toList();
    }

    public static EstudianteResumenDTO toDTO(final EstudianteResumenEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("El estudiante consultado es obligatorio.");
        }
        return new EstudianteResumenDTO(
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
