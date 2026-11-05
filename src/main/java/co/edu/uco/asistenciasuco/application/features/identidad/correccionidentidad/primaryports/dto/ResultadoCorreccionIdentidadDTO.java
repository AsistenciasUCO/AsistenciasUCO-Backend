package co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto;

import java.util.UUID;

/**
 * Resultado de la correccion de identidad (HU159). {@code sincronizacionIdentidad} es OK o PENDIENTE: la base de datos
 * ya tiene el cambio; si el proveedor de identidad fallo, el desfase queda registrado y {@code mensajeUsuario} avisa.
 */
public record ResultadoCorreccionIdentidadDTO(UUID idRegistro, String sincronizacionIdentidad, String mensajeUsuario) {

    public static final String SINCRONIZACION_OK = "OK";
    public static final String SINCRONIZACION_PENDIENTE = "PENDIENTE";
}
