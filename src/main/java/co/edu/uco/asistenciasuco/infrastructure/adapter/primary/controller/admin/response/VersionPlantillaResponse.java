package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import java.time.Instant;
import java.util.UUID;

public record VersionPlantillaResponse(
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
