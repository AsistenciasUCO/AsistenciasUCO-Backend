package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DetalleSolicitudRevisionDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionDocenteProjection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Consulta de las solicitudes de revision del docente titular (LB-004D, R2 y detalle HU036). La titularidad se
 * resuelve en SQL por {@code Docente.usuario = usuarioEjecutor}: otro docente obtiene cero filas.
 */
public interface SolicitudRevisionDocenteQueryPort {

    List<SolicitudRevisionDocenteProjection> consultarSolicitudesDocente(
            UUID usuarioEjecutor,
            String codigoEstado,
            int numeroPagina,
            int tamanoPagina
    );

    /** Detalle con causal de una solicitud; vacio si no existe o no pertenece a un grupo del docente (indistinguible). */
    Optional<DetalleSolicitudRevisionDocenteProjection> consultarDetalleSolicitudDocente(
            UUID idSolicitud,
            UUID usuarioEjecutor
    );
}
