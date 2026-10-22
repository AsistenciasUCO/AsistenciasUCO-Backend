package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.ConsultarDetalleReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.DetalleReclamoDocenteDTO;

public interface ConsultarDetalleReclamoDocenteInputPort {

    DetalleReclamoDocenteDTO execute(ConsultarDetalleReclamoDocenteDTO dto);
}
