package co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.dto;

import java.util.UUID;

public record ActualizarCoordinadorDTO(
        UUID idCoordinador,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        UUID usuario,
        UUID idTipoIdentificacion,
        Integer numeroIdentificacion,
        String motivo
) {

    /** Edicion sin correccion de documento (contrato original de HU121). */
    public ActualizarCoordinadorDTO(final UUID idCoordinador, final String primerNombre, final String segundoNombre,
            final String primerApellido, final String segundoApellido, final String correo, final UUID usuario) {
        this(idCoordinador, primerNombre, segundoNombre, primerApellido, segundoApellido, correo, usuario, null, null, null);
    }
}
