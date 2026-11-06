package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/**
 * {@code totalSemestres} es el mayor numero de semestre del plan (0 si no tiene semestres; HU082/HU083).
 * {@code totalAsignaturas}/{@code totalCreditos}: asignaturas de la malla y suma de sus creditos, con la misma regla
 * que {@code usp_consultar_programas_creditos_facultad_paginado} (certificacion F3).
 */
public record PlanEstudioProjection(
        UUID id,
        UUID idPrograma,
        String nombrePrograma,
        String inp,
        boolean estaActivoPlanEstudio,
        String estaActivoTextoPlanEstudio,
        String justificacionEstado,
        int totalSemestres,
        int totalAsignaturas,
        int totalCreditos
) {

    /** Compatibilidad: sin totales de la malla (certificacion F3). */
    public PlanEstudioProjection(final UUID id, final UUID idPrograma, final String nombrePrograma, final String inp,
            final boolean estaActivoPlanEstudio, final String estaActivoTextoPlanEstudio,
            final String justificacionEstado, final int totalSemestres) {
        this(id, idPrograma, nombrePrograma, inp, estaActivoPlanEstudio, estaActivoTextoPlanEstudio,
                justificacionEstado, totalSemestres, 0, 0);
    }

    public PlanEstudioProjection(final UUID id, final UUID idPrograma, final String nombrePrograma, final String inp,
                                 final boolean estaActivoPlanEstudio, final String estaActivoTextoPlanEstudio,
                                 final String justificacionEstado) {
        this(id, idPrograma, nombrePrograma, inp, estaActivoPlanEstudio, estaActivoTextoPlanEstudio,
                justificacionEstado, 0);
    }
}
