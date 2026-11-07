package co.edu.uco.asistenciasuco.application.features.sesion.common.entity;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sesion consultada por el contrato v2.
 *
 * <p>Regla UTC-D06/D07: solo una fila CONFIRMADA expone sus horas como instantes UTC. Una fila
 * INDETERMINADA conserva identificacion y datos no temporales, pero sus horas quedan en null y su
 * procedencia en null: nunca se ofrece un horario que no se puede probar.</p>
 */
public record SesionProcedenciaEntity(
        UUID sesion,
        UUID grupo,
        String nombre,
        Integer numero,
        String codigo,
        Integer numeroSemana,
        String codigoGrupo,
        String nombreGrupo,
        LocalDateTime fechaHoraInicioUtc,
        LocalDateTime fechaHoraFinUtc,
        EstadoHorarioSesion estadoTemporal,
        String procedenciaTemporal,
        CancelacionSesionDTO cancelacion
) {

    /** Sesion programada (sin cancelacion). */
    public SesionProcedenciaEntity(
            final UUID sesion,
            final UUID grupo,
            final String nombre,
            final Integer numero,
            final String codigo,
            final Integer numeroSemana,
            final String codigoGrupo,
            final String nombreGrupo,
            final LocalDateTime fechaHoraInicioUtc,
            final LocalDateTime fechaHoraFinUtc,
            final EstadoHorarioSesion estadoTemporal,
            final String procedenciaTemporal
    ) {
        this(sesion, grupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo, fechaHoraInicioUtc,
                fechaHoraFinUtc, estadoTemporal, procedenciaTemporal, null);
    }

    /** CULM-001 B.1 (§3.2.4): {@code PROGRAMADA} o {@code CANCELADA}. */
    public EstadoSesion estado() {
        return EstadoSesion.de(cancelacion != null);
    }

    public static SesionProcedenciaEntity desdeAlmacenamiento(
            final UUID sesion,
            final UUID grupo,
            final String nombre,
            final Integer numero,
            final String codigo,
            final Integer numeroSemana,
            final String codigoGrupo,
            final String nombreGrupo,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final String procedenciaTemporal
    ) {
        return desdeAlmacenamiento(sesion, grupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo,
                fechaHoraInicio, fechaHoraFin, procedenciaTemporal, null);
    }

    public static SesionProcedenciaEntity desdeAlmacenamiento(
            final UUID sesion,
            final UUID grupo,
            final String nombre,
            final Integer numero,
            final String codigo,
            final Integer numeroSemana,
            final String codigoGrupo,
            final String nombreGrupo,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final String procedenciaTemporal,
            final CancelacionSesionDTO cancelacion
    ) {
        final EstadoHorarioSesion estado = EstadoHorarioSesion.desdeProcedencia(procedenciaTemporal);
        final boolean confirmada = estado == EstadoHorarioSesion.CONFIRMADA;
        return new SesionProcedenciaEntity(
                sesion, grupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo,
                confirmada ? fechaHoraInicio : null,
                confirmada ? fechaHoraFin : null,
                estado,
                confirmada ? procedenciaTemporal : null,
                cancelacion
        );
    }
}
