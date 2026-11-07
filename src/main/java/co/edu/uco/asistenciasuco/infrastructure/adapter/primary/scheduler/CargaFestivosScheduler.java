package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.application.features.festivos.cargarfestivosautomatico.primaryports.CargarFestivosAutomaticoInputPort;
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
 * CULM-001 B.1 (§3.4.5): job diario del SERVIDOR que carga los festivos del anio actual y de los siguientes que falten
 * (mismo patron que {@link RotacionTokenMatriculaScheduler}: {@link SmartLifecycle}, hilo daemon, sin {@code @Scheduled}).
 * Cada pasada abre su propia correlacion y nunca propaga excepciones. El interruptor {@code FESTIVOS/HABILITADO} lo
 * aplica el caso de uso. No es polling del cliente.
 */
public final class CargaFestivosScheduler implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(CargaFestivosScheduler.class);

    private final CargarFestivosAutomaticoInputPort cargarFestivos;
    private final Duration retardoInicial;
    private final Duration tick;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService executor;

    public CargaFestivosScheduler(final CargarFestivosAutomaticoInputPort cargarFestivos, final Duration retardoInicial,
                                  final Duration tick) {
        this.cargarFestivos = Objects.requireNonNull(cargarFestivos, "CargarFestivosAutomaticoInputPort es obligatorio.");
        this.retardoInicial = Objects.requireNonNull(retardoInicial, "El retardo inicial del scheduler es obligatorio.");
        this.tick = Objects.requireNonNull(tick, "El tick del scheduler es obligatorio.");
        if (tick.isZero() || tick.isNegative()) {
            throw new IllegalArgumentException("El tick del scheduler de festivos debe ser positivo.");
        }
        if (retardoInicial.isNegative()) {
            throw new IllegalArgumentException("El retardo inicial del scheduler de festivos no puede ser negativo.");
        }
    }

    /** Una pasada completa; visible para pruebas. */
    public void ejecutarPasada() {
        final boolean correlacionPropia = CorrelationIdContext.get() == null;
        if (correlacionPropia) {
            CorrelationIdContext.set(UUID.randomUUID());
        }
        try {
            final int cargados = cargarFestivos.execute();
            if (cargados > 0) {
                LOGGER.info("Pasada de carga de festivos completada. aniosCargados={}", cargados);
            }
        } catch (final RuntimeException exception) {
            LOGGER.error("Fallo la pasada de carga de festivos: {}", exception.getClass().getSimpleName());
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
            final Thread hilo = new Thread(tarea, "carga-festivos");
            hilo.setDaemon(true);
            return hilo;
        });
        executor.scheduleWithFixedDelay(this::ejecutarPasada, retardoInicial.toMillis(), tick.toMillis(), TimeUnit.MILLISECONDS);
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
