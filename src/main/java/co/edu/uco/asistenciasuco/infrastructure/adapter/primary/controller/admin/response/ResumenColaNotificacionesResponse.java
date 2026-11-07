package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

public record ResumenColaNotificacionesResponse(
        long pendientes,
        long reservadas,
        long entregadas,
        long fallidasReintentables,
        long fallidasDefinitivas
) {
}
