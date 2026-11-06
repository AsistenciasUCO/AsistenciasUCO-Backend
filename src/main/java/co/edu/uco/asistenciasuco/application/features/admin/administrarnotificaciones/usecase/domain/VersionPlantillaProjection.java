package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain;

import java.time.Instant;
import java.util.UUID;

public record VersionPlantillaProjection(
        UUID id,
        String tipo,
        String idioma,
        int version,
        String asunto,
        String cuerpo,
        boolean activa,
        Instant creadoEn
) {
}
