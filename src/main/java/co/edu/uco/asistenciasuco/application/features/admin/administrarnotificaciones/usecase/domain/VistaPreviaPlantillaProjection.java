package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain;

public record VistaPreviaPlantillaProjection(
        String asunto,
        String textoPlano,
        String html,
        String origen
) {
}
