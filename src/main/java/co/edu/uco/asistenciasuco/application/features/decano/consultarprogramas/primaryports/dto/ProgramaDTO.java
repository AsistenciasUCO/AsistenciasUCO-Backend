package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto;

import java.util.UUID;

public record ProgramaDTO(
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
