package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record AsignaturaDocenteProjection(
        UUID idAsignatura,
        String nombreAsignatura,
        UUID idGrupo,
        String nombreGrupo,
        UUID idPrograma,
        String nombrePrograma,
        Integer estudiantesActivos
) {
}
