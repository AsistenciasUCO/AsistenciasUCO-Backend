package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog;

/**
 * CULM-001 D.3: proveedor de parametros con cache en memoria que puede invalidarse (Azure App Configuration y
 * {@code sqlserver}). Tras editar un parametro, {@code CatalogInvalidationPort} lo invalida para que la siguiente lectura
 * vea el valor nuevo.
 */
public interface InvalidableParameterCatalog {

    void invalidateParameter(String group, String key);

    void invalidateAll();
}
