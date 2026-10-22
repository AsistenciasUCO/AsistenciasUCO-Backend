package co.edu.uco.asistenciasuco.application.features.admin.consultarareas.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de areas.
 */
public record AreaDomain(UUID id, String codigo, String nombre, Long totalAsignaturas) {
}
