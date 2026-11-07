package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain;

import java.util.List;

public record PaginaColaNotificacionProjection(
        List<NotificacionSalienteAdminProjection> elementos,
        int numeroPagina,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas,
        ResumenColaNotificacionProjection resumen
) {
    public PaginaColaNotificacionProjection {
        elementos = elementos == null ? List.of() : List.copyOf(elementos);
    }
}
