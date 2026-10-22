package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface FacultadCommandPort {

    void crearFacultad(CrearFacultadCommand command);

    void actualizarFacultad(ActualizarFacultadCommand command);

    void cambiarEstadoFacultad(CambiarEstadoFacultadCommand command);

    record CrearFacultadCommand(
            UUID id,
            String nombre,
            UUID idInstitucion,
            UUID idDecano,
            UUID actorUsuarioId
    ) {
    }

    record ActualizarFacultadCommand(
            UUID id,
            String nombre,
            UUID idDecano,
            UUID actorUsuarioId
    ) {
    }

    record CambiarEstadoFacultadCommand(
            UUID id,
            boolean activa,
            UUID actorUsuarioId
    ) {
    }
}
