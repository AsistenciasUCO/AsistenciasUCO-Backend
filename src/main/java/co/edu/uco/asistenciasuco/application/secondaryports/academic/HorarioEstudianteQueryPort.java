package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioEstudianteProjection;

import java.util.List;
import java.util.UUID;

public interface HorarioEstudianteQueryPort {

    /** CULM-001 A.2-11: recibe el USUARIO ejecutor; el SP resuelve el estudiante y solo cuenta la matricula inscrita (A). */
    List<HorarioEstudianteProjection> consultarHorarioEstudiante(UUID usuarioEjecutor);
}
