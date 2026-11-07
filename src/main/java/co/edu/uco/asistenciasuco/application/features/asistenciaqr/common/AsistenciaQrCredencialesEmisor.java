package co.edu.uco.asistenciasuco.application.features.asistenciaqr.common;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.exception.AsistenciaQrErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.security.TokenGeneratorPort;

import java.util.Objects;
import java.util.function.Function;

/**
 * CULM-001 B.2: emite token (32 bytes base64url) y PIN (6 digitos) nuevos y reintenta cuando la base de datos informa que
 * el PIN ya esta en uso entre los activos o anteriores vigentes ({@code AQR_006}). Tras {@link #MAX_INTENTOS} colisiones
 * propaga el ultimo error.
 */
public final class AsistenciaQrCredencialesEmisor {

    public static final int MAX_INTENTOS = 5;

    public record Credenciales(String token, String pin) {
    }

    private final TokenGeneratorPort generador;

    public AsistenciaQrCredencialesEmisor(final TokenGeneratorPort generador) {
        this.generador = Objects.requireNonNull(generador, "TokenGeneratorPort es obligatorio.");
    }

    public <R> R emitir(final Function<Credenciales, R> operacion) {
        ApplicationException ultimo = null;
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            final Credenciales credenciales = new Credenciales(generador.generarToken(), generador.generarPin());
            try {
                return operacion.apply(credenciales);
            } catch (final ApplicationException exception) {
                if (!AsistenciaQrErrorCode.AQR_006.code().equals(exception.getCode())) {
                    throw exception;
                }
                ultimo = exception;
            }
        }
        throw ultimo;
    }
}
