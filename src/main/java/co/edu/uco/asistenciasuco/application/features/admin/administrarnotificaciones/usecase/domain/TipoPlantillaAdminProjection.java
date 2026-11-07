package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain;

import java.time.Instant;
import java.util.List;

public record TipoPlantillaAdminProjection(
        String tipo,
        String idioma,
        int versionActiva,
        String asuntoActivo,
        String cuerpoActivo,
        Instant actualizadoEn,
        List<MarcadorPlantillaProjection> marcadores,
        List<VersionPlantillaProjection> versiones
) {
    public TipoPlantillaAdminProjection {
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
        versiones = versiones == null ? List.of() : List.copyOf(versiones);
    }
}
