package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto;

import java.util.UUID;

/** CULM-001 A.2-1: el detalle del directorio se consulta con el ejecutor autenticado (ambito en el SP). */
public record ConsultarEstudiantePorIdDTO(UUID estudianteId, UUID actorUsuarioId) {
}
