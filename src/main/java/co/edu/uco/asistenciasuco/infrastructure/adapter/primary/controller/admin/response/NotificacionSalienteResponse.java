package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import java.time.Instant;
import java.util.UUID;

public record NotificacionSalienteResponse(
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
        UUID idCorrelacion
) {
}
