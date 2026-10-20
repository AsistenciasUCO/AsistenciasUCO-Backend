package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Solicitud de revision visible para el docente titular (los instantes son UTC; nulos si no hay procedencia verificable). */
public record ReclamoDocenteDomain(
        UUID idSolicitud,
        String nombreSolicitud,
        LocalDate fechaSolicitud,
        String codigoEstado,
        String justificacionSolicitud,
        String justificacionRespuesta,
        UUID idEstudiante,
        String nombreEstudiante,
        UUID idSesion,
        Integer numeroSesion,
        Instant fechaHoraInicioSesionUtc,
        String procedenciaTemporalSesion,
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String codigoAsignatura,
        String nombreAsignatura,
        UUID idArchivoSoporte,
        String nombreSoporte,
        String tipoContenidoSoporte,
        Long tamanioSoporteBytes,
        Instant fechaRegistroSoporteUtc
) {
}
