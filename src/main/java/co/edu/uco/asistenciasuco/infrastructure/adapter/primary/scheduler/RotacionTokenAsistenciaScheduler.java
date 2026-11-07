package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotartokensasistenciaautomatico.primaryports.RotarTokensAsistenciaAutomaticoInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartomasvencidas.primaryports.CerrarTomasVencidasInputPort;
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
 * CULM-001 B.2 (contrato 3.5): job del SERVIDOR (patron de {@link RotacionTokenMatriculaScheduler}) que (1) rota los
 * tokens de asistencia vencidos y empuja el QR nuevo al titular por SSE y (2) cierra las tomas cuya ventana vencio. No es
 * polling del cliente. Cada pasada abre su propia correlacion, nunca propaga excepciones y es segura entre instancias (la
 * base de datos decide si rota o cierra). Hilo daemon {@code rotacion-token-asistencia-qr}; nunca {@code @Scheduled}.
 */
public final class RotacionTokenAsistenciaScheduler implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotacionTokenAsistenciaScheduler.class);

    private final RotarTokensAsistenciaAutomaticoInputPort rotarTokens;
    private final CerrarTomasVencidasInputPort cerrarVencidas;
    private final Duration tick;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService executor;

    public RotacionTokenAsistenciaScheduler(
            final RotarTokensAsistenciaAutomaticoInputPort rotarTokens,
            final CerrarTomasVencidasInputPort cerrarVencidas,
            final Duration tick
    ) {
        this.rotarTokens = Objects.requireNonNull(rotarTokens, "RotarTokensAsistenciaAutomaticoInputPort es obligatorio.");
        this.cerrarVencidas = Objects.requireNonNull(cerrarVencidas, "CerrarTomasVencidasInputPort es obligatorio.");
        this.tick = Objects.requireNonNull(tick, "El tick del scheduler es obligatorio.");
        if (tick.isZero() || tick.isNegative()) {
            throw new IllegalArgumentException("El tick del scheduler debe ser positivo.");
        }
    }

    /** Una pasada completa (rotacion y cierre); visible para pruebas. */
    public void ejecutarPasada() {
        final boolean correlacionPropia = CorrelationIdContext.get() == null;
        if (correlacionPropia) {
            CorrelationIdContext.set(UUID.randomUUID());
        }
        try {
            ejecutar("rotacion de tokens de asistencia", rotarTokens::execute);
            ejecutar("cierre de tomas vencidas", cerrarVencidas::execute);
        } finally {
            if (correlacionPropia) {
                CorrelationIdContext.clear();
            }
        }
    }

    private static void ejecutar(final String nombre, final Runnable paso) {
        try {
            paso.run();
        } catch (final RuntimeException exception) {
            LOGGER.error("Fallo la pasada de {}: {}", nombre, exception.getClass().getSimpleName());
        }
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(tarea -> {
            final Thread hilo = new Thread(tarea, "rotacion-token-asistencia-qr");
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
