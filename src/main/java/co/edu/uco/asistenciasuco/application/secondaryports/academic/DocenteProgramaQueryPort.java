package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;

import java.util.List;
import java.util.UUID;

public interface DocenteProgramaQueryPort {

    List<DocenteProgramaProjection> consultarDocentesPorPrograma(UUID idPrograma);

    default List<DocenteProgramaProjection> consultarDocentesDisponibles() {
        return List.of();
    }
}
