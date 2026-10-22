package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain;

import java.util.UUID;

public record ProgramaDomain(
        UUID id,
        String nombrePrograma,
        UUID idFacultad,
        String nombreFacultad,
        UUID idInstitucion,
        String nombreInstitucion,
        UUID idCoordinador,
        String nombreCoordinador,
        boolean estaActivoPrograma,
        String estaActivoTextoPrograma,
        UUID idTipoPrograma,
        String nombreTipoPrograma
) {
}
