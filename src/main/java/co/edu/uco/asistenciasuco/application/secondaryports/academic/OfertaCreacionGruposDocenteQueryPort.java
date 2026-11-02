package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaOfertaDocenteProjection;
import java.util.List;
import java.util.UUID;

/** Asignaturas que el docente puede abrir como grupo (programas con vinculo activo), leidas de la DB. */
public interface OfertaCreacionGruposDocenteQueryPort {
    List<AsignaturaOfertaDocenteProjection> consultarOferta(UUID usuarioEjecutor);
}
