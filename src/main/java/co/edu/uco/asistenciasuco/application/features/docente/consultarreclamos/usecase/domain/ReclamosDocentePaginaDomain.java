package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.domain;

import java.util.List;

public record ReclamosDocentePaginaDomain(List<ReclamoDocenteDomain> reclamos, int totalRegistros) {
}
