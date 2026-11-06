package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU176B (CA-07, T-23, T-24, T-25): limitador de ventana deslizante en memoria, con claves independientes y memoria
 * acotada (D7).
 */
class SlidingWindowRateLimiterTest {

    @Test
    void T23_permite_hasta_el_limite_y_rechaza_el_siguiente_dentro_de_la_ventana() {
        final MutableClock clock = new MutableClock();
        final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(Duration.ofSeconds(60), 100, clock);

        for (int i = 0; i < 20; i++) {
            assertTrue(limiter.tryAcquire("ip-1", 20), "el intento " + (i + 1) + " debia permitirse");
        }
        assertFalse(limiter.tryAcquire("ip-1", 20), "el intento 21 debia rechazarse");
    }

    @Test
    void T23_despues_de_la_ventana_vuelve_a_permitir() {
        final MutableClock clock = new MutableClock();
        final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(Duration.ofSeconds(60), 100, clock);
        for (int i = 0; i < 20; i++) {
            limiter.tryAcquire("ip-1", 20);
        }
        assertFalse(limiter.tryAcquire("ip-1", 20));

        clock.avanzar(Duration.ofSeconds(61));

        assertTrue(limiter.tryAcquire("ip-1", 20));
    }

    @Test
    void T23_la_ventana_es_deslizante_no_fija() {
        final MutableClock clock = new MutableClock();
        final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(Duration.ofSeconds(60), 100, clock);
        assertTrue(limiter.tryAcquire("ip-1", 2));            // t = 0
        clock.avanzar(Duration.ofSeconds(40));
        assertTrue(limiter.tryAcquire("ip-1", 2));            // t = 40
        clock.avanzar(Duration.ofSeconds(25));                // t = 65: el intento de t = 0 ya salio de la ventana
        assertTrue(limiter.tryAcquire("ip-1", 2));
        assertFalse(limiter.tryAcquire("ip-1", 2));
    }

    @Test
    void T24_las_claves_son_independientes_y_cada_llamada_trae_su_propio_limite() {
        final MutableClock clock = new MutableClock();
        final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(Duration.ofSeconds(60), 100, clock);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("token-a", 5));
        }

        assertFalse(limiter.tryAcquire("token-a", 5), "el sexto registro con el mismo token debia rechazarse");
        assertTrue(limiter.tryAcquire("token-b", 5), "otro token no se ve afectado");
        assertTrue(limiter.tryAcquire("ip-1", 20), "la misma clave en otro espacio de nombres no comparte contador");
    }

    @Test
    void un_intento_rechazado_no_extiende_el_bloqueo() {
        final MutableClock clock = new MutableClock();
        final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(Duration.ofSeconds(60), 100, clock);
        assertTrue(limiter.tryAcquire("ip-1", 1));
        clock.avanzar(Duration.ofSeconds(30));
        assertFalse(limiter.tryAcquire("ip-1", 1));
        clock.avanzar(Duration.ofSeconds(31));               // pasaron 61 s desde el unico intento aceptado

        assertTrue(limiter.tryAcquire("ip-1", 1));
    }

    @Test
    void T25_la_memoria_esta_acotada_y_desaloja_la_clave_menos_reciente() {
        final MutableClock clock = new MutableClock();
        final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(Duration.ofSeconds(60), 3, clock);

        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("ip-" + i, 20);
        }

        assertEquals(3, limiter.clavesActivas());
    }

    @Test
    void clave_nula_se_trata_como_desconocida_y_tambien_se_limita() {
        final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(Duration.ofSeconds(60), 10, new MutableClock());

        assertTrue(limiter.tryAcquire(null, 1));
        assertFalse(limiter.tryAcquire(null, 1));
    }

    @Test
    void valida_los_parametros_de_construccion_y_de_uso() {
        final MutableClock clock = new MutableClock();
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowRateLimiter(Duration.ZERO, 10, clock));
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowRateLimiter(Duration.ofSeconds(60), 0, clock));
        assertThrows(NullPointerException.class, () -> new SlidingWindowRateLimiter(Duration.ofSeconds(60), 10, null));
        final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(Duration.ofSeconds(60), 10, clock);
        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire("ip", 0));
    }

    /** Reloj controlable para las pruebas. */
    private static final class MutableClock extends Clock {
        private Instant ahora = Instant.parse("2026-11-04T10:00:00Z");

        void avanzar(final Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
