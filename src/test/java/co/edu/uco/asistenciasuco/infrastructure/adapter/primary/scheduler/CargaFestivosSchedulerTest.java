package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.application.features.festivos.cargarfestivosautomatico.primaryports.CargarFestivosAutomaticoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CULM-001 B.1 (CONTRACT 3.4.5): job diario de festivos con correlacion propia por pasada y sin propagar fallos. */
class CargaFestivosSchedulerTest {

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void cada_pasada_abre_y_cierra_su_propia_correlacion() {
        final AtomicReference<UUID> vista = new AtomicReference<>();
        final CargaFestivosScheduler scheduler = new CargaFestivosScheduler(() -> {
            vista.set(CorrelationIdContext.get());
            return 1;
        }, Duration.ofMinutes(1), Duration.ofHours(24));

        scheduler.ejecutarPasada();

        assertNotNull(vista.get());
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void conserva_una_correlacion_existente_y_no_propaga_excepciones() {
        final UUID existente = UUID.randomUUID();
        CorrelationIdContext.set(existente);
        final AtomicReference<UUID> vista = new AtomicReference<>();
        new CargaFestivosScheduler(() -> {
            vista.set(CorrelationIdContext.get());
            throw new IllegalStateException("db caida");
        }, Duration.ZERO, Duration.ofHours(24)).ejecutarPasada();

        assertEquals(existente, vista.get());
        assertEquals(existente, CorrelationIdContext.get());
        new CargaFestivosScheduler(() -> 0, Duration.ZERO, Duration.ofHours(24)).ejecutarPasada();
    }

    @Test
    void start_y_stop_son_idempotentes() {
        final CargaFestivosScheduler scheduler =
                new CargaFestivosScheduler(() -> 0, Duration.ofHours(1), Duration.ofHours(24));
        assertFalse(scheduler.isRunning());
        scheduler.start();
        scheduler.start();
        assertTrue(scheduler.isRunning());
        scheduler.stop();
        scheduler.stop();
        assertFalse(scheduler.isRunning());
        new CargaFestivosScheduler(() -> 0, Duration.ofHours(1), Duration.ofHours(24)).stop();
    }

    @Test
    void valida_sus_argumentos() {
        final CargarFestivosAutomaticoInputPort puerto = () -> 0;
        assertThrows(NullPointerException.class, () -> new CargaFestivosScheduler(null, Duration.ZERO, Duration.ofHours(1)));
        assertThrows(NullPointerException.class, () -> new CargaFestivosScheduler(puerto, null, Duration.ofHours(1)));
        assertThrows(NullPointerException.class, () -> new CargaFestivosScheduler(puerto, Duration.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> new CargaFestivosScheduler(puerto, Duration.ZERO, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new CargaFestivosScheduler(puerto, Duration.ZERO, Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> new CargaFestivosScheduler(puerto, Duration.ofSeconds(-1), Duration.ofHours(1)));
    }
}
