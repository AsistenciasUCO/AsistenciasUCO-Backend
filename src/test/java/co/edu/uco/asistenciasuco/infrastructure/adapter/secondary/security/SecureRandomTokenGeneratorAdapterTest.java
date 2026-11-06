package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU176B (T-08, T-09, R1): token aleatorio de 256 bits en Base64URL y PIN de 6 digitos.
 */
class SecureRandomTokenGeneratorAdapterTest {

    private static final Pattern BASE64_URL = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern SEIS_DIGITOS = Pattern.compile("^[0-9]{6}$");

    private final SecureRandomTokenGeneratorAdapter generator = new SecureRandomTokenGeneratorAdapter();

    @Test
    void el_token_tiene_256_bits_de_entropia_codificados_en_base64_url_sin_relleno() {
        final String token = generator.generarToken();

        assertEquals(43, token.length(), "32 bytes en Base64URL sin relleno son 43 caracteres");
        assertTrue(BASE64_URL.matcher(token).matches());
    }

    @Test
    void los_tokens_no_se_repiten() {
        final Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            assertTrue(vistos.add(generator.generarToken()));
        }
    }

    @Test
    void el_pin_son_seis_digitos_incluyendo_ceros_a_la_izquierda() {
        boolean huboCeroInicial = false;
        for (int i = 0; i < 5000; i++) {
            final String pin = generator.generarPin();
            assertTrue(SEIS_DIGITOS.matcher(pin).matches(), pin);
            huboCeroInicial |= pin.startsWith("0");
        }
        assertTrue(huboCeroInicial, "el PIN debe poder empezar por cero (relleno a 6 digitos)");
    }
}
