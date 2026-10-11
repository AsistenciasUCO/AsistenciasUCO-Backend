package co.edu.uco.asistenciasuco.application.features.sesion.common.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.SesionV2ConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;

import java.util.List;
import java.util.Objects;

/**
 * Traduce filas de {@code uv_sesion_v2} al modelo v2 aplicando la regla de procedencia, y el modelo
 * al DTO de los puertos de entrada.
 */
public final class SesionProcedenciaMapper {

    private SesionProcedenciaMapper() {
    }

    public static SesionProcedenciaEntity toEntity(final SesionProcedenciaRepositoryProjection row) {
        Objects.requireNonNull(row, "La fila de sesion v2 es obligatoria.");
        return SesionProcedenciaEntity.desdeAlmacenamiento(
                row.sesion(), row.grupo(), row.nombre(), row.numero(), row.codigo(), row.numeroSemana(),
                row.codigoGrupo(), row.nombreGrupo(), row.fechaHoraInicio(), row.fechaHoraFin(),
                row.procedenciaTemporal()
        );
    }

    public static SesionV2ConsultadaDTO toDTO(final SesionProcedenciaEntity entity) {
        Objects.requireNonNull(entity, "La sesion v2 es obligatoria.");
        return new SesionV2ConsultadaDTO(
                entity.sesion(), entity.grupo(), entity.nombre(), entity.numero(), entity.codigo(),
                entity.numeroSemana(), entity.codigoGrupo(), entity.nombreGrupo(),
                entity.fechaHoraInicioUtc(), entity.fechaHoraFinUtc(),
                entity.estadoTemporal().name(), entity.procedenciaTemporal()
        );
    }

    public static List<SesionV2ConsultadaDTO> toDTOs(final List<SesionProcedenciaEntity> entities) {
        return entities.stream().map(SesionProcedenciaMapper::toDTO).toList();
    }
}
