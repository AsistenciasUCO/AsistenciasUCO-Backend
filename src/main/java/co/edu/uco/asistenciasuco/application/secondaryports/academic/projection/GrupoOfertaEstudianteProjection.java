package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Grupo de la oferta del estudiante (HU008). estadoPeriodo: VIGENTE o PROXIMO. matriculado: solo la matricula inscrita
 * cuenta. codigoComponente: ESP (propia del programa), BAS o INS (transversales).
 */
public record GrupoOfertaEstudianteProjection(
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String codigoAsignatura,
        String nombreAsignatura,
        String nombreDocente,
        UUID idPeriodoAcademico,
        String nombrePeriodoAcademico,
        LocalDate fechaInicioPeriodo,
        LocalDate fechaFinPeriodo,
        String estadoPeriodo,
        String horario,
        int capacidadMaxima,
        int estudiantesActivos,
        int cuposDisponibles,
        boolean matriculado,
        String codigoComponente
) {
}
