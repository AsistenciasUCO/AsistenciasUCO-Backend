package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record AreaProjection(UUID id, String codigo, String nombre, Long totalAsignaturas) {
}
