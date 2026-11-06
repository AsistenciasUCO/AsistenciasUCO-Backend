package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioReclamoProjection;

import java.util.List;
import java.util.UUID;

/**
 * Puerto secundario para consultar destinatarios de avisos de reclamo/revision de asistencia.
 */
public interface DestinatariosReclamoPort {

    List<DestinatarioReclamoProjection> consultarDestinatariosPorSesion(UUID idSesion, UUID idEstudiante);

    List<DestinatarioReclamoProjection> consultarDestinatariosPorSolicitud(UUID idSolicitud);
}
