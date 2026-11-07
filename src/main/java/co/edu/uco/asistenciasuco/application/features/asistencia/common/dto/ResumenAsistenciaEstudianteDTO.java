package co.edu.uco.asistenciasuco.application.features.asistencia.common.dto;

import java.util.UUID;

/** CULM-001 B.2 (F-DOC-004): fila de la sabana del docente con los indicadores por horas de un estudiante del grupo. */
public record ResumenAsistenciaEstudianteDTO(
        UUID estudianteId,
        String nombreCompleto,
        String codigoEstadoEstudiante,
        IndicadoresAsistenciaDTO indicadores
) {
}
