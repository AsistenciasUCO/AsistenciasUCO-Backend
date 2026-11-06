package co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto;

/**
 * HU159: si la correccion del tipo y numero de documento esta habilitada (parametro
 * IDENTIDAD.PERMITIR_CORREGIR_DOCUMENTO). Con {@code false} el frontend oculta esos campos; la base de datos la
 * rechaza de todas formas (IDE_003).
 */
public record ConfiguracionCorreccionIdentidadDTO(boolean permitirCorregirDocumento) {
}
