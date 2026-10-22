package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Detalle de la solicitud de revision visible para el docente titular (instantes UTC; nulos sin procedencia verificable). */
public record DetalleReclamoDocenteDomain(
        UUID idSolicitud,
        String nombreSolicitud,
        LocalDate fechaSolicitud,
        String codigoEstado,
        String nombreEstado,
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
        Boolean asistio,
        String codigoRazonCausa,
        String nombreRazonCausa,
        UUID idArchivoSoporte,
        String nombreSoporte,
        String tipoContenidoSoporte,
        Long tamanioSoporteBytes,
        Instant fechaRegistroSoporteUtc
) {
}
