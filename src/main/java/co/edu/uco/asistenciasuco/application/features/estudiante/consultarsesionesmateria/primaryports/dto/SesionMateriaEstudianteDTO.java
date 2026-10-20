package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto;

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
                                         String estadoAsistencia, String estadoSolicitudRevision, boolean reclamable) {
}
