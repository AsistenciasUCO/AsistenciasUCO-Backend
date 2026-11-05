package co.edu.uco.asistenciasuco.application.secondaryports.identity.dto;

/**
 * Datos de una cuenta de identidad que se pueden actualizar de forma neutral. No incluye la contrasena (no existen
 * flujos de cambio en este programa).
 *
 * <p>HU159 (decision de John 2026-11-05): {@code username} es el documento de identidad corregido. Es nulo cuando el
 * documento no cambia; en ese caso el proveedor conserva el nombre de usuario actual.</p>
 *
 * @param correo         correo electronico vigente del usuario.
 * @param primerNombre   primer nombre.
 * @param primerApellido primer apellido.
 * @param username       nuevo nombre de usuario (documento corregido) o {@code null} si no cambia.
 */
public record ActualizarCuentaIdentidadDTO(
        String correo,
        String primerNombre,
        String primerApellido,
        String username
) {

    /** Actualizacion sin cambio de documento (el nombre de usuario se conserva). */
    public ActualizarCuentaIdentidadDTO(final String correo, final String primerNombre, final String primerApellido) {
        this(correo, primerNombre, primerApellido, null);
    }
}
