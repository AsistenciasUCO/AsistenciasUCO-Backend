package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.SesionGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpUtcInstantCodec;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wire de HU060 {@code GET /api/v1/coordinador/grupos/{grupoId}/sesiones}. Los campos v1 se conservan sin cambios
 * ({@code fechaHoraInicio}/{@code fechaHoraFin}: {@code LocalDateTime} sin zona). APP-04 agrega, de forma aditiva,
 * {@code fechaHoraInicioUtc}/{@code fechaHoraFinUtc} con sufijo {@code Z} y la misma precision DATETIME2(7) que el
 * contrato v2, solo si {@code estadoTemporal = CONFIRMADA}; en otro caso se serializan como {@code null} explicito.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SesionGrupoCoordinadorResponse(
        UUID id,
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        String nombre,
        Integer numero,
        String codigo,
        Integer numeroSemana,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String fechaHoraInicioUtc,
        String fechaHoraFinUtc,
        String estadoTemporal
) {

    public static SesionGrupoCoordinadorResponse from(final SesionGrupoDTO dto) {
        return new SesionGrupoCoordinadorResponse(dto.id(), dto.idGrupo(), dto.codigoGrupo(), dto.nombreGrupo(),
                dto.nombre(), dto.numero(), dto.codigo(), dto.numeroSemana(), dto.fechaHoraInicio(),
                dto.fechaHoraFin(), utc(dto.fechaHoraInicioUtc()), utc(dto.fechaHoraFinUtc()), dto.estadoTemporal());
    }

    private static String utc(final LocalDateTime value) {
        return value == null ? null : HttpUtcInstantCodec.fromUtcDatabaseDateTime(value);
    }
}
