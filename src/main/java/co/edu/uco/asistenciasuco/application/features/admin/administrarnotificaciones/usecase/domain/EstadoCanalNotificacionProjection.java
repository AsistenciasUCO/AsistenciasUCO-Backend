package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain;

public record EstadoCanalNotificacionProjection(
        String provider,
        String sender,
        boolean dispatcherEnabled,
        boolean disponible,
        String host,
        int port,
        String username,
        boolean tienePasswordConfigurado,
        String fromAddress,
        String fromName,
        boolean starttls,
        boolean ssl,
        int maxBcc,
        int ratePerMinute,
        int ratePerDay,
        boolean requireConfirmedEmail,
        String platformUrl,
        boolean testModeEnabled,
        int testModeRedirectCount
) {
}
