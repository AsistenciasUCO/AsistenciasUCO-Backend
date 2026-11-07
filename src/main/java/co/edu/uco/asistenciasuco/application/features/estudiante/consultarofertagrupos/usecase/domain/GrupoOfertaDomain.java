package co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * tipoAsignatura: ESPECIFICA (propia del programa), BASICA o INSTITUCIONAL (transversales, {@code transversal = true}).
 */
public record GrupoOfertaDomain(
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
        String tipoAsignatura,
        boolean transversal
) {
}
