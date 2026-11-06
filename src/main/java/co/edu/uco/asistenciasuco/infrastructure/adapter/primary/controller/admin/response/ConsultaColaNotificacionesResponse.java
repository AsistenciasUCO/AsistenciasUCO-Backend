package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import java.util.List;

public record ConsultaColaNotificacionesResponse(
        List<NotificacionSalienteResponse> elementos,
        int numeroPagina,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas,
        ResumenColaNotificacionesResponse resumen
) {
    public ConsultaColaNotificacionesResponse {
        elementos = elementos == null ? List.of() : List.copyOf(elementos);
    }
}
