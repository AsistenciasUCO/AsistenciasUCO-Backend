package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase;

import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.domain.ConsultarReclamosDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.domain.ReclamosDocentePaginaDomain;

public interface ConsultarReclamosDocenteUseCase {

    ReclamosDocentePaginaDomain execute(ConsultarReclamosDocenteDomain domain);
}
