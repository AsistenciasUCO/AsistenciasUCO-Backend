package co.edu.uco.asistenciasuco.application.features.asistencia.common;

import java.util.List;

/**
 * CULM-001 B.2: pagina de una consulta paginada en SQL ({@code OFFSET/FETCH} + {@code COUNT(*) OVER()}).
 *
 * @param numeroPagina   pagina solicitada (base 1)
 * @param tamanoPagina   tamano de pagina aplicado
 * @param totalElementos total de filas que cumplen el filtro
 */
public record PaginaResultado<T>(List<T> elementos, int numeroPagina, int tamanoPagina, long totalElementos) {

    public PaginaResultado {
        elementos = elementos == null ? List.of() : List.copyOf(elementos);
    }
}
