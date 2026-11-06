package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.RotarTokensMatriculaAutomaticoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HU176B (D2): job del SERVIDOR que rota los tokens de matricula vencidos. No es polling del cliente: el docente recibe
 * el QR nuevo por SSE. Cada pasada abre su propia correlacion, nunca propaga excepciones y es idempotente entre
 * instancias (la DB solo rota si la ventana debe rotar). El periodo de rotacion lo decide la DB; este tick solo fija cada
 * cuanto se revisa.
 */
public final class RotacionTokenMatriculaScheduler implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotacionTokenMatriculaScheduler.class);

    private final RotarTokensMatriculaAutomaticoInputPort rotarTokens;
    private final Duration tick;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService executor;

    public RotacionTokenMatriculaScheduler(final RotarTokensMatriculaAutomaticoInputPort rotarTokens, final Duration tick) {
        this.rotarTokens = Objects.requireNonNull(rotarTokens, "RotarTokensMatriculaAutomaticoInputPort es obligatorio.");
        this.tick = Objects.requireNonNull(tick, "El tick del scheduler es obligatorio.");
        if (tick.isZero() || tick.isNegative()) {
            throw new IllegalArgumentException("El tick del scheduler debe ser positivo.");
        }
    }

    /** Una pasada completa; visible para pruebas. */
    public void ejecutarPasada() {
        final boolean correlacionPropia = CorrelationIdContext.get() == null;
        if (correlacionPropia) {
            CorrelationIdContext.set(UUID.randomUUID());
        }
        try {
            rotarTokens.execute();
        } catch (final RuntimeException exception) {
            LOGGER.error("Fallo la pasada de rotacion de tokens de matricula: {}", exception.getClass().getSimpleName());
        } finally {
            if (correlacionPropia) {
                CorrelationIdContext.clear();
            }
        }
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(tarea -> {
            final Thread hilo = new Thread(tarea, "rotacion-token-matricula-qr");
            hilo.setDaemon(true);
            return hilo;
        });
        executor.scheduleWithFixedDelay(this::ejecutarPasada, tick.toMillis(), tick.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false) && executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
