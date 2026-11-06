package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/**
 * Destinatario del aviso de reclamo de asistencia con contexto del grupo y sesion.
 */
public record DestinatarioReclamoProjection(
        String rol,
        UUID idUsuario,
        String nombreCompleto,
        String correo,
        UUID idGrupo,
        String nombreGrupo,
        String fechaSesion
) {
}
