package co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.dto;

import java.util.UUID;

public record AreaDTO(UUID id, String codigo, String nombre, Long totalAsignaturas) {
}
