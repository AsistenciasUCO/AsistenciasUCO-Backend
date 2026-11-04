package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.util.UUID;

/** HU153: alta y edicion del nombre de un tipo de documento (usp_guardar_tipo_identificacion). */
public interface TipoIdentificacionCommandPort {

    void guardarTipoIdentificacion(GuardarTipoIdentificacionCommand command);

    /** {@code id} nulo es un alta; con id solo cambia el nombre (el codigo es inmutable). */
    record GuardarTipoIdentificacionCommand(UUID id, String codigo, String nombre, UUID actorUsuarioId) {
    }
}
