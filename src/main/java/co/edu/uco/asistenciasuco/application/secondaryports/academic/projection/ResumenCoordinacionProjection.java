package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/**
 * Certificacion F3 (tarjeta del coordinador): por programa de la facultad, su facultad, los docentes con vinculacion
 * activa ({@code uv_docente_programa.estaActivoDocentePrograma = 1}) y los grupos habilitados ({@code uv_grupo_programa}).
 */
public record ResumenCoordinacionProjection(
        UUID idPrograma,
        String nombreFacultad,
        int totalDocentes,
        int totalGrupos
) {
}
