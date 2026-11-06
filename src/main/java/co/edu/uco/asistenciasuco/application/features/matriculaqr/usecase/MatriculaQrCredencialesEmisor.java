package co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.TokenGeneratorPort;

import java.util.Objects;
import java.util.function.Function;

/**
 * HU176B (R3): emite token y PIN nuevos y reintenta cuando la base de datos informa que el PIN ya esta en uso por otra
 * ventana activa (MQR_006). Tras {@link #MAX_INTENTOS} colisiones propaga el ultimo error.
 */
public final class MatriculaQrCredencialesEmisor {

    public static final int MAX_INTENTOS = 5;
    private static final String CODIGO_PIN_REPETIDO = "MQR_006";

    public record Credenciales(String token, String pin) {
    }

    private final TokenGeneratorPort generador;

    public MatriculaQrCredencialesEmisor(final TokenGeneratorPort generador) {
        this.generador = Objects.requireNonNull(generador, "TokenGeneratorPort es obligatorio.");
    }

    public <T> T emitir(final Function<Credenciales, T> operacion) {
        ApplicationException ultimo = null;
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            final Credenciales credenciales = new Credenciales(generador.generarToken(), generador.generarPin());
            try {
                return operacion.apply(credenciales);
            } catch (final ApplicationException exception) {
                if (!CODIGO_PIN_REPETIDO.equals(exception.getCode())) {
                    throw exception;
                }
                ultimo = exception;
            }
        }
        throw ultimo;
    }
}
