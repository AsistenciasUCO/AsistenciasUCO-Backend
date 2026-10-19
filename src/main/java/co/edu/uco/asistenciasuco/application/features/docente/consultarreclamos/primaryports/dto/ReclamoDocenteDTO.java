package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Solicitud de revision para el docente titular. {@code fechaSesionUtc} es un instante UTC (nulo si la hora
 * historica de la sesion no tiene procedencia verificable); el cliente la presenta en la zona IANA del usuario.
 */
public record ReclamoDocenteDTO(
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
        Instant fechaSesionUtc,
        String procedenciaTemporalSesion,
        UUID grupoId,
        Integer grupoCodigo,
        String grupoNombre,
        UUID materiaId,
        String materiaCodigo,
        String materiaNombre,
        SoporteAdjuntoDTO soporteAdjunto
) {

    /** Soporte vinculado: se descarga SIEMPRE por el backend autenticado ({@code url} relativa), nunca por el storage. */
    public record SoporteAdjuntoDTO(
            UUID fileId,
            String nombre,
            String tipo,
            long tamanioBytes,
            Instant fechaSubidaUtc,
            String url
    ) {
    }
}
