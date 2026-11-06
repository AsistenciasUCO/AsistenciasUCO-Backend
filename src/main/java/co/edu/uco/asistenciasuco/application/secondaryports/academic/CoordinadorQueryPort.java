package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenCoordinacionProjection;

import java.util.List;
import java.util.UUID;

public interface CoordinadorQueryPort {

    List<CoordinadorProjection> consultarCoordinadoresPorFacultad(UUID idFacultad);

    /** Coordinadores aun sin programa de la facultad indicada (HU121 D2: ya no se mezclan las de otras facultades). */
    default List<CoordinadorProjection> consultarCoordinadoresSinPrograma(final UUID idFacultad) {
        return List.of();
    }

    /** Certificacion F3: facultad y conteos de docentes y grupos de cada programa de la facultad (aditivo). */
    default List<ResumenCoordinacionProjection> consultarResumenProgramasFacultad(final UUID idFacultad) {
        return List.of();
    }
}
