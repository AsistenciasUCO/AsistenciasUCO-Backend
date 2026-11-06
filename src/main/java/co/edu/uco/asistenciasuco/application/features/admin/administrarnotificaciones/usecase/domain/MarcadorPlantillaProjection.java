package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain;

public record MarcadorPlantillaProjection(
        String marcador,
        String descripcion,
        String ejemplo,
        boolean obligatorio
) {
}
