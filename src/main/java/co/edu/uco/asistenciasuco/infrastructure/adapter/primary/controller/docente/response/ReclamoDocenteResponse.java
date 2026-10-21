package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Representacion HTTP de una solicitud de revision para el docente titular (LB-004D). Los instantes son cadenas UTC
 * ISO-8601 con {@code Z} y hasta siete decimales (misma codificacion que el contrato UTC v2 de sesiones); {@code null} si
 * el dato historico no tiene procedencia verificable. {@code fechaSolicitud} es una fecha de calendario ({@code yyyy-MM-dd}).
 */
public record ReclamoDocenteResponse(
        UUID id,
        String nombre,
        LocalDate fechaSolicitud,
        String estadoSolicitud,
        String justificacionSolicitud,
        String justificacionRespuesta,
        UUID estudianteId,
        String estudianteNombre,
        UUID sesionId,
        Integer sesionNumero,
        String fechaSesionUtc,
        String procedenciaTemporalSesion,
        UUID grupoId,
        Integer grupoCodigo,
        String grupoNombre,
        UUID materiaId,
        String materiaCodigo,
        String materiaNombre,
        SoporteAdjuntoResponse soporteAdjunto
) {

    /** Soporte vinculado: se descarga por {@code GET /api/v1/archivos/{fileId}} autenticado, nunca por el storage. */
    public record SoporteAdjuntoResponse(
            UUID fileId,
            String nombre,
            String tipo,
            long tamanioBytes,
            String fechaSubidaUtc,
            String url
    ) {
    }
}
