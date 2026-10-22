package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaProjection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramaQueryPort {

    List<ProgramaProjection> consultarProgramasPorFacultad(UUID idFacultad);

    Optional<ProgramaProjection> consultarProgramaPorId(UUID idPrograma);
}
