package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface AreaCommandPort {

    void crearArea(CrearAreaCommand command);

    void actualizarArea(ActualizarAreaCommand command);

    record CrearAreaCommand(
            UUID id,
            String codigo,
            String nombre,
            UUID actorUsuarioId
    ) {
    }

    record ActualizarAreaCommand(
            UUID id,
            String codigo,
            String nombre,
            UUID actorUsuarioId
    ) {
    }
}
