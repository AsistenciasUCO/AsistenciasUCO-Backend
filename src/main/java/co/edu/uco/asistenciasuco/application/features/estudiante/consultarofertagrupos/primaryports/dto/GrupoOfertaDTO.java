package co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto;

import java.time.LocalDate;
import java.util.UUID;

public record GrupoOfertaDTO(
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
