package co.edu.uco.asistenciasuco.application.features.festivos.common;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.CalendarioFestivosPort;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CULM-001 B.1 (CONTRACT 3.4.4, DEC-017): orden de proveedores por parametros, respaldo, fusion de fechas repetidas,
 * validacion del conjunto y diferencias frente al calculo local.
 */
class ObtenerFestivosConRespaldoServiceTest {

    private static final int ANIO = 2026;

    private final ParameterCatalogPort catalogo = mock(ParameterCatalogPort.class);
    private final FestivosParametros parametros = new FestivosParametros(catalogo);

    {
        when(catalogo.getParameter(anyString(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    void principal_ok_gana_sin_respaldo_y_sin_diferencias() {
        final Proveedor principal = new Proveedor("CALENDARIOS_NACIONALES", festivos(ANIO, 19));
        final Proveedor feriados = new Proveedor("FERIADOS_IO", festivos(ANIO, 19));
        final Proveedor local = new Proveedor("CALCULO_LOCAL", festivos(ANIO, 19));

        final ResultadoObtencionFestivos resultado = servicio(principal, feriados, local).obtener(ANIO);

        assertEquals("CALENDARIOS_NACIONALES", resultado.fuente());
        assertFalse(resultado.usoRespaldo());
        assertEquals(0, resultado.diferenciasCalculoLocal());
        assertEquals(19, resultado.festivos().size());
        assertEquals(0, feriados.llamadas.get());
    }

    @Test
    void principal_caida_usa_feriados_io_como_respaldo() {
        final Proveedor principal = Proveedor.fallido("CALENDARIOS_NACIONALES");
        final Proveedor feriados = new Proveedor("FERIADOS_IO", festivos(ANIO, 19));

        final ResultadoObtencionFestivos resultado =
                servicio(principal, feriados, new Proveedor("CALCULO_LOCAL", festivos(ANIO, 19))).obtener(ANIO);

        assertEquals("FERIADOS_IO", resultado.fuente());
        assertTrue(resultado.usoRespaldo());
    }

    @Test
    void sin_api_key_feriados_io_se_salta_y_responde_el_calculo_local() {
        final Proveedor feriados = new Proveedor("FERIADOS_IO", festivos(ANIO, 19));
        feriados.disponible = false;

        final ResultadoObtencionFestivos resultado = servicio(Proveedor.fallido("CALENDARIOS_NACIONALES"), feriados,
                new Proveedor("CALCULO_LOCAL", festivos(ANIO, 19))).obtener(ANIO);

        assertEquals("CALCULO_LOCAL", resultado.fuente());
        assertTrue(resultado.usoRespaldo());
        assertEquals(0, resultado.diferenciasCalculoLocal());
        assertEquals(0, feriados.llamadas.get());
    }

    @Test
    void un_conjunto_invalido_pasa_al_siguiente_proveedor() {
        final Proveedor pocos = new Proveedor("CALENDARIOS_NACIONALES", festivos(ANIO, 3));
        final Proveedor otroAnio = new Proveedor("FERIADOS_IO", festivos(ANIO + 1, 19));
        final ResultadoObtencionFestivos resultado =
                servicio(pocos, otroAnio, new Proveedor("CALCULO_LOCAL", festivos(ANIO, 19))).obtener(ANIO);
        assertEquals("CALCULO_LOCAL", resultado.fuente());

        final List<FestivoColombia> sinNombre = new ArrayList<>(festivos(ANIO, 18));
        sinNombre.add(new FestivoColombia(LocalDate.of(ANIO, 12, 31), "   "));
        final List<FestivoColombia> nombreLargo = new ArrayList<>(festivos(ANIO, 18));
        nombreLargo.add(new FestivoColombia(LocalDate.of(ANIO, 12, 31), "x".repeat(121)));
        assertEquals("CALCULO_LOCAL", servicio(new Proveedor("CALENDARIOS_NACIONALES", sinNombre),
                new Proveedor("FERIADOS_IO", nombreLargo), new Proveedor("CALCULO_LOCAL", festivos(ANIO, 19)))
                .obtener(ANIO).fuente());
    }

    @Test
    void demasiados_festivos_tambien_es_un_conjunto_invalido() {
        final Proveedor demasiados = new Proveedor("CALENDARIOS_NACIONALES", festivos(ANIO, 30));
        assertEquals("CALCULO_LOCAL", servicio(demasiados, new Proveedor("CALCULO_LOCAL", festivos(ANIO, 19)))
                .obtener(ANIO).fuente());
    }

    @Test
    void las_diferencias_con_el_calculo_local_se_cuentan_como_diferencia_simetrica_y_gana_la_principal() {
        final List<FestivoColombia> principal = new ArrayList<>(festivos(ANIO, 18));
        principal.add(new FestivoColombia(LocalDate.of(ANIO, 12, 30), "Solo en la fuente"));
        final List<FestivoColombia> local = new ArrayList<>(festivos(ANIO, 18));
        local.add(new FestivoColombia(LocalDate.of(ANIO, 12, 31), "Solo en el calculo local"));

        final ResultadoObtencionFestivos resultado = servicio(new Proveedor("CALENDARIOS_NACIONALES", principal),
                new Proveedor("CALCULO_LOCAL", local)).obtener(ANIO);

        assertEquals("CALENDARIOS_NACIONALES", resultado.fuente());
        assertEquals(2, resultado.diferenciasCalculoLocal());
        assertTrue(resultado.festivos().stream().anyMatch(f -> f.fecha().equals(LocalDate.of(ANIO, 12, 30))));
    }

    @Test
    void sin_calculo_local_registrado_no_hay_diferencias_que_contar() {
        final ResultadoObtencionFestivos resultado =
                servicio(new Proveedor("CALENDARIOS_NACIONALES", festivos(ANIO, 19))).obtener(ANIO);
        assertEquals(0, resultado.diferenciasCalculoLocal());
    }

    // NB-5 / SUPUESTO-B-22: dos festivos de la misma fecha, de cualquier fuente, se fusionan antes de validar.
    @Test
    void los_festivos_de_la_misma_fecha_se_fusionan_con_barra() {
        final List<FestivoColombia> conRepetida = new ArrayList<>(festivos(ANIO, 18));
        conRepetida.add(new FestivoColombia(LocalDate.of(ANIO, 1, 1), " Otro nombre "));
        conRepetida.add(new FestivoColombia(LocalDate.of(ANIO, 1, 1), "Festivo 1"));
        conRepetida.add(null);

        final ResultadoObtencionFestivos resultado =
                servicio(new Proveedor("CALENDARIOS_NACIONALES", conRepetida)).obtener(ANIO);

        assertEquals(18, resultado.festivos().size());
        assertEquals("Festivo 1 / Otro nombre", resultado.festivos().getFirst().nombre());
        assertEquals(List.of(), ObtenerFestivosConRespaldoService.fusionar(null));
    }

    @Test
    void si_todos_fallan_responde_el_calculo_local_aunque_no_este_en_el_orden() {
        when(catalogo.getParameter("FESTIVOS", "ORDEN_RESPALDO")).thenReturn(Optional.of(" feriados_io , , "));
        final ResultadoObtencionFestivos resultado = servicio(Proveedor.fallido("CALENDARIOS_NACIONALES"),
                Proveedor.fallido("FERIADOS_IO"), new Proveedor("CALCULO_LOCAL", festivos(ANIO, 19))).obtener(ANIO);
        assertEquals("CALCULO_LOCAL", resultado.fuente());
        assertTrue(resultado.usoRespaldo());
    }

    @Test
    void si_ningun_proveedor_entrega_un_conjunto_valido_falla_con_fes_010() {
        final InternalApplicationException error = assertThrows(InternalApplicationException.class,
                () -> servicio(Proveedor.fallido("CALENDARIOS_NACIONALES"), new Proveedor("CALCULO_LOCAL", festivos(ANIO, 2)))
                        .obtener(ANIO));
        assertEquals("FES_010", error.getCode());
    }

    @Test
    void sin_calculo_local_y_con_todos_los_proveedores_caidos_tambien_falla_con_fes_010() {
        assertEquals("FES_010", assertThrows(InternalApplicationException.class,
                () -> servicio(Proveedor.fallido("CALENDARIOS_NACIONALES")).obtener(ANIO)).getCode());
        assertTrue(new ResultadoObtencionFestivos("CALCULO_LOCAL", null, true, 0).festivos().isEmpty());
    }

    @Test
    void el_orden_lo_deciden_los_parametros_y_se_normaliza() {
        when(catalogo.getParameter("FESTIVOS", "PROVEEDOR_PRINCIPAL")).thenReturn(Optional.of(" feriados_io "));
        when(catalogo.getParameter("FESTIVOS", "ORDEN_RESPALDO")).thenReturn(Optional.of("CALENDARIOS_NACIONALES"));
        final Proveedor principal = new Proveedor("CALENDARIOS_NACIONALES", festivos(ANIO, 19));
        final Proveedor feriados = new Proveedor("FERIADOS_IO", festivos(ANIO, 19));

        final ResultadoObtencionFestivos resultado = servicio(principal, feriados).obtener(ANIO);

        assertEquals("FERIADOS_IO", resultado.fuente());
        assertFalse(resultado.usoRespaldo());
        assertEquals(0, principal.llamadas.get());
    }

    @Test
    void un_proveedor_desconocido_en_el_orden_se_salta_y_un_proveedor_sin_fuente_no_se_registra() {
        when(catalogo.getParameter("FESTIVOS", "PROVEEDOR_PRINCIPAL")).thenReturn(Optional.of("OTRO"));
        final Proveedor sinFuente = new Proveedor(null, festivos(ANIO, 19));
        final ResultadoObtencionFestivos resultado =
                servicio(sinFuente, new Proveedor("CALCULO_LOCAL", festivos(ANIO, 19))).obtener(ANIO);
        assertEquals("CALCULO_LOCAL", resultado.fuente());
        assertEquals(0, sinFuente.llamadas.get());
    }

    @Test
    void el_constructor_exige_proveedores_y_parametros() {
        assertThrows(NullPointerException.class, () -> new ObtenerFestivosConRespaldoService(null, parametros));
        assertThrows(NullPointerException.class, () -> new ObtenerFestivosConRespaldoService(List.of(), null));
    }

    private ObtenerFestivosConRespaldoService servicio(final CalendarioFestivosPort... proveedores) {
        return new ObtenerFestivosConRespaldoService(List.of(proveedores), parametros);
    }

    private static List<FestivoColombia> festivos(final int anio, final int cantidad) {
        final List<FestivoColombia> lista = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            lista.add(new FestivoColombia(LocalDate.of(anio, 1, 1).plusDays(i * 7L), "Festivo " + (i + 1)));
        }
        return lista;
    }

    private static final class Proveedor implements CalendarioFestivosPort {
        private final String fuente;
        private final List<FestivoColombia> festivos;
        private final boolean falla;
        private boolean disponible = true;
        private final AtomicInteger llamadas = new AtomicInteger();

        Proveedor(final String fuente, final List<FestivoColombia> festivos) {
            this(fuente, festivos, false);
        }

        private Proveedor(final String fuente, final List<FestivoColombia> festivos, final boolean falla) {
            this.fuente = fuente;
            this.festivos = festivos;
            this.falla = falla;
        }

        static Proveedor fallido(final String fuente) {
            return new Proveedor(fuente, List.of(), true);
        }

        @Override
        public String fuente() {
            return fuente;
        }

        @Override
        public boolean disponible() {
            return disponible;
        }

        @Override
        public List<FestivoColombia> obtenerFestivos(final int anio) {
            llamadas.incrementAndGet();
            if (falla) {
                throw new CalendarioFestivosException("caido");
            }
            return festivos;
        }
    }
}
