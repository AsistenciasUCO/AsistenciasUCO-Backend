package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code estadoAsistencia}: PRESENTE | AUSENTE | JUSTIFICADA | NO_DISPONIBLE (nunca nulo).
 * {@code estadoSolicitudRevision}: nombre del estado de la solicitud de revision existente, o nulo.
 * {@code reclamable}: pista de UI (ausencia confirmada y sin solicitud previa); no sustituye la validacion al radicar.
 */
public record SesionMateriaEstudianteDTO(UUID id, String nombre, Integer numero, String codigo, Integer numeroSemana,
                                         UUID idGrupo, String codigoGrupo, String nombreGrupo,
                                         LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin,
                                         LocalDateTime fechaHoraInicioUtc, LocalDateTime fechaHoraFinUtc,
                                         String estadoTemporal,
                                         String estadoAsistencia, String estadoSolicitudRevision, boolean reclamable,
                                         String estado, CancelacionSesionDTO cancelacion) {

    /** Sesion programada (sin cancelacion). */
    public SesionMateriaEstudianteDTO(final UUID id, final String nombre, final Integer numero, final String codigo,
                                      final Integer numeroSemana, final UUID idGrupo, final String codigoGrupo,
                                      final String nombreGrupo, final LocalDateTime fechaHoraInicio,
                                      final LocalDateTime fechaHoraFin, final LocalDateTime fechaHoraInicioUtc,
                                      final LocalDateTime fechaHoraFinUtc, final String estadoTemporal,
                                      final String estadoAsistencia, final String estadoSolicitudRevision,
                                      final boolean reclamable) {
        this(id, nombre, numero, codigo, numeroSemana, idGrupo, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin,
                fechaHoraInicioUtc, fechaHoraFinUtc, estadoTemporal, estadoAsistencia, estadoSolicitudRevision, reclamable,
                EstadoSesion.PROGRAMADA.name(), null);
    }
}
