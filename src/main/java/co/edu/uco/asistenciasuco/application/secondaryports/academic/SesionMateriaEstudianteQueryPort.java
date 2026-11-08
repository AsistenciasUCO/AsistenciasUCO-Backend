package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;

import java.util.UUID;

public interface SesionMateriaEstudianteQueryPort {

    /**
     * Sesiones de la asignatura del estudiante autenticado (CULM-001 D.1): el ambito (matricula inscrita del usuario),
     * la paginacion y la fecha limite de justificacion se resuelven dentro del procedimiento almacenado.
     */
    PaginaResultado<SesionMateriaEstudianteProjection> consultarSesionesMateria(UUID usuarioEjecutor, UUID idAsignatura,
                                                                                int numeroPagina, int tamanoPagina);
}
