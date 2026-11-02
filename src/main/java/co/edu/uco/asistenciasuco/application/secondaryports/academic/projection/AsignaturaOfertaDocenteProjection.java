package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record AsignaturaOfertaDocenteProjection(
        UUID idAsignatura,
        String codigoAsignatura,
        String nombreAsignatura,
        UUID idPrograma,
        String nombrePrograma
) {
}
