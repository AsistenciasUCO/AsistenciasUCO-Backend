package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase;

import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.domain.ConsultarDetalleReclamoDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.domain.DetalleReclamoDocenteDomain;

public interface ConsultarDetalleReclamoDocenteUseCase {

    DetalleReclamoDocenteDomain execute(ConsultarDetalleReclamoDocenteDomain domain);
}
