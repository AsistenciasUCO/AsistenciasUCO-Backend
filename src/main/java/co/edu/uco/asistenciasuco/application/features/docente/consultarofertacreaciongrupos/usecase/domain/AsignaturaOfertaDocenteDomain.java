package co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.domain;

import java.util.UUID;

public record AsignaturaOfertaDocenteDomain(UUID idAsignatura, String codigoAsignatura, String nombreAsignatura,
                                            UUID idPrograma, String nombrePrograma) {
}
