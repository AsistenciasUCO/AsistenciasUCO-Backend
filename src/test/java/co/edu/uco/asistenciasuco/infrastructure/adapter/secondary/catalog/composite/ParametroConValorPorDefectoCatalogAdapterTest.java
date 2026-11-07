package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.composite.ParametroConValorPorDefectoConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.wiring.MatriculaQrWiringConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CULM-001 A.2-6 (A2-B-09, DEC-018): el decorador devuelve el valor por defecto en codigo cuando el proveedor real falla
 * o no tiene la clave; ParametrosPorDefecto es la unica fuente (igual a la semilla) y cubre todas las claves leidas en
 * src/main.
 */
class ParametroConValorPorDefectoCatalogAdapterTest {

    private final ParameterCatalogPort delegado = mock(ParameterCatalogPort.class);
    private final ParametroConValorPorDefectoCatalogAdapter adapter = new ParametroConValorPorDefectoCatalogAdapter(delegado);

    @Test
    void con_valor_en_el_proveedor_lo_devuelve_sin_tocarlo() {
        when(delegado.getParameter("MATRICULA_QR", "LIMITE_IP_POR_MINUTO")).thenReturn(Optional.of("7"));

        assertEquals(Optional.of("7"), adapter.getParameter("MATRICULA_QR", "LIMITE_IP_POR_MINUTO"));
        assertEquals(7, adapter.getParameterAs("MATRICULA_QR", "LIMITE_IP_POR_MINUTO", Integer.class));
        assertSame(delegado, adapter.delegado());
    }

    @Test
    void si_el_proveedor_falla_o_esta_vacio_rige_el_valor_por_defecto() {
        when(delegado.getParameter("RED", "CIDRS_CAMPUS")).thenThrow(new ParameterCatalogPort.ParameterCatalogException("401 de Azure"));
        when(delegado.getParameter("REGISTRO", "DOMINIO_CORREO_ESTUDIANTE")).thenReturn(Optional.empty());
        when(delegado.getParameter("MATRICULA_QR", "HABILITADA")).thenReturn(null);

        assertEquals(Optional.of("127.0.0.1/32,::1/128"), adapter.getParameter("RED", "CIDRS_CAMPUS"));
        assertEquals("@uco.edu.co", adapter.getRequiredParameter("REGISTRO", "DOMINIO_CORREO_ESTUDIANTE"));
        assertTrue(adapter.getParameterAs("MATRICULA_QR", "HABILITADA", Boolean.class));
    }

    @Test
    void sin_valor_por_defecto_el_opcional_queda_vacio_el_fallo_se_propaga_y_el_obligatorio_falla() {
        when(delegado.getParameter("X", "Y")).thenReturn(Optional.empty());
        when(delegado.getParameter("X", "Z")).thenThrow(new ParameterCatalogPort.ParameterCatalogException("caido"));

        assertTrue(adapter.getParameter("X", "Y").isEmpty());
        assertThrows(ParameterCatalogPort.ParameterNotFoundException.class, () -> adapter.getRequiredParameter("X", "Y"));
        assertThrows(ParameterCatalogPort.ParameterCatalogException.class, () -> adapter.getParameter("X", "Z"));
    }

    @Test
    void conversion_de_tipos_del_valor_por_defecto() {
        when(delegado.getParameter("ARCHIVOS", "LIMITE_SUBIDAS_POR_MINUTO")).thenReturn(Optional.empty());

        assertEquals(10L, adapter.getParameterAs("ARCHIVOS", "LIMITE_SUBIDAS_POR_MINUTO", Long.class));
        assertEquals(10, adapter.getParameterAs("ARCHIVOS", "LIMITE_SUBIDAS_POR_MINUTO", int.class));
        assertEquals(10.0, adapter.getParameterAs("ARCHIVOS", "LIMITE_SUBIDAS_POR_MINUTO", Double.class));
        assertEquals("10", adapter.getParameterAs("ARCHIVOS", "LIMITE_SUBIDAS_POR_MINUTO", String.class));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.getParameterAs("ARCHIVOS", "LIMITE_SUBIDAS_POR_MINUTO", java.math.BigDecimal.class));
        assertThrows(NullPointerException.class, () -> new ParametroConValorPorDefectoCatalogAdapter(null));
    }

    @Test
    void la_tabla_de_valores_por_defecto_es_la_de_la_semilla_y_cubre_las_claves_leidas() {
        final Map<String, String> esperado = new LinkedHashMap<>();
        esperado.put("MATRICULA_QR:HABILITADA", "true");
        esperado.put("MATRICULA_QR:URL_BASE_FRONTEND", "http://localhost:4200");
        esperado.put("MATRICULA_QR:LIMITE_IP_POR_MINUTO", "20");
        esperado.put("MATRICULA_QR:LIMITE_REGISTROS_TOKEN_POR_MINUTO", "5");
        esperado.put("MATRICULA_QR:ESTADO_MATRICULA", "A");
        esperado.put("RED:CIDRS_CAMPUS", "127.0.0.1/32,::1/128");
        esperado.put("RED:PROXIES_CONFIABLES", "");
        esperado.put("IDENTIDAD:PERMITIR_CORREGIR_DOCUMENTO", "1");
        esperado.put("REGISTRO:DOMINIO_CORREO_ESTUDIANTE", "@uco.edu.co");
        esperado.put("ARCHIVOS:LIMITE_SUBIDAS_POR_MINUTO", "10");
        esperado.put("ASISTENCIA_QR:LIMITE_IP_POR_MINUTO", "20");
        esperado.put("ASISTENCIA_QR:LIMITE_ESTUDIANTE_POR_MINUTO", "5");
        // CULM-001 B.2 (semilla 35; DEC-045 conserva el 5 de arriba).
        esperado.put("ASISTENCIA_QR:HABILITADA", "true");
        esperado.put("ASISTENCIA_QR:ROTACION_SEGUNDOS", "60");
        esperado.put("ASISTENCIA_QR:LIMITE_FALLIDOS_IP_POR_MINUTO", "600");
        esperado.put("ASISTENCIA_QR:LIMITE_TOKEN_POR_MINUTO", "120");
        esperado.put("ASISTENCIA_QR:URL_BASE_FRONTEND", "http://localhost:4200");
        esperado.put("RECLAMO:ESTADO_PENDIENTE", "P");
        esperado.put("RECLAMO:ESTADO_RETIRADO", "X");
        esperado.put("MATRICULA:ESTADO_RETIRADA", "I");
        esperado.put("MATRICULA:ESTADO_INSCRITA", "A");

        assertEquals(esperado, ParametrosPorDefecto.todos());
        assertTrue(ParametrosPorDefecto.valor(null, "X").isEmpty());
        assertTrue(ParametrosPorDefecto.valor("X", null).isEmpty());
        assertEquals(20, ParametrosPorDefecto.entero(" MATRICULA_QR ", "LIMITE_IP_POR_MINUTO"));
        assertThrows(IllegalArgumentException.class, () -> ParametrosPorDefecto.entero("X", "Y"));
    }

    @Test
    void matricula_qr_wiring_ya_no_declara_su_propio_valor_por_defecto() {
        final boolean declara = Arrays.stream(MatriculaQrWiringConfiguration.class.getDeclaredFields())
                .map(Field::getName)
                .anyMatch("LIMITE_IP_POR_DEFECTO"::equals);

        assertFalse(declara, "ParametrosPorDefecto es la unica fuente de valores por defecto (N9).");
    }

    @Test
    void el_contexto_arranca_con_el_decorador_primario_y_el_delegado_calificado_sin_ciclo() {
        new ApplicationContextRunner()
                .withUserConfiguration(DelegadoDePrueba.class, ParametroConValorPorDefectoConfiguration.class)
                .run(context -> {
                    final ParameterCatalogPort port = context.getBean(ParameterCatalogPort.class);
                    assertInstanceOf(ParametroConValorPorDefectoCatalogAdapter.class, port);
                    assertSame(context.getBean("delegadoDePrueba"), ((ParametroConValorPorDefectoCatalogAdapter) port).delegado());
                    assertEquals(Optional.of("20"), port.getParameter("ASISTENCIA_QR", "LIMITE_IP_POR_MINUTO"));
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class DelegadoDePrueba {

        @Bean
        @Qualifier(ParametroConValorPorDefectoConfiguration.DELEGADO)
        ParameterCatalogPort delegadoDePrueba() {
            final ParameterCatalogPort caido = mock(ParameterCatalogPort.class);
            when(caido.getParameter(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                    .thenThrow(new ParameterCatalogPort.ParameterCatalogException("catalogo caido"));
            return caido;
        }
    }
}
