package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface FacultadCommandPort {

    void crearFacultad(CrearFacultadCommand command);

    record CrearFacultadCommand(
            UUID id,
            String nombre,
            UUID idInstitucion,
            UUID idDecano,
            UUID actorUsuarioId
    ) {
    }
}
