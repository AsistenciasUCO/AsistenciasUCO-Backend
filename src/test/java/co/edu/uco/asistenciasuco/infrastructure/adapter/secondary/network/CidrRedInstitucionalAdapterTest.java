package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.network;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU176B (CA-05, T-18, T-21, T-22): la IP del cliente debe pertenecer a los rangos CIDR del campus configurados en
 * {@code RED/CIDRS_CAMPUS}. Sin rangos validos se rechaza cualquier direccion (fail-closed).
 */
class CidrRedInstitucionalAdapterTest {

    @ParameterizedTest
    @CsvSource({
            "10.0.0.0/8, 10.0.0.0, true",
            "10.0.0.0/8, 10.255.255.255, true",
            "10.0.0.0/8, 9.255.255.255, false",
            "10.0.0.0/8, 11.0.0.0, false",
            "192.168.10.0/24, 192.168.10.37, true",
            "192.168.10.0/24, 192.168.10.129, true",
            "192.168.10.0/25, 192.168.10.127, true",
            "192.168.10.0/25, 192.168.10.128, false",
            "192.168.10.0/24, 192.168.11.1, false",
            "127.0.0.1/32, 127.0.0.1, true",
            "127.0.0.1/32, 127.0.0.2, false",
            "2001:db8::/32, 2001:db8:ffff::1, true",
            "2001:db8::/32, 2001:db9::1, false",
            "::1/128, ::1, true",
            "::1/128, ::2, false"
    })
    void T22_pertenencia_correcta_en_los_bordes_del_rango_ipv4_e_ipv6(final String cidr, final String ip, final boolean esperado) {
        final CidrRedInstitucionalAdapter adapter = adapter(cidr);

        assertEquals(esperado, adapter.perteneceACampus(ip), () -> ip + " frente a " + cidr + " debia ser " + esperado);
    }

    @Test
    void T22_acepta_varios_rangos_separados_por_comas_y_espacios() {
        final CidrRedInstitucionalAdapter adapter = adapter(" 10.0.0.0/8 , 172.16.0.0/12 ,::1/128");

        assertTrue(adapter.perteneceACampus("10.1.2.3"));
        assertTrue(adapter.perteneceACampus("172.20.0.9"));
        assertTrue(adapter.perteneceACampus("::1"));
        assertFalse(adapter.perteneceACampus("8.8.8.8"));
    }

    @Test
    void T22_una_ipv4_mapeada_en_ipv6_se_compara_como_ipv4() {
        assertTrue(adapter("10.0.0.0/8").perteneceACampus("::ffff:10.1.2.3"));
        assertFalse(adapter("10.0.0.0/8").perteneceACampus("::ffff:11.1.2.3"));
    }

    @Test
    void T21_fail_closed_sin_rangos_configurados() {
        assertFalse(adapter("").perteneceACampus("127.0.0.1"));
        assertFalse(adapter("   ").perteneceACampus("127.0.0.1"));
        assertFalse(new CidrRedInstitucionalAdapter(Optional::empty).perteneceACampus("127.0.0.1"));
    }

    @Test
    void T21_fail_closed_si_todos_los_rangos_son_invalidos() {
        assertFalse(adapter("no-es-un-cidr,10.0.0.0/99,10.0.0/8,/8,10.0.0.1/,10.0.0.1/8/16,10.0.0.1/-1,10.0.0.1/abc,2001:db8::/129").perteneceACampus("10.0.0.1"));
        assertTrue(co.edu.uco.asistenciasuco.crosscutting.network.CidrRange.parse(null).isEmpty());
        assertTrue(co.edu.uco.asistenciasuco.crosscutting.network.CidrRange.parseLista(null).isEmpty());
        assertFalse(co.edu.uco.asistenciasuco.crosscutting.network.CidrRange.esDireccionIpValida(null));
        assertFalse(co.edu.uco.asistenciasuco.crosscutting.network.CidrRange.esDireccionIpValida("2001:db8::zzzz"));
    }

    @Test
    void un_rango_invalido_no_invalida_los_rangos_validos_restantes() {
        final CidrRedInstitucionalAdapter adapter = adapter("basura,10.0.0.0/8");

        assertTrue(adapter.perteneceACampus("10.0.0.1"));
    }

    @Test
    void ip_nula_vacia_o_con_formato_invalido_se_niega_sin_lanzar_excepcion() {
        final CidrRedInstitucionalAdapter adapter = adapter("0.0.0.0/0");

        assertFalse(adapter.perteneceACampus(null));
        assertFalse(adapter.perteneceACampus(""));
        assertFalse(adapter.perteneceACampus("   "));
        assertFalse(adapter.perteneceACampus("no-es-una-ip"));
        assertFalse(adapter.perteneceACampus("256.1.1.1"));
        // un nombre de host nunca se resuelve por DNS
        assertFalse(adapter.perteneceACampus("localhost"));
    }

    @Test
    void lee_los_rangos_en_cada_consulta_para_aplicar_cambios_del_catalogo_sin_reiniciar() {
        final String[] valor = {"10.0.0.0/8"};
        final CidrRedInstitucionalAdapter adapter = new CidrRedInstitucionalAdapter(() -> Optional.of(valor[0]));

        assertTrue(adapter.perteneceACampus("10.0.0.1"));
        valor[0] = "192.168.0.0/16";
        assertFalse(adapter.perteneceACampus("10.0.0.1"));
        assertTrue(adapter.perteneceACampus("192.168.1.1"));
    }

    @Test
    void usa_el_catalogo_de_parametros_con_el_grupo_RED_y_la_clave_CIDRS_CAMPUS() {
        final ParameterCatalogPort catalogo = new ParameterCatalogPort() {
            @Override
            public Optional<String> getParameter(final String group, final String key) {
                return "RED".equals(group) && "CIDRS_CAMPUS".equals(key) ? Optional.of("10.0.0.0/8") : Optional.empty();
            }

            @Override
            public String getRequiredParameter(final String group, final String key) {
                return getParameter(group, key).orElseThrow();
            }

            @Override
            public <T> T getParameterAs(final String group, final String key, final Class<T> targetType) {
                throw new UnsupportedOperationException();
            }
        };

        assertTrue(CidrRedInstitucionalAdapter.desdeCatalogo(catalogo).perteneceACampus("10.2.3.4"));
        assertFalse(CidrRedInstitucionalAdapter.desdeCatalogo(catalogo).perteneceACampus("11.2.3.4"));
    }

    private static CidrRedInstitucionalAdapter adapter(final String cidrs) {
        return new CidrRedInstitucionalAdapter(() -> Optional.of(cidrs));
    }
}
