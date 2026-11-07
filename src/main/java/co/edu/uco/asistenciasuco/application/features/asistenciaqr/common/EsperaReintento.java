package co.edu.uco.asistenciasuco.application.features.asistenciaqr.common;

import java.util.concurrent.ThreadLocalRandom;

/**
 * CULM-001 B.2 (3.2.7): espera entre reintentos tras un interbloqueo (20-80 ms aleatorios). Es una interfaz para que las
 * pruebas no duerman.
 */
@FunctionalInterface
public interface EsperaReintento {

    long MINIMO_MS = 20;
    long MAXIMO_MS = 80;

    void esperar(long milisegundos);

    default void esperarAleatorio() {
        esperar(ThreadLocalRandom.current().nextLong(MINIMO_MS, MAXIMO_MS + 1));
    }

    static EsperaReintento delHilo() {
        return milisegundos -> {
            try {
                Thread.sleep(milisegundos);
            } catch (final InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        };
    }
}
