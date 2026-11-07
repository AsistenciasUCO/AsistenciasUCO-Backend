package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain.ConsultarAsistenciasPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity.AsistenciaConsultadaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper entre el caso de uso y el contrato del puerto secundario.
 */
public final class ConsultarAsistenciasPorGrupoRepositoryMapper {

    private ConsultarAsistenciasPorGrupoRepositoryMapper() {
    }

    public static ConsultarAsistenciasPorGrupoRepositoryDTO toRepositoryDTO(
            final ConsultarAsistenciasPorGrupoDomain domain
    ) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar asistencias por grupo es obligatorio.");
        }

        return new ConsultarAsistenciasPorGrupoRepositoryDTO(
                domain.getGrupo(),
                domain.getSesion(),
                domain.getUsuarioEjecutor()
        );
    }

    public static List<AsistenciaConsultadaEntity> toUseCaseEntities(final List<AsistenciaRepositoryProjection> entities) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }

        final List<AsistenciaConsultadaEntity> resultado = new ArrayList<>();

        for (final AsistenciaRepositoryProjection entity : entities) {
            resultado.add(new AsistenciaConsultadaEntity(
                    entity.getAsistencia(),
                    entity.getEstudiante(),
                    entity.getGrupo(),
                    entity.getSesion(),
                    entity.isPresente(),
                    entity.getEstado(),
                    entity.getObservacion()
            ));
        }

        return resultado;
    }
}
