package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.ConsultarEstudiantePorIdDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.EstudianteContextoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.EstudianteDetalleDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.domain.ConsultarEstudiantePorIdDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.entity.EstudianteContextoAcademicoEntity;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.entity.EstudianteDetalleEntity;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.mapper.ConsultarEstudiantesMapper;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;
import java.util.UUID;

public final class ConsultarEstudiantePorIdMapper {

    private ConsultarEstudiantePorIdMapper() {
    }

    public static ConsultarEstudiantePorIdDomain toDomain(final ConsultarEstudiantePorIdDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar estudiante por ID es obligatorio.");
        }
        return ConsultarEstudiantePorIdDomain.crear(dto.estudianteId(), dto.actorUsuarioId());
    }

    public static EstudianteDetalleDTO toDTO(final EstudianteDetalleEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("El detalle de estudiante es obligatorio.");
        }
        return new EstudianteDetalleDTO(
                ConsultarEstudiantesMapper.toDTO(entity.datosPersonales()),
                toDTOs(entity.contextosAcademicos())
        );
    }

    private static List<EstudianteContextoAcademicoDTO> toDTOs(final List<EstudianteContextoAcademicoEntity> entities) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }
        return entities.stream().map(ConsultarEstudiantePorIdMapper::toDTO).toList();
    }

    private static EstudianteContextoAcademicoDTO toDTO(final EstudianteContextoAcademicoEntity entity) {
        return new EstudianteContextoAcademicoDTO(
                entity.idInstitucion(),
                entity.nombreInstitucion(),
                entity.idFacultad(),
                entity.nombreFacultad(),
                entity.idPrograma(),
                entity.nombrePrograma(),
                entity.idPlanEstudio(),
                entity.inpPlanEstudio(),
                entity.idAsignatura(),
                entity.nombreAsignatura(),
                entity.idGrupo(),
                entity.nombreGrupo()
        );
    }
}
