package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto;

import java.util.UUID;

/** Entrada: el docente se identifica solo por el usuario autenticado; estado y paginacion son opcionales. */
public record ConsultarReclamosDocenteDTO(UUID actorUsuarioId, String estado, Integer pagina, Integer tamano) {
}
