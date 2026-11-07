package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de sesiones de la materia del estudiante.
 * {@code estadoSolicitudRevision} es el nombre del estado de la solicitud vigente (nulo si no hay) y
 * {@code reclamable} es solo una pista de presentacion: la autorizacion definitiva es del backend/DB al radicar.
 * CULM-001 D.1 (HU026B): {@code fechaLimiteJustificacionUtc} y {@code plazoJustificacionVigente} los calcula la base de datos
 * (limite exclusivo en UTC; nulo si faltan sus parametros) y Java solo los transporta.
 */
public record SesionMateriaEstudianteDomain(UUID id, String nombre, Integer numero, String codigo, Integer numeroSemana,
                                            UUID idGrupo, String codigoGrupo, String nombreGrupo,
                                            LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin,
                                            LocalDateTime fechaHoraInicioUtc, LocalDateTime fechaHoraFinUtc,
                                            EstadoHorarioSesion estadoTemporal,
                                            EstadoAsistenciaSesion estadoAsistencia, String estadoSolicitudRevision,
                                            boolean reclamable, CancelacionSesionDTO cancelacion,
                                            LocalDateTime fechaLimiteJustificacionUtc,
                                            boolean plazoJustificacionVigente) {

    /** Sesion programada (sin cancelacion). */
    public SesionMateriaEstudianteDomain(final UUID id, final String nombre, final Integer numero, final String codigo,
                                         final Integer numeroSemana, final UUID idGrupo, final String codigoGrupo,
                                         final String nombreGrupo, final LocalDateTime fechaHoraInicio,
                                         final LocalDateTime fechaHoraFin, final LocalDateTime fechaHoraInicioUtc,
                                         final LocalDateTime fechaHoraFinUtc, final EstadoHorarioSesion estadoTemporal,
                                         final EstadoAsistenciaSesion estadoAsistencia,
                                         final String estadoSolicitudRevision, final boolean reclamable) {
        this(id, nombre, numero, codigo, numeroSemana, idGrupo, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin,
                fechaHoraInicioUtc, fechaHoraFinUtc, estadoTemporal, estadoAsistencia, estadoSolicitudRevision, reclamable,
                null, null, false);
    }

    /** Sesion con cancelacion opcional y sin informacion de plazo. */
    public SesionMateriaEstudianteDomain(final UUID id, final String nombre, final Integer numero, final String codigo,
                                         final Integer numeroSemana, final UUID idGrupo, final String codigoGrupo,
                                         final String nombreGrupo, final LocalDateTime fechaHoraInicio,
                                         final LocalDateTime fechaHoraFin, final LocalDateTime fechaHoraInicioUtc,
                                         final LocalDateTime fechaHoraFinUtc, final EstadoHorarioSesion estadoTemporal,
                                         final EstadoAsistenciaSesion estadoAsistencia,
                                         final String estadoSolicitudRevision, final boolean reclamable,
                                         final CancelacionSesionDTO cancelacion) {
        this(id, nombre, numero, codigo, numeroSemana, idGrupo, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin,
                fechaHoraInicioUtc, fechaHoraFinUtc, estadoTemporal, estadoAsistencia, estadoSolicitudRevision, reclamable,
                cancelacion, null, false);
    }
}
