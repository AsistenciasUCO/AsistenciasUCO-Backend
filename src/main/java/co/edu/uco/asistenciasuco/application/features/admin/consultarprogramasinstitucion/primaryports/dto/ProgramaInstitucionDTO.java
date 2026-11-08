package co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.dto;

import java.util.UUID;

/** Programa de la institucion del administrador (CULM-001 D.4). */
public record ProgramaInstitucionDTO(UUID id, String nombre, UUID idFacultad, String nombreFacultad) {
}
