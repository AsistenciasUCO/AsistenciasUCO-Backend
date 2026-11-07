package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request;

public record EnviarCorreoPruebaRequest(
        String tipo,
        String correoDestino
) {
    public EnviarCorreoPruebaRequest(final String correoDestino) {
        this(null, correoDestino);
    }
}
