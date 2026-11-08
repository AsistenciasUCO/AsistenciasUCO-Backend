package co.edu.uco.asistenciasuco.application.secondaryports.catalog;

/**
 * CULM-001 D.3 ([SUPUESTO-D-06]): indica si el catalogo de parametros activo del backend es la base de datos, unica fuente
 * en la que la edicion tiene efecto. Con otro proveedor (Azure App Configuration) editar en BD crearia dos verdades.
 */
public interface ParametroEdicionCapacidadPort {

    boolean admiteEdicion();
}
