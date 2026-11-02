package co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.domain;

import java.util.List;
import java.util.UUID;

/** Modelo de resultado interno de la oferta de creacion de grupos del docente. */
public record OfertaCreacionGruposDocenteDomain(UUID idDocente, List<AsignaturaOfertaDocenteDomain> asignaturas) {
}
