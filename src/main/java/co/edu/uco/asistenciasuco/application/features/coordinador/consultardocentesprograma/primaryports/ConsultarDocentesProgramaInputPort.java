package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;

import java.util.List;
import java.util.UUID;

public interface ConsultarDocentesProgramaInputPort {

    List<DocenteProgramaDTO> execute(UUID actorUsuarioId);
}
