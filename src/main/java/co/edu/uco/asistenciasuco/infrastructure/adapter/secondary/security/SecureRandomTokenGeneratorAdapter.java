package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import co.edu.uco.asistenciasuco.application.secondaryports.security.TokenGeneratorPort;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generador criptografico de credenciales de matricula basado en {@link SecureRandom} y SHA-256.
 */
public final class SecureRandomTokenGeneratorAdapter implements TokenGeneratorPort {

    private static final int BYTES_TOKEN = 32;
    private static final int MODULO_PIN = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generarToken() {
        final byte[] bytes = new byte[BYTES_TOKEN];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String generarPin() {
        return String.format("%06d", random.nextInt(MODULO_PIN));
    }
}
