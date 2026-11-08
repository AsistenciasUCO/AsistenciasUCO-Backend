package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Programa activo de la institucion del administrador (CULM-001 D.4). */
public record ProgramaInstitucionProjection(UUID id, String nombre, UUID idFacultad, String nombreFacultad) {
}
