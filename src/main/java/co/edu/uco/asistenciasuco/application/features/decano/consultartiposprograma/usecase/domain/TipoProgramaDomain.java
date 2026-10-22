package co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.usecase.domain;

import java.util.UUID;

public record TipoProgramaDomain(
        UUID id,
        String nombre,
        boolean estaActivoTipoPrograma,
        String estaActivoTextoTipoPrograma
) {
}
