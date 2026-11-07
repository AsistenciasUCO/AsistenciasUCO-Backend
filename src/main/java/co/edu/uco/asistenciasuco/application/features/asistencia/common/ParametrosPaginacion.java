package co.edu.uco.asistenciasuco.application.features.asistencia.common;

/**
 * CULM-001 B.2: valores por defecto de la paginacion de las consultas de tomas y resumenes (contrato §3.2.5 y §3.2.8:
 * {@code pagina=1&tamano=50}). Los limites finales los valida el SP ({@code PAG_001}).
 */
public final class ParametrosPaginacion {

    public static final int PAGINA_POR_DEFECTO = 1;
    public static final int TAMANO_POR_DEFECTO = 50;

    private ParametrosPaginacion() {
    }

    public static int pagina(final Integer pagina) {
        return pagina == null ? PAGINA_POR_DEFECTO : pagina;
    }

    public static int tamano(final Integer tamano) {
        return tamano == null ? TAMANO_POR_DEFECTO : tamano;
    }
}
