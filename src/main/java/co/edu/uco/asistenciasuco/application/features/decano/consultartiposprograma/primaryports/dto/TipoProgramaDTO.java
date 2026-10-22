package co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports.dto;

import java.util.UUID;

public record TipoProgramaDTO(
        UUID id,
        String nombre,
        boolean estaActivoTipoPrograma,
        String estaActivoTextoTipoPrograma
) {
}
