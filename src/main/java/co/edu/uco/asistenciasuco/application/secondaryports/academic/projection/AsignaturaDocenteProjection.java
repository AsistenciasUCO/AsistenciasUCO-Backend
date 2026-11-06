package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record AsignaturaDocenteProjection(
        UUID idAsignatura,
        String nombreAsignatura,
        UUID idGrupo,
        String nombreGrupo,
        UUID idPrograma,
        String nombrePrograma,
        UUID idTipoPrograma,
        String nombreTipoPrograma,
        Integer estudiantesActivos,
        /** Cupo máximo del grupo (uv_grupo); nulo si no se conoce. */
        Integer capacidadMaximaPermitida
) {
    /** Compatibilidad: sin capacidad informada. */
    public AsignaturaDocenteProjection(final UUID idAsignatura, final String nombreAsignatura, final UUID idGrupo, final String nombreGrupo, final UUID idPrograma, final String nombrePrograma, final UUID idTipoPrograma, final String nombreTipoPrograma, final Integer estudiantesActivos) {
        this(idAsignatura, nombreAsignatura, idGrupo, nombreGrupo, idPrograma, nombrePrograma, idTipoPrograma, nombreTipoPrograma, estudiantesActivos, null);
    }
}
