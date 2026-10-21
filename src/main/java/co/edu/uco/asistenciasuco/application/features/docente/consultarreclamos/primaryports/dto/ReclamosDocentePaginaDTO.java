package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto;

import java.util.List;

public record ReclamosDocentePaginaDTO(List<ReclamoDocenteDTO> reclamos, int totalRegistros) {
}
