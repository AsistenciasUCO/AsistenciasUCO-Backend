package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain;

public record ResumenColaNotificacionProjection(
        long pendientes,
        long reservadas,
        long entregadas,
        long fallidasReintentables,
        long fallidasDefinitivas
) {
}
