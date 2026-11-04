package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionMateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpUtcInstantCodec;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wire de {@code GET /api/v1/estudiante/materias/{materiaId}/sesiones}. Los campos existentes se conservan sin
 * cambios ({@code fechaHoraInicio}/{@code fechaHoraFin}: {@code LocalDateTime} sin zona). APP-04 agrega, de forma
 * aditiva y con la misma regla que HU060 coordinador, {@code fechaHoraInicioUtc}/{@code fechaHoraFinUtc} con sufijo
 * {@code Z} solo cuando {@code estadoTemporal = CONFIRMADA}; en otro caso son nulos y la SPA no inventa una zona.
 */
public record SesionMateriaEstudianteResponse(
        UUID id,
        String nombre,
        Integer numero,
        String codigo,
        Integer numeroSemana,
        UUID idGrupo,
        String codigoGrupo,
        String nombreGrupo,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String fechaHoraInicioUtc,
        String fechaHoraFinUtc,
        String estadoTemporal,
        String estadoAsistencia,
        String estadoSolicitudRevision,
        boolean reclamable
) {

    public static SesionMateriaEstudianteResponse from(final SesionMateriaEstudianteDTO dto) {
        return new SesionMateriaEstudianteResponse(dto.id(), dto.nombre(), dto.numero(), dto.codigo(),
                dto.numeroSemana(), dto.idGrupo(), dto.codigoGrupo(), dto.nombreGrupo(), dto.fechaHoraInicio(),
                dto.fechaHoraFin(), utc(dto.fechaHoraInicioUtc()), utc(dto.fechaHoraFinUtc()), dto.estadoTemporal(),
                dto.estadoAsistencia(), dto.estadoSolicitudRevision(), dto.reclamable());
    }

    private static String utc(final LocalDateTime value) {
        return value == null ? null : HttpUtcInstantCodec.fromUtcDatabaseDateTime(value);
    }
}
