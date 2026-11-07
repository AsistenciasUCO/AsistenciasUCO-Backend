package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import java.time.Instant;
import java.util.List;

public record TipoPlantillaAdminResponse(
        String tipo,
        String idioma,
        int versionActiva,
        String asuntoActivo,
        String cuerpoActivo,
        Instant actualizadoEn,
        List<MarcadorPlantillaResponse> marcadores,
        List<VersionPlantillaResponse> versiones
) {
    public TipoPlantillaAdminResponse {
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
        versiones = versiones == null ? List.of() : List.copyOf(versiones);
    }
}
