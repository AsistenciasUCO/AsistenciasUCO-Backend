package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.MatriculaQrPublicaController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * HU176B (CA-07, D7): limite de intentos por IP sobre las dos rutas publicas de matricula por QR/PIN, en memoria. Usa la IP
 * que resuelve {@link NetworkClientIpResolver} (solo confia en {@code X-Forwarded-For} desde un proxy confiable). El limite
 * por minuto lo da un parametro del catalogo y se lee en cada peticion. Responde 429 antes de llegar a la seguridad o al
 * controller, de modo que tambien limita los intentos que terminarian en 400 o 404.
 *
 * <p>Pendiente documentado: el limite por token (5 registros/minuto) exige leer el cuerpo y se resuelve aparte.</p>
 */
public final class MatriculaQrRateLimitFilter extends OncePerRequestFilter {

    private final SlidingWindowRateLimiter limiter;
    private final IntSupplier limitePorMinuto;
    private final NetworkClientIpResolver ipResolver;
    private final SecurityErrorResponseWriter responseWriter;

    public MatriculaQrRateLimitFilter(
            final SlidingWindowRateLimiter limiter,
            final IntSupplier limitePorMinuto,
            final NetworkClientIpResolver ipResolver,
            final SecurityErrorResponseWriter responseWriter
    ) {
        this.limiter = Objects.requireNonNull(limiter, "SlidingWindowRateLimiter es obligatorio.");
        this.limitePorMinuto = Objects.requireNonNull(limitePorMinuto, "El proveedor del limite es obligatorio.");
        this.ipResolver = Objects.requireNonNull(ipResolver, "NetworkClientIpResolver es obligatorio.");
        this.responseWriter = Objects.requireNonNull(responseWriter, "SecurityErrorResponseWriter es obligatorio.");
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String uri = request.getRequestURI();
        return !HttpMethod.POST.matches(request.getMethod())
                || !(MatriculaQrPublicaController.RUTA_VALIDAR.equals(uri) || MatriculaQrPublicaController.RUTA_REGISTRO.equals(uri));
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        final int limite = Math.max(1, limitePorMinuto.getAsInt());
        if (!limiter.tryAcquire("ip:" + ipResolver.resolve(request), limite)) {
            responseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, SecurityErrorCode.TOO_MANY_REQUESTS);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
