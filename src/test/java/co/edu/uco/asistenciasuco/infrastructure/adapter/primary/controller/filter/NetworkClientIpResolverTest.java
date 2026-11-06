package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU176B (D5, T-19, T-20): la IP que se compara con los rangos del campus es {@code remoteAddr}, salvo que la peticion
 * llegue desde un proxy confiable ({@code RED/PROXIES_CONFIABLES}); solo entonces se lee {@code X-Forwarded-For}, de
 * derecha a izquierda, saltando los proxies confiables. Un encabezado falsificado nunca puede hacer pasar por el campus.
 */
class NetworkClientIpResolverTest {

    @Test
    void T19_un_remoteAddr_no_confiable_ignora_X_Forwarded_For_falsificado() {
        final MockHttpServletRequest request = request("203.0.113.9", "10.0.0.5");

        assertEquals("203.0.113.9", resolver("192.168.0.5").resolve(request));
    }

    @Test
    void T19_sin_proxies_configurados_nunca_se_confia_en_X_Forwarded_For() {
        final MockHttpServletRequest request = request("203.0.113.9", "10.0.0.5");

        assertEquals("203.0.113.9", resolver("").resolve(request));
        assertEquals("203.0.113.9", new NetworkClientIpResolver(Optional::empty).resolve(request));
    }

    @Test
    void T20_un_proxy_confiable_permite_leer_la_IP_del_cliente_de_X_Forwarded_For() {
        final MockHttpServletRequest request = request("192.168.0.5", "10.20.30.40");

        assertEquals("10.20.30.40", resolver("192.168.0.0/24").resolve(request));
    }

    @Test
    void T20_con_una_cadena_de_proxies_toma_el_primer_salto_no_confiable_de_derecha_a_izquierda() {
        // El cliente anterior agrego "10.0.0.5" al inicio para falsificar; el proxy confiable agrego la IP real al final.
        final MockHttpServletRequest request = request("192.168.0.5", "10.0.0.5, 203.0.113.77, 192.168.0.9");

        assertEquals("203.0.113.77", resolver("192.168.0.0/24").resolve(request));
    }

    @Test
    void sin_encabezado_o_con_encabezado_invalido_usa_remoteAddr() {
        assertEquals("192.168.0.5", resolver("192.168.0.0/24").resolve(request("192.168.0.5", null)));
        assertEquals("192.168.0.5", resolver("192.168.0.0/24").resolve(request("192.168.0.5", "   ")));
        assertEquals("192.168.0.5", resolver("192.168.0.0/24").resolve(request("192.168.0.5", "no-es-una-ip")));
        assertEquals("192.168.0.5", resolver("192.168.0.0/24").resolve(request("192.168.0.5", "10.0.0.5, basura")));
    }

    @Test
    void si_todos_los_saltos_son_proxies_confiables_usa_remoteAddr() {
        assertEquals("192.168.0.5", resolver("192.168.0.0/24").resolve(request("192.168.0.5", "192.168.0.7, 192.168.0.8")));
    }

    @Test
    void remoteAddr_ausente_devuelve_cadena_vacia_que_la_red_institucional_niega() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(null);

        assertEquals("", resolver("192.168.0.0/24").resolve(request));
    }

    @Test
    void lee_los_proxies_en_cada_peticion() {
        final String[] proxies = {""};
        final NetworkClientIpResolver resolver = new NetworkClientIpResolver(() -> Optional.of(proxies[0]));
        final MockHttpServletRequest request = request("192.168.0.5", "10.20.30.40");

        assertEquals("192.168.0.5", resolver.resolve(request));
        proxies[0] = "192.168.0.0/24";
        assertEquals("10.20.30.40", resolver.resolve(request));
    }

    private static NetworkClientIpResolver resolver(final String proxies) {
        return new NetworkClientIpResolver(() -> Optional.of(proxies));
    }

    private static MockHttpServletRequest request(final String remoteAddr, final String forwardedFor) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }
}
