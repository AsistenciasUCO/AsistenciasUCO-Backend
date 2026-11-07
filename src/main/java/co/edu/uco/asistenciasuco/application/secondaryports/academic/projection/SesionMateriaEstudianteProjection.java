package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sesion de una materia del estudiante (CULM-001 D.1, HU026B). {@code fechaLimiteJustificacionUtc} es el limite exclusivo
 * (UTC, sin zona) que calcula la base de datos o nulo si faltan sus parametros; {@code plazoJustificacionVigente} es falso
 * para una sesion cancelada o cuyo plazo ya vencio. Java no recalcula ninguno de los dos.
 */
public record SesionMateriaEstudianteProjection(
        UUID id,
        String nombre,
        Integer numero,
        String codigo,
        Integer numeroSemana,
        UUID idGrupo,
        String codigoGrupo,
        String nombreGrupo,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        LocalDateTime fechaLimiteJustificacionUtc,
        boolean plazoJustificacionVigente
) {

    /** Sesion sin informacion de plazo (nulo y no vigente). */
    public SesionMateriaEstudianteProjection(final UUID id, final String nombre, final Integer numero, final String codigo,
                                             final Integer numeroSemana, final UUID idGrupo, final String codigoGrupo,
                                             final String nombreGrupo, final LocalDateTime fechaHoraInicio,
                                             final LocalDateTime fechaHoraFin) {
        this(id, nombre, numero, codigo, numeroSemana, idGrupo, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin,
                null, false);
    }
}
