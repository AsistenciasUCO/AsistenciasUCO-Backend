package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.request;

import java.util.UUID;

/** Cuerpos HTTP de la matricula por QR/PIN (HU176B). Nunca se registran en logs: llevan el token y la contrasena. */
public final class MatriculaQrRequests {

    private MatriculaQrRequests() {
    }

    /** Exactamente uno de {@code token} o {@code pin}. */
    public record Credencial(String token, String pin) {
    }

    /** Alta publica: credencial (token o PIN) mas los datos de la persona. */
    public record Registro(
            String token,
            String pin,
            UUID idTipoIdentificacion,
            Long numeroIdentificacion,
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            String correo,
            String password
    ) {
    }
}
