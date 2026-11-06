package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain;

import java.time.Instant;
import java.util.UUID;

public record NotificacionSalienteAdminProjection(
        UUID id,
        String tipo,
        String asunto,
        String codigoEstado,
        String nombreEstado,
        boolean esTerminal,
        int cantidadDestinatarios,
        int intentos,
        Instant creadoEn,
        Instant proximoIntentoEn,
        Instant entregadoEn,
        String codigoError,
        String detalleError,
        UUID referenciaTraza
) {
}
