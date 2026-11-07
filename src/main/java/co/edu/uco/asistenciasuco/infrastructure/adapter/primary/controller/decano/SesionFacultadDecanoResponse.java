package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.CancelacionSesionResponse;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.SesionFacultadDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpUtcInstantCodec;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wire de HU145 {@code GET /api/v1/decano/sesiones}. Los campos v1 se conservan sin cambios
 * ({@code fechaHoraInicio}/{@code fechaHoraFin}: {@code LocalDateTime} sin zona). Igual que el cronograma del
 * coordinador (APP-04), agrega de forma aditiva {@code fechaHoraInicioUtc}/{@code fechaHoraFinUtc} con sufijo
 * {@code Z} solo si {@code estadoTemporal = CONFIRMADA}; en otro caso se serializan como {@code null} explicito.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SesionFacultadDecanoResponse(
        UUID id,
        String nombre,
        String codigo,
        Integer numeroSemana,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String nombreAsignatura,
        UUID idPrograma,
        String nombrePrograma,
        String fechaHoraInicioUtc,
        String fechaHoraFinUtc,
        String estadoTemporal,
        String estado,
        CancelacionSesionResponse cancelacion
) {

    public static SesionFacultadDecanoResponse from(final SesionFacultadDTO dto) {
        return new SesionFacultadDecanoResponse(dto.id(), dto.nombre(), dto.codigo(), dto.numeroSemana(),
                dto.fechaHoraInicio(), dto.fechaHoraFin(), dto.idGrupo(), dto.codigoGrupo(), dto.nombreGrupo(),
                dto.idAsignatura(), dto.nombreAsignatura(), dto.idPrograma(), dto.nombrePrograma(),
                utc(dto.fechaHoraInicioUtc()), utc(dto.fechaHoraFinUtc()), dto.estadoTemporal(), dto.estado(),
                CancelacionSesionResponse.from(dto.cancelacion()));
    }

    private static String utc(final LocalDateTime value) {
        return value == null ? null : HttpUtcInstantCodec.fromUtcDatabaseDateTime(value);
    }
}
