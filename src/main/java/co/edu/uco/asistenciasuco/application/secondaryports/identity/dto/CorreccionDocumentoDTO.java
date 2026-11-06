package co.edu.uco.asistenciasuco.application.secondaryports.identity.dto;

import java.util.UUID;

/**
 * HU159 (decision de John 2026-11-05): correccion opcional del tipo y numero de documento de un usuario, con su motivo.
 * Aislada a proposito: la valida y aplica un unico interno de la base de datos (apagable con el parametro
 * IDENTIDAD.PERMITIR_CORREGIR_DOCUMENTO) y el nombre de usuario del proveedor de identidad se sincroniza solo si cambia.
 *
 * @param idTipoIdentificacion tipo de documento corregido o {@code null} si no cambia.
 * @param numeroIdentificacion numero de documento corregido o {@code null} si no cambia.
 * @param motivo               motivo de la correccion (obligatorio en la DB cuando el documento cambia).
 */
public record CorreccionDocumentoDTO(
        UUID idTipoIdentificacion,
        Integer numeroIdentificacion,
        String motivo
) {

    private static final CorreccionDocumentoDTO NINGUNA = new CorreccionDocumentoDTO(null, null, null);

    /** Sin correccion de documento. */
    public static CorreccionDocumentoDTO ninguna() {
        return NINGUNA;
    }

    /** {@code true} si se pide corregir el tipo o el numero de documento. */
    public boolean solicitaCambio() {
        return idTipoIdentificacion != null || numeroIdentificacion != null;
    }

    /** Nuevo nombre de usuario del proveedor de identidad (el numero de documento) o {@code null} si no cambia. */
    public String nuevoUsername() {
        return numeroIdentificacion == null ? null : String.valueOf(numeroIdentificacion);
    }
}
