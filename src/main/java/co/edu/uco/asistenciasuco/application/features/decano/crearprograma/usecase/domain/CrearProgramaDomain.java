package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.domain;

import java.util.UUID;

public record CrearProgramaDomain(
        UUID idPrograma,
        Integer codigo,
        String nombre,
        UUID idCoordinador,
        UUID idTipoPrograma,
        UUID actorUsuarioId
) {
}
