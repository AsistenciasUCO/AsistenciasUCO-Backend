package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntSupplier;

/**
 * CULM-001 A.2-4 (F-TRX-003): limite de intentos por minuto, en memoria, de dos operaciones autenticadas.
 *
 * <ul>
 *   <li>{@code POST /api/v1/archivos/subir}: por usuario ({@code usr:<sub>}) y por IP ({@code ip:<ip>}), ambos con
 *       {@code ARCHIVOS/LIMITE_SUBIDAS_POR_MINUTO}.</li>
 *   <li>{@code POST /api/v1/estudiante/asistencia-qr}: por IP con {@code ASISTENCIA_QR/LIMITE_IP_POR_MINUTO} y por
 *       estudiante con {@code ASISTENCIA_QR/LIMITE_ESTUDIANTE_POR_MINUTO}.</li>
 * </ul>
 *
 * <p>Se registra despues de la cadena de Spring Security, asi que conoce al principal autenticado (las peticiones sin token
 * o sin rol ya fueron rechazadas). Al superar un limite responde 429 {@code TOO_MANY_REQUESTS} con el escritor de errores
 * de seguridad. Los limites se leen en cada peticion (el administrador los cambia sin reiniciar); si el catalogo falla
 * rigen los valores por defecto. El limite por token de asistencia queda para el paquete B.</p>
 */
public final class OperacionAutenticadaRateLimitFilter extends OncePerRequestFilter {

    public static final String RUTA_SUBIR_ARCHIVO = "/api/v1/archivos/subir";
    public static final String RUTA_ASISTENCIA_QR = "/api/v1/estudiante/asistencia-qr";

    /** Una clave de limite: prefijo, origen de la clave y limite por minuto. */
    public record Regla(String prefijo, Origen origen, IntSupplier limitePorMinuto) {

        public Regla {
            Objects.requireNonNull(prefijo, "El prefijo de la regla es obligatorio.");
            Objects.requireNonNull(origen, "El origen de la regla es obligatorio.");
            Objects.requireNonNull(limitePorMinuto, "El proveedor del limite es obligatorio.");
        }
    }

    /** De donde sale el valor de la clave de limite. */
    public enum Origen { USUARIO, IP }

    private final SlidingWindowRateLimiter limiter;
    private final NetworkClientIpResolver ipResolver;
    private final SecurityErrorResponseWriter responseWriter;
    private final List<Regla> reglasSubida;
    private final List<Regla> reglasAsistenciaQr;

    public OperacionAutenticadaRateLimitFilter(
            final SlidingWindowRateLimiter limiter,
            final NetworkClientIpResolver ipResolver,
            final SecurityErrorResponseWriter responseWriter,
            final IntSupplier limiteSubidasPorMinuto,
            final IntSupplier limiteAsistenciaQrPorIp,
            final IntSupplier limiteAsistenciaQrPorEstudiante
    ) {
        this.limiter = Objects.requireNonNull(limiter, "SlidingWindowRateLimiter es obligatorio.");
        this.ipResolver = Objects.requireNonNull(ipResolver, "NetworkClientIpResolver es obligatorio.");
        this.responseWriter = Objects.requireNonNull(responseWriter, "SecurityErrorResponseWriter es obligatorio.");
        this.reglasSubida = List.of(
                new Regla("archivos:", Origen.USUARIO, limiteSubidasPorMinuto),
                new Regla("archivos:", Origen.IP, limiteSubidasPorMinuto));
        this.reglasAsistenciaQr = List.of(
                new Regla("asistencia-qr:", Origen.IP, limiteAsistenciaQrPorIp),
                new Regla("asistencia-qr:", Origen.USUARIO, limiteAsistenciaQrPorEstudiante));
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return reglasDe(request).isEmpty();
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        for (final Regla regla : reglasDe(request)) {
            final int limite = Math.max(1, regla.limitePorMinuto().getAsInt());
            if (!limiter.tryAcquire(regla.prefijo() + clave(regla.origen(), request), limite)) {
                responseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, SecurityErrorCode.TOO_MANY_REQUESTS);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private List<Regla> reglasDe(final HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return List.of();
        }
        final String uri = request.getRequestURI();
        if (RUTA_SUBIR_ARCHIVO.equals(uri)) {
            return reglasSubida;
        }
        if (RUTA_ASISTENCIA_QR.equals(uri)) {
            return reglasAsistenciaQr;
        }
        return List.of();
    }

    private String clave(final Origen origen, final HttpServletRequest request) {
        if (origen == Origen.IP) {
            return "ip:" + ipResolver.resolve(request);
        }
        return "usr:" + Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("anonimo");
    }
}
