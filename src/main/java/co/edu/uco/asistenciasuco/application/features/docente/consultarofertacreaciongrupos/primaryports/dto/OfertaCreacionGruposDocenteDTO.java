package co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.dto;

import java.util.List;
import java.util.UUID;

/** Datos que el docente necesita para abrir un grupo: su propio idDocente y las asignaturas de sus programas. */
public record OfertaCreacionGruposDocenteDTO(UUID idDocente, List<AsignaturaOfertaDocenteDTO> asignaturas) {
}
