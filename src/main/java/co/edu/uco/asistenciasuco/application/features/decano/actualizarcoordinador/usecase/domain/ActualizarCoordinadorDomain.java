package co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.domain;

import java.util.UUID;

/**
 * Dominio de la operacion editar coordinador (HU121). HU159 (2026-11-05): tipo y numero de documento opcionales, con
 * motivo si cambian.
 */
public record ActualizarCoordinadorDomain(
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
    public ActualizarCoordinadorDomain(final UUID idCoordinador, final String primerNombre, final String segundoNombre,
            final String primerApellido, final String segundoApellido, final String correo, final UUID usuario) {
        this(idCoordinador, primerNombre, segundoNombre, primerApellido, segundoApellido, correo, usuario, null, null, null);
    }
}
