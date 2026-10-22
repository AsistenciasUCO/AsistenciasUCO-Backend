package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ProgramaDTO;

import java.util.List;
import java.util.UUID;

public interface ConsultarProgramasInputPort {

    List<ProgramaDTO> execute(UUID actorUsuarioId);
}
