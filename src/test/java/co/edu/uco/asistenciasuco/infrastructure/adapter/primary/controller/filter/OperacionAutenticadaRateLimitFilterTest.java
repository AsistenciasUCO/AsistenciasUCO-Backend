package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.config.wiring.RateLimitWiringConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CULM-001 A.2-4 (A2-B-07, F-TRX-003): limite de intentos por usuario y por IP de la subida de archivos y de la
 * asistencia por QR. N+1 intentos responden 429; las demas rutas no se limitan; con el catalogo caido rigen los valores
 * por defecto.
 */
class OperacionAutenticadaRateLimitFilterTest {

    private final SecurityErrorResponseWriter writer = mock(SecurityErrorResponseWriter.class);

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private OperacionAutenticadaRateLimitFilter filtro(final int subidas, final int qrIp, final int qrEstudiante) {
        return new OperacionAutenticadaRateLimitFilter(
                new SlidingWindowRateLimiter(Duration.ofSeconds(60), 100, Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"), ZoneOffset.UTC)),
                new NetworkClientIpResolver(Optional::empty), writer, () -> subidas);
    }

    private static MockHttpServletRequest peticion(final String uri, final String metodo, final String ip, final String usuario) {
        SecurityContextHolder.getContext().setAuthentication(usuario == null ? null : new TestingAuthenticationToken(usuario, null));
        final MockHttpServletRequest request = new MockHttpServletRequest(metodo, uri);
        request.setRequestURI(uri);
        request.setRemoteAddr(ip);
        return request;
    }

    private boolean pasa(final OperacionAutenticadaRateLimitFilter filtro, final MockHttpServletRequest request) throws Exception {
        final MockFilterChain cadena = new MockFilterChain();
        filtro.doFilter(request, new MockHttpServletResponse(), cadena);
        return cadena.getRequest() != null;
    }

    @Test
    void subidas_por_usuario_n_mas_uno_responde_429() throws Exception {
        final OperacionAutenticadaRateLimitFilter f = filtro(2, 50, 50);
        for (int i = 0; i < 2; i++) {
            assertEquals(true, pasa(f, peticion(OperacionAutenticadaRateLimitFilter.RUTA_SUBIR_ARCHIVO, "POST", "10.0.0." + i, "est-1")));
        }
        final MockHttpServletRequest tercera = peticion(OperacionAutenticadaRateLimitFilter.RUTA_SUBIR_ARCHIVO, "POST", "10.0.0.9", "est-1");
        final MockHttpServletResponse respuesta = new MockHttpServletResponse();
        final MockFilterChain cadena = new MockFilterChain();
        f.doFilter(tercera, respuesta, cadena);

        assertNull(cadena.getRequest());
        verify(writer).write(tercera, respuesta, HttpStatus.TOO_MANY_REQUESTS, SecurityErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    void subidas_por_ip_con_usuarios_distintos_responde_429() throws Exception {
        final OperacionAutenticadaRateLimitFilter f = filtro(2, 50, 50);
        assertEquals(true, pasa(f, peticion(OperacionAutenticadaRateLimitFilter.RUTA_SUBIR_ARCHIVO, "POST", "10.0.0.1", "a")));
        assertEquals(true, pasa(f, peticion(OperacionAutenticadaRateLimitFilter.RUTA_SUBIR_ARCHIVO, "POST", "10.0.0.1", "b")));
        assertEquals(false, pasa(f, peticion(OperacionAutenticadaRateLimitFilter.RUTA_SUBIR_ARCHIVO, "POST", "10.0.0.1", "c")));
    }

    @Test
    void B2_la_asistencia_por_qr_ya_no_se_limita_en_este_filtro() throws Exception {
        // CULM-001 B.2 ([SUPUESTO-B-12]): el limite por IP cuenta solo rechazos (AsistenciaQrRateLimitFilter) y los limites
        // por estudiante y token viven en el caso de uso; este filtro deja pasar todos los intentos de la ruta.
        final OperacionAutenticadaRateLimitFilter f = filtro(1, 1, 1);
        for (int i = 0; i < 5; i++) {
            assertEquals(true, pasa(f, peticion("/api/v1/estudiante/asistencia-qr", "POST", "10.0.0.2", "a")));
        }
        // Sin principal la clave de la subida es anonima (la cadena de seguridad ya habria rechazado la peticion).
        assertEquals(true, pasa(f, peticion(OperacionAutenticadaRateLimitFilter.RUTA_SUBIR_ARCHIVO, "POST", "10.0.0.5", null)));
    }

    @Test
    void otras_rutas_y_otros_metodos_no_se_limitan_y_un_limite_no_positivo_vale_uno() throws Exception {
        final OperacionAutenticadaRateLimitFilter f = filtro(0, 0, 0);
        for (int i = 0; i < 5; i++) {
            assertEquals(true, pasa(f, peticion("/api/v1/grupos", "POST", "10.0.0.1", "a")));
            assertEquals(true, pasa(f, peticion(OperacionAutenticadaRateLimitFilter.RUTA_SUBIR_ARCHIVO, "GET", "10.0.0.1", "a")));
        }
        assertEquals(true, pasa(f, peticion(OperacionAutenticadaRateLimitFilter.RUTA_SUBIR_ARCHIVO, "POST", "10.0.0.7", "z")));
        assertEquals(false, pasa(f, peticion(OperacionAutenticadaRateLimitFilter.RUTA_SUBIR_ARCHIVO, "POST", "10.0.0.7", "z")));
    }

    @Test
    void con_el_catalogo_caido_rigen_los_valores_por_defecto() throws Exception {
        final ParameterCatalogPort caido = mock(ParameterCatalogPort.class);
        when(caido.getParameter(anyString(), anyString())).thenThrow(new ParameterCatalogPort.ParameterCatalogException("401"));
        final ParameterCatalogPort vacio = mock(ParameterCatalogPort.class);
        when(vacio.getParameter(anyString(), anyString())).thenReturn(Optional.empty());
        final ParameterCatalogPort invalido = mock(ParameterCatalogPort.class);
        when(invalido.getParameter(anyString(), anyString())).thenReturn(Optional.of("diez"));
        final ParameterCatalogPort configurado = mock(ParameterCatalogPort.class);
        when(configurado.getParameter(anyString(), anyString())).thenReturn(Optional.of(" 3 "));

        assertEquals(10, RateLimitWiringConfiguration.limite(caido, "ARCHIVOS", "LIMITE_SUBIDAS_POR_MINUTO"));
        assertEquals(20, RateLimitWiringConfiguration.limite(vacio, "ASISTENCIA_QR", "LIMITE_IP_POR_MINUTO"));
        assertEquals(5, RateLimitWiringConfiguration.limite(invalido, "ASISTENCIA_QR", "LIMITE_ESTUDIANTE_POR_MINUTO"));
        assertEquals(3, RateLimitWiringConfiguration.limite(configurado, "ARCHIVOS", "LIMITE_SUBIDAS_POR_MINUTO"));
        verify(writer, never()).write(any(), any(), any(), any());
    }

    @Test
    void la_regla_exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new OperacionAutenticadaRateLimitFilter.Regla(null,
                OperacionAutenticadaRateLimitFilter.Origen.IP, () -> 1));
        assertThrows(NullPointerException.class, () -> new OperacionAutenticadaRateLimitFilter.Regla("p", null, () -> 1));
        assertThrows(NullPointerException.class, () -> new OperacionAutenticadaRateLimitFilter.Regla("p",
                OperacionAutenticadaRateLimitFilter.Origen.USUARIO, null));
        assertNotNull(new OperacionAutenticadaRateLimitFilter.Regla("p", OperacionAutenticadaRateLimitFilter.Origen.USUARIO, () -> 1));
    }
}
