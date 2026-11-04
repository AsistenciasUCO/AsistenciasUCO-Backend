package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.dto;

import java.util.UUID;

/** {@code id} nulo es un alta (codigo y nombre); con id se edita solo el nombre. */
public record GuardarTipoIdentificacionDTO(
        UUID id,
        String codigo,
        String nombre,
        UUID actorUsuarioId
) {
}
