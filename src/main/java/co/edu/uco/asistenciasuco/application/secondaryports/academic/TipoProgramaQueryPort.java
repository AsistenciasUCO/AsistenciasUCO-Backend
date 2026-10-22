package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.TipoProgramaProjection;

import java.util.List;

public interface TipoProgramaQueryPort {

    List<TipoProgramaProjection> consultarTiposPrograma();
}
