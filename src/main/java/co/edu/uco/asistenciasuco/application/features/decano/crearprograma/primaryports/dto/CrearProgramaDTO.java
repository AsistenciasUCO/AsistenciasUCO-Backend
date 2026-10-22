package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.dto;

import java.util.UUID;

public record CrearProgramaDTO(
        UUID idPrograma,
        Integer codigo,
        String nombre,
        UUID idCoordinador,
        UUID idTipoPrograma,
        UUID actorUsuarioId
) {
}
