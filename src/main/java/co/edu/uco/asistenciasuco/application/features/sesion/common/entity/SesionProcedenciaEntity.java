package co.edu.uco.asistenciasuco.application.features.sesion.common.entity;

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
        String procedenciaTemporal
) {

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
        final EstadoHorarioSesion estado = EstadoHorarioSesion.desdeProcedencia(procedenciaTemporal);
        final boolean confirmada = estado == EstadoHorarioSesion.CONFIRMADA;
        return new SesionProcedenciaEntity(
                sesion, grupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo,
                confirmada ? fechaHoraInicio : null,
                confirmada ? fechaHoraFin : null,
                estado,
                confirmada ? procedenciaTemporal : null
        );
    }
}
