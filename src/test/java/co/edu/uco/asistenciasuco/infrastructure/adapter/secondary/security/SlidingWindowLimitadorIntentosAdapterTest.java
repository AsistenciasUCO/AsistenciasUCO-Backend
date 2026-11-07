package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CULM-001 B.2: el limitador del caso de uso cuenta todos los intentos de una clave; un limite no positivo vale 1. */
class SlidingWindowLimitadorIntentosAdapterTest {

    @Test
    void permite_hasta_el_limite_por_clave_y_un_limite_no_positivo_vale_uno() {
        final SlidingWindowLimitadorIntentosAdapter adapter = new SlidingWindowLimitadorIntentosAdapter(new SlidingWindowRateLimiter(
                Duration.ofSeconds(60), 100, Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"), ZoneOffset.UTC)));

        assertTrue(adapter.permitir("estudiante:1", 2));
        assertTrue(adapter.permitir("estudiante:1", 2));
        assertFalse(adapter.permitir("estudiante:1", 2));
        assertTrue(adapter.permitir("estudiante:2", 0));
        assertFalse(adapter.permitir("estudiante:2", 0));
        assertThrows(NullPointerException.class, () -> new SlidingWindowLimitadorIntentosAdapter(null));
    }
}
