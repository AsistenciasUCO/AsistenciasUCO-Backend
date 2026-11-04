package co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de asignaturas del docente.
 */
public record AsignaturaDocenteDomain(UUID idAsignatura, String nombreAsignatura, UUID idGrupo,
                                      String nombreGrupo, UUID idPrograma, String nombrePrograma,
                                      UUID idTipoPrograma, String nombreTipoPrograma, Integer estudiantesActivos) {
}
