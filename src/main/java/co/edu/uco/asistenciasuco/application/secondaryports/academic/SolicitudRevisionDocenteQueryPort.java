package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionDocenteProjection;

import java.util.List;
import java.util.UUID;

/**
 * Consulta paginada de las solicitudes de revision del docente titular (LB-004D, R2). La titularidad se
 * resuelve en SQL por {@code Docente.usuario = usuarioEjecutor}: otro docente obtiene cero filas.
 */
public interface SolicitudRevisionDocenteQueryPort {

    List<SolicitudRevisionDocenteProjection> consultarSolicitudesDocente(
            UUID usuarioEjecutor,
            String codigoEstado,
            int numeroPagina,
            int tamanoPagina
    );
}
