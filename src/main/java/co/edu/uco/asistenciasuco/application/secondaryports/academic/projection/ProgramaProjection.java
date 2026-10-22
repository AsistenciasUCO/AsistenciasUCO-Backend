package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record ProgramaProjection(
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
