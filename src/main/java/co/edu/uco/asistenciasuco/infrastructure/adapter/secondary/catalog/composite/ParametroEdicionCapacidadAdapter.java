package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParametroEdicionCapacidadPort;

/**
 * CULM-001 D.3 ([SUPUESTO-D-06]): la edicion de parametros solo tiene efecto cuando el catalogo activo es SQL Server
 * ({@code app.adapters.parameter-catalog.provider=sqlserver}, DEC-018). Con Azure App Configuration la edicion en BD
 * crearia dos verdades y se responde 409 {@code PRM_010}.
 */
public final class ParametroEdicionCapacidadAdapter implements ParametroEdicionCapacidadPort {

    public static final String PROVEEDOR_SQLSERVER = "sqlserver";

    private final String proveedor;

    public ParametroEdicionCapacidadAdapter(final String proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public boolean admiteEdicion() {
        return proveedor != null && PROVEEDOR_SQLSERVER.equalsIgnoreCase(proveedor.strip());
    }
}
