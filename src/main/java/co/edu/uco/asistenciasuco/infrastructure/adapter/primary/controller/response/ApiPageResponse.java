package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import java.util.List;

/**
 * Respuesta paginada estandar (AGENTS.md §2.3; CULM-001 B §3.1): elementos de la pagina y metadatos de paginacion.
 *
 * @param numeroPagina pagina solicitada (base 1)
 * @param tamanoPagina tamano de pagina aplicado
 * @param totalElementos total de filas que cumplen el filtro (no solo las de la pagina)
 * @param totalPaginas  {@code ceil(totalElementos / tamanoPagina)}; 0 si no hay elementos
 */
public record ApiPageResponse<T>(
        boolean exitoso,
        List<T> elementos,
        int numeroPagina,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas
) {

    public ApiPageResponse {
        elementos = elementos == null ? List.of() : List.copyOf(elementos);
    }

    public static <T> ApiPageResponse<T> of(final List<T> elementos, final int numeroPagina, final int tamanoPagina,
                                            final long totalElementos) {
        final int paginas = tamanoPagina <= 0 || totalElementos <= 0
                ? 0 : (int) ((totalElementos + tamanoPagina - 1) / tamanoPagina);
        return new ApiPageResponse<>(true, elementos, numeroPagina, tamanoPagina, totalElementos, paginas);
    }
}
