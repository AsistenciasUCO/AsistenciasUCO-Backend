package co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.UUID;

/**
 * Datos para asegurar la cuenta de identidad de un usuario ya confirmado en la base de datos. {@code passwordInicial} es la
 * clave en claro que solo se entrega al proveedor de identidad al crear una cuenta nueva; nunca se imprime.
 */
public record SolicitudCuentaIdentidad(
        UUID idUsuario,
        String username,
        String correo,
        String primerNombre,
        String primerApellido,
        String passwordInicial,
        InstitutionalRole rol,
        boolean requiereVerificacionCorreo
) {

    @Override
    public String toString() {
        return "SolicitudCuentaIdentidad[idUsuario=" + idUsuario + ", rol=" + rol + ", requiereVerificacionCorreo="
                + requiereVerificacionCorreo + "]";
    }
}
