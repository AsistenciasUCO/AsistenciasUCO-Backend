package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;

import java.util.List;
import java.util.UUID;

public interface CoordinadorQueryPort {

    List<CoordinadorProjection> consultarCoordinadoresPorFacultad(UUID idFacultad);

    default List<CoordinadorProjection> consultarCoordinadoresSinPrograma() {
        return List.of();
    }
}
