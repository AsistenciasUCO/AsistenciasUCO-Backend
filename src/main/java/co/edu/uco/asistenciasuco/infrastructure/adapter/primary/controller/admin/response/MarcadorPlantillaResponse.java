package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

public record MarcadorPlantillaResponse(
        String marcador,
        String descripcion,
        String ejemplo,
        boolean obligatorio
) {
}
