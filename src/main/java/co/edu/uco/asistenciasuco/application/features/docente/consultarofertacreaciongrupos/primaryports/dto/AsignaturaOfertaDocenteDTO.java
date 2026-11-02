package co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.dto;

import java.util.UUID;

public record AsignaturaOfertaDocenteDTO(UUID idAsignatura, String codigoAsignatura, String nombreAsignatura,
                                         UUID idPrograma, String nombrePrograma) {
}
