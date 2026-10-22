package co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports.dto.TipoProgramaDTO;

import java.util.List;

public interface ConsultarTiposProgramaInputPort {

    List<TipoProgramaDTO> execute();
}
