package co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto;

import java.util.UUID;

public record AsignaturaDocenteDTO(UUID idAsignatura, String nombreAsignatura, UUID idGrupo,
                                   String nombreGrupo, UUID idPrograma, String nombrePrograma,
                                   UUID idTipoPrograma, String nombreTipoPrograma, Integer estudiantesActivos) {
}
