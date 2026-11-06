package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.RotarTokensMatriculaAutomaticoInputPort;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU176B (D2): ciclo de vida y aislamiento de correlacion/errores del scheduler de rotacion de tokens de matricula.
 */
class RotacionTokenMatriculaSchedulerTest {

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void constructor_valida_colaborador_y_tick_positivo() {
        final RotarTokensMatriculaAutomaticoInputPort port = mock(RotarTokensMatriculaAutomaticoInputPort.class);
        final Duration cincoSegundos = Duration.ofSeconds(5);
        final Duration negativo = Duration.ofSeconds(-1);

        assertThrows(NullPointerException.class, () -> new RotacionTokenMatriculaScheduler(null, cincoSegundos));
        assertThrows(NullPointerException.class, () -> new RotacionTokenMatriculaScheduler(port, null));
        assertThrows(IllegalArgumentException.class, () -> new RotacionTokenMatriculaScheduler(port, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new RotacionTokenMatriculaScheduler(port, negativo));
    }

    @Test
    void ejecutar_pasada_abre_y_limpia_correlacion_propia() {
        final AtomicReference<UUID> correlacionEnEjecucion = new AtomicReference<>();
        final RotarTokensMatriculaAutomaticoInputPort port = () -> {
            correlacionEnEjecucion.set(CorrelationIdContext.get());
            return 1;
        };
        final RotacionTokenMatriculaScheduler scheduler = new RotacionTokenMatriculaScheduler(port, Duration.ofSeconds(5));

        scheduler.ejecutarPasada();

        assertNotNull(correlacionEnEjecucion.get());
        assertNull(CorrelationIdContext.get());
    }

    @Test
    void ejecutar_pasada_conserva_correlacion_existente_y_absorbe_errores() {
        final UUID existente = UUID.randomUUID();
        CorrelationIdContext.set(existente);
        final RotarTokensMatriculaAutomaticoInputPort port = mock(RotarTokensMatriculaAutomaticoInputPort.class);
        when(port.execute()).thenThrow(new IllegalStateException("fallo simulado"));
        final RotacionTokenMatriculaScheduler scheduler = new RotacionTokenMatriculaScheduler(port, Duration.ofSeconds(5));

        scheduler.ejecutarPasada();

        verify(port).execute();
        assertEquals(existente, CorrelationIdContext.get());
    }

    @Test
    void ciclo_de_vida_start_y_stop_es_idempotente() {
        final RotarTokensMatriculaAutomaticoInputPort port = mock(RotarTokensMatriculaAutomaticoInputPort.class);
        final RotacionTokenMatriculaScheduler scheduler = new RotacionTokenMatriculaScheduler(port, Duration.ofHours(1));

        assertFalse(scheduler.isRunning());
        scheduler.stop();
        assertFalse(scheduler.isRunning());

        scheduler.start();
        assertTrue(scheduler.isRunning());
        scheduler.start();
        assertTrue(scheduler.isRunning());

        scheduler.stop();
        assertFalse(scheduler.isRunning());
    }
}
