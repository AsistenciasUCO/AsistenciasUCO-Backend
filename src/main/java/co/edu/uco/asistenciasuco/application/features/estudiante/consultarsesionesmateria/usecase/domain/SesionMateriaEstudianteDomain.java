package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de sesiones de la materia del estudiante.
 * {@code estadoSolicitudRevision} es el nombre del estado de la solicitud vigente (nulo si no hay) y
 * {@code reclamable} es solo una pista de presentacion: la autorizacion definitiva es del backend/DB al radicar.
 */
public record SesionMateriaEstudianteDomain(UUID id, String nombre, Integer numero, String codigo, Integer numeroSemana,
                                            UUID idGrupo, String codigoGrupo, String nombreGrupo,
                                            LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin,
                                            EstadoAsistenciaSesion estadoAsistencia, String estadoSolicitudRevision,
                                            boolean reclamable) {
}
