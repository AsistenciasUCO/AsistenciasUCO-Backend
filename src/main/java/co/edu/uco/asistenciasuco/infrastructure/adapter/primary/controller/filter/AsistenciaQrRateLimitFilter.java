package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.exception.AsistenciaQrErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * CULM-001 B.2 ([SUPUESTO-B-12], contrato 3.2.7 NB-3): limite por IP del registro de asistencia por QR que cuenta SOLO los
 * intentos rechazados. Antes de la cadena consulta el contador de la IP ({@code superaLimite}, sin registrar): si ya
 * alcanzo {@code ASISTENCIA_QR/LIMITE_FALLIDOS_IP_POR_MINUTO} responde 429 {@code AQR_011} con {@code Retry-After}. Al
 * terminar, registra un intento solo si la respuesta fue >= 400 y distinta de 429; un registro nuevo o idempotente
 * (200/201) no consume cupo, de modo que un salon completo detras del NAT del campus nunca recibe 429 por registrar.
 */
public final class AsistenciaQrRateLimitFilter extends OncePerRequestFilter {

    public static final String RUTA = "/api/v1/estudiante/asistencia-qr";
    static final String RETRY_AFTER_SEGUNDOS = "60";

    private final SlidingWindowRateLimiter limiter;
    private final IntSupplier limiteFallidosPorMinuto;
    private final NetworkClientIpResolver ipResolver;
    private final SecurityErrorResponseWriter responseWriter;

    public AsistenciaQrRateLimitFilter(
            final SlidingWindowRateLimiter limiter,
            final IntSupplier limiteFallidosPorMinuto,
            final NetworkClientIpResolver ipResolver,
            final SecurityErrorResponseWriter responseWriter
    ) {
        this.limiter = Objects.requireNonNull(limiter, "SlidingWindowRateLimiter es obligatorio.");
        this.limiteFallidosPorMinuto = Objects.requireNonNull(limiteFallidosPorMinuto, "El proveedor del limite es obligatorio.");
        this.ipResolver = Objects.requireNonNull(ipResolver, "NetworkClientIpResolver es obligatorio.");
        this.responseWriter = Objects.requireNonNull(responseWriter, "SecurityErrorResponseWriter es obligatorio.");
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !RUTA.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        final String clave = "asistencia-qr:ip:" + ipResolver.resolve(request);
        if (limiter.superaLimite(clave, Math.max(1, limiteFallidosPorMinuto.getAsInt()))) {
            response.setHeader(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SEGUNDOS);
            responseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, AsistenciaQrErrorCode.AQR_011);
            return;
        }
        boolean rechazado = true;
        try {
            filterChain.doFilter(request, response);
            final int estado = response.getStatus();
            rechazado = estado >= 400 && estado != HttpStatus.TOO_MANY_REQUESTS.value();
        } finally {
            if (rechazado) {
                limiter.registrar(clave);
            }
        }
    }
}
