package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.exception.AsistenciaQrErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CULM-001 B.2 ([SUPUESTO-B-12], B-9): el limite por IP cuenta solo los rechazos. Un 200/201 no incrementa el contador,
 * un 404/400/409 si y un 429 no; superado el limite responde 429 AQR_011 con Retry-After sin llegar al controller.
 */
class AsistenciaQrRateLimitFilterTest {

    private final SecurityErrorResponseWriter writer = mock(SecurityErrorResponseWriter.class);
    private final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(Duration.ofSeconds(60), 100,
            Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"), ZoneOffset.UTC));
    private final AsistenciaQrRateLimitFilter filtro = new AsistenciaQrRateLimitFilter(limiter, () -> 2,
            new NetworkClientIpResolver(Optional::empty), writer);

    private static MockHttpServletRequest peticion(final String metodo, final String uri) {
        final MockHttpServletRequest request = new MockHttpServletRequest(metodo, uri);
        request.setRemoteAddr("10.20.0.1");
        return request;
    }

    private int ejecutar(final int estado) throws ServletException, IOException {
        final AtomicInteger llamadas = new AtomicInteger();
        final MockHttpServletResponse respuesta = new MockHttpServletResponse();
        final FilterChain cadena = (req, res) -> {
            llamadas.incrementAndGet();
            ((MockHttpServletResponse) res).setStatus(estado);
        };
        filtro.doFilter(peticion("POST", AsistenciaQrRateLimitFilter.RUTA), respuesta, cadena);
        return llamadas.get();
    }

    @Test
    void los_registros_exitosos_e_idempotentes_nunca_consumen_cupo_por_ip() throws Exception {
        for (int i = 0; i < 50; i++) {
            assertEquals(1, ejecutar(i % 2 == 0 ? 201 : 200));
        }
        verify(writer, never()).write(any(), any(), any(), any());
    }

    @Test
    void los_rechazos_cuentan_y_al_alcanzar_el_limite_responde_429_aqr_011_con_retry_after() throws Exception {
        assertEquals(1, ejecutar(404));
        assertEquals(1, ejecutar(429));
        assertEquals(1, ejecutar(400));

        final MockHttpServletResponse respuesta = new MockHttpServletResponse();
        final MockHttpServletRequest request = peticion("POST", AsistenciaQrRateLimitFilter.RUTA);
        final AtomicInteger llamadas = new AtomicInteger();
        filtro.doFilter(request, respuesta, (req, res) -> llamadas.incrementAndGet());

        assertEquals(0, llamadas.get());
        assertEquals("60", respuesta.getHeader("Retry-After"));
        verify(writer).write(eq(request), eq(respuesta), eq(HttpStatus.TOO_MANY_REQUESTS), eq(AsistenciaQrErrorCode.AQR_011));
    }

    @Test
    void una_excepcion_de_la_cadena_cuenta_como_rechazo_y_se_propaga() throws Exception {
        final FilterChain falla = (req, res) -> {
            throw new ServletException("x");
        };
        assertThrows(ServletException.class, () -> filtro.doFilter(peticion("POST", AsistenciaQrRateLimitFilter.RUTA),
                new MockHttpServletResponse(), falla));
        assertThrows(ServletException.class, () -> filtro.doFilter(peticion("POST", AsistenciaQrRateLimitFilter.RUTA),
                new MockHttpServletResponse(), falla));
        assertEquals(0, ejecutar(201));
    }

    @Test
    void otras_rutas_y_metodos_no_se_filtran() throws Exception {
        final AtomicInteger llamadas = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            filtro.doFilter(peticion("GET", AsistenciaQrRateLimitFilter.RUTA), new MockHttpServletResponse(), (req, res) -> {
                llamadas.incrementAndGet();
                ((MockHttpServletResponse) res).setStatus(404);
            });
            filtro.doFilter(peticion("POST", "/api/v1/estudiante/matricular-grupo"), new MockHttpServletResponse(), (req, res) -> {
                llamadas.incrementAndGet();
                ((MockHttpServletResponse) res).setStatus(404);
            });
        }
        assertEquals(10, llamadas.get());
        assertEquals(1, ejecutar(201));
        assertThrows(NullPointerException.class, () -> new AsistenciaQrRateLimitFilter(null, () -> 1, new NetworkClientIpResolver(Optional::empty), writer));
        assertThrows(NullPointerException.class, () -> new AsistenciaQrRateLimitFilter(limiter, null, new NetworkClientIpResolver(Optional::empty), writer));
        assertThrows(NullPointerException.class, () -> new AsistenciaQrRateLimitFilter(limiter, () -> 1, null, writer));
        assertThrows(NullPointerException.class, () -> new AsistenciaQrRateLimitFilter(limiter, () -> 1, new NetworkClientIpResolver(Optional::empty), null));
    }
}
