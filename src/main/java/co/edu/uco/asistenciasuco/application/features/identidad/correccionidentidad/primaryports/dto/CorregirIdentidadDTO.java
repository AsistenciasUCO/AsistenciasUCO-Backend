package co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto;

import java.util.UUID;

/**
 * Solicitud de correccion de identidad (HU159). {@code tipoUsuario} es ESTUDIANTE o DECANO; el tipo y el numero de
 * documento son opcionales (nulos = no cambian); el motivo es obligatorio.
 */
public record CorregirIdentidadDTO(
        String tipoUsuario,
        UUID idRegistro,
        UUID idTipoIdentificacion,
        Integer numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String motivo,
        UUID actorUsuario
) {
}
