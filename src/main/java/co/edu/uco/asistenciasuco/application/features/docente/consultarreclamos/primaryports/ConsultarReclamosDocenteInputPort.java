package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ConsultarReclamosDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamosDocentePaginaDTO;

public interface ConsultarReclamosDocenteInputPort {

    ReclamosDocentePaginaDTO execute(ConsultarReclamosDocenteDTO dto);
}
