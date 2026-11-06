package co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto;

import java.util.UUID;

public record AsignaturaDocenteDTO(UUID idAsignatura, String nombreAsignatura, UUID idGrupo,
                                   String nombreGrupo, UUID idPrograma, String nombrePrograma,
                                   UUID idTipoPrograma, String nombreTipoPrograma, Integer estudiantesActivos,
                                   Integer capacidadMaximaPermitida) {
    /** Compatibilidad: sin capacidad informada. */
    public AsignaturaDocenteDTO(final UUID idAsignatura, final String nombreAsignatura, final UUID idGrupo, final String nombreGrupo, final UUID idPrograma, final String nombrePrograma, final UUID idTipoPrograma, final String nombreTipoPrograma, final Integer estudiantesActivos) {
        this(idAsignatura, nombreAsignatura, idGrupo, nombreGrupo, idPrograma, nombrePrograma, idTipoPrograma, nombreTipoPrograma, estudiantesActivos, null);
    }
}
