package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

public record SolicitarRevisionAsistenciaRepositoryDTO(
        UUID estudiante,
        UUID sesion,
        String categoria,
        String justificacion,
        UUID soporteArchivoId,
        UUID usuarioEjecutor
) {
}
