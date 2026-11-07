package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import co.edu.uco.asistenciasuco.application.secondaryports.security.LimitadorIntentosPort;
import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;

import java.util.Objects;

/**
 * CULM-001 B.2 (contrato 3.3): limite de intentos por minuto del caso de uso del QR de asistencia (por estudiante y por
 * hash del token o PIN), en memoria y por instancia, sobre el limitador compartido de {@code infrastructure.ratelimit}.
 */
public final class SlidingWindowLimitadorIntentosAdapter implements LimitadorIntentosPort {

    private final SlidingWindowRateLimiter limiter;

    public SlidingWindowLimitadorIntentosAdapter(final SlidingWindowRateLimiter limiter) {
        this.limiter = Objects.requireNonNull(limiter, "SlidingWindowRateLimiter es obligatorio.");
    }

    @Override
    public boolean permitir(final String clave, final int limitePorMinuto) {
        return limiter.tryAcquire(clave, Math.max(1, limitePorMinuto));
    }
}
