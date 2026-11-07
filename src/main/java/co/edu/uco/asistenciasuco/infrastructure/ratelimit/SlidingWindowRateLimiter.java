package co.edu.uco.asistenciasuco.infrastructure.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Limitador de ventana deslizante en memoria (D7), reutilizable por los filtros de endpoints publicos y por el adaptador
 * secundario de limites de intentos (CULM-001 B.2, R2-2: vive en un paquete neutral de infraestructura).
 *
 * <p>El limite lo recibe cada llamada, de modo que puede venir de un parametro que el administrador cambia sin
 * reiniciar. La memoria esta acotada: al superar {@code maxClaves} se desaloja la clave menos recientemente usada. Solo
 * los intentos aceptados cuentan: un rechazo no extiende el bloqueo. En varias instancias el limite es por instancia.</p>
 */
public final class SlidingWindowRateLimiter {

    private final Duration ventana;
    private final int maxClaves;
    private final Clock clock;
    private final Map<String, Deque<Instant>> intentos = new LinkedHashMap<>(16, 0.75f, true);

    public SlidingWindowRateLimiter(final Duration ventana, final int maxClaves, final Clock clock) {
        Objects.requireNonNull(ventana, "La ventana es obligatoria.");
        if (ventana.isZero() || ventana.isNegative()) {
            throw new IllegalArgumentException("La ventana del limitador debe ser positiva.");
        }
        if (maxClaves < 1) {
            throw new IllegalArgumentException("El maximo de claves del limitador debe ser al menos 1.");
        }
        this.ventana = ventana;
        this.maxClaves = maxClaves;
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    /** Registra un intento para la clave; {@code false} si ya alcanzo {@code limite} intentos dentro de la ventana. */
    public synchronized boolean tryAcquire(final String clave, final int limite) {
        if (limite < 1) {
            throw new IllegalArgumentException("El limite de intentos debe ser al menos 1.");
        }
        final String claveNormalizada = clave == null ? "desconocida" : clave;
        final Instant ahora = clock.instant();
        final Instant desde = ahora.minus(ventana);

        final Deque<Instant> registrados = intentos.computeIfAbsent(claveNormalizada, k -> new ArrayDeque<>());
        while (!registrados.isEmpty() && !registrados.peekFirst().isAfter(desde)) {
            registrados.pollFirst();
        }
        final boolean permitido = registrados.size() < limite;
        if (permitido) {
            registrados.addLast(ahora);
        }
        while (intentos.size() > maxClaves) {
            intentos.remove(intentos.keySet().iterator().next());
        }
        return permitido;
    }

    /**
     * CULM-001 B.2: solo lectura. {@code true} si la clave ya alcanzo {@code limite} intentos dentro de la ventana; poda los
     * intentos vencidos y nunca registra uno nuevo ni crea la clave.
     */
    public synchronized boolean superaLimite(final String clave, final int limite) {
        if (limite < 1) {
            throw new IllegalArgumentException("El limite de intentos debe ser al menos 1.");
        }
        final Deque<Instant> registrados = intentos.get(normalizar(clave));
        if (registrados == null) {
            return false;
        }
        podar(registrados, clock.instant().minus(ventana));
        return registrados.size() >= limite;
    }

    /** CULM-001 B.2: registra un intento de la clave sin evaluar el limite (p. ej. un intento ya rechazado). */
    public synchronized void registrar(final String clave) {
        final Instant ahora = clock.instant();
        final Deque<Instant> registrados = intentos.computeIfAbsent(normalizar(clave), k -> new ArrayDeque<>());
        podar(registrados, ahora.minus(ventana));
        registrados.addLast(ahora);
        desalojarExcedente();
    }

    private static String normalizar(final String clave) {
        return clave == null ? "desconocida" : clave;
    }

    private static void podar(final Deque<Instant> registrados, final Instant desde) {
        while (!registrados.isEmpty() && !registrados.peekFirst().isAfter(desde)) {
            registrados.pollFirst();
        }
    }

    private void desalojarExcedente() {
        while (intentos.size() > maxClaves) {
            intentos.remove(intentos.keySet().iterator().next());
        }
    }

    /** Numero de claves con intentos en memoria (visible para pruebas). */
    synchronized int clavesActivas() {
        return intentos.size();
    }
}
