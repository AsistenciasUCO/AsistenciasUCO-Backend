package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.usecase.domain;

import java.util.UUID;

public record GuardarTipoIdentificacionDomain(
        UUID id,
        String codigo,
        String nombre,
        UUID actorUsuarioId
) {
}
