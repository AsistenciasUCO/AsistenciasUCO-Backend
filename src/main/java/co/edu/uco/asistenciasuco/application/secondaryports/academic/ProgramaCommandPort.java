package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface ProgramaCommandPort {

    void crearPrograma(CrearProgramaCommand command);

    record CrearProgramaCommand(
            UUID idPrograma,
            Integer codigo,
            String nombre,
            UUID idCoordinador,
            UUID idFacultad,
            UUID idTipoPrograma,
            UUID actorUsuarioId
    ) {
    }
}
