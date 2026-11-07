package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import co.edu.uco.asistenciasuco.application.features.festivos.consultarsesionesfestivo.primaryports.ConsultarSesionesFestivoConAsistenciaInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpUtcInstantCodec;

import java.util.UUID;

/** CULM-001 B.1 (§3.2.2): sesion en festivo que no se cancelo por tener asistencia registrada. */
public record SesionFestivoConAsistenciaResponse(
        UUID idSesion,
        UUID idGrupo,
        String nombreGrupo,
        String nombreAsignatura,
        String nombreDocente,
        String fechaHoraInicioUtc,
        String fecha,
        String nombreFestivo,
        int totalTomas,
        int totalRegistros
) {

    public static SesionFestivoConAsistenciaResponse from(
            final ConsultarSesionesFestivoConAsistenciaInputPort.SesionFestivo s) {
        return new SesionFestivoConAsistenciaResponse(s.idSesion(), s.idGrupo(), s.nombreGrupo(), s.nombreAsignatura(),
                s.nombreDocente(),
                s.fechaHoraInicioUtc() == null ? null : HttpUtcInstantCodec.fromUtcDatabaseDateTime(s.fechaHoraInicioUtc()),
                s.fecha() == null ? null : s.fecha().toString(), s.nombreFestivo(), s.totalTomas(), s.totalRegistros());
    }
}
