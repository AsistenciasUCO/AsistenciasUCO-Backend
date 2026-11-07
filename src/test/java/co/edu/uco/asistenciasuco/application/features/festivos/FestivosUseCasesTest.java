package co.edu.uco.asistenciasuco.application.features.festivos;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.primaryports.AsegurarFestivosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.usecase.impl.AsegurarFestivosPeriodoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.festivos.cargarfestivosautomatico.usecase.impl.CargarFestivosAutomaticoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.festivos.common.CargaAnualFestivosService;
import co.edu.uco.asistenciasuco.application.features.festivos.common.FestivosParametros;
import co.edu.uco.asistenciasuco.application.features.festivos.common.ObtenerFestivosConRespaldoService;
import co.edu.uco.asistenciasuco.application.features.festivos.common.ResultadoObtencionFestivos;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarfestivos.primaryports.ConsultarFestivosAnioInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarfestivos.usecase.impl.ConsultarFestivosAnioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarsesionesfestivo.primaryports.ConsultarSesionesFestivoConAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarsesionesfestivo.usecase.impl.ConsultarSesionesFestivoConAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.festivos.recargarfestivos.primaryports.RecargarFestivosAnioInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.recargarfestivos.usecase.impl.RecargarFestivosAnioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.AplicacionFestivos;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.CargaFestivos;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.FestivoRegistrado;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.PaginaFestivos;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.PaginaSesionesFestivo;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.RangoFechas;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.SesionFestivoConAsistencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CULM-001 B.1 (CONTRACT 3.2.2, 3.2.3, 3.4.5): casos de uso de festivos (100 % de lineas y ramas): carga anual si falta,
 * job, asegurar festivos del periodo, recarga, consulta y sesiones en festivo con asistencia.
 */
class FestivosUseCasesTest {

    private static final UUID ADMIN = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final List<FestivoColombia> FESTIVOS = List.of(new FestivoColombia(LocalDate.of(2026, 1, 1), "Año Nuevo"));
    private static final ResultadoObtencionFestivos OBTENIDOS =
            new ResultadoObtencionFestivos("CALENDARIOS_NACIONALES", FESTIVOS, false, 0);

    private final ParameterCatalogPort catalogo = mock(ParameterCatalogPort.class);
    private final FestivosParametros parametros = new FestivosParametros(catalogo);
    private final DiaFestivoRepositoryPort repositorio = mock(DiaFestivoRepositoryPort.class);
    private final ObtenerFestivosConRespaldoService obtener = mock(ObtenerFestivosConRespaldoService.class);
    private final CargaAnualFestivosService cargaAnual = new CargaAnualFestivosService(obtener, repositorio);

    @BeforeEach
    void catalogoVacio() {
        when(catalogo.getParameter(anyString(), anyString())).thenReturn(Optional.empty());
        when(obtener.obtener(anyInt())).thenReturn(OBTENIDOS);
        when(repositorio.cargarAutomatico(anyInt(), org.mockito.ArgumentMatchers.anyList(), anyString(), anyInt()))
                .thenReturn(new CargaFestivos(2026, 19, 19, 0, 0));
        when(repositorio.aplicarAutomatico(anyInt())).thenReturn(new AplicacionFestivos(4, 2, 1));
    }

    // ---------------- CargaAnualFestivosService ----------------

    @Test
    void carga_anual_no_recarga_un_anio_ya_cargado() {
        when(repositorio.contarFestivosActivos(2026)).thenReturn(19);
        assertFalse(cargaAnual.cargarSiFalta(2026));
        verifyNoInteractions(obtener);
        verify(repositorio, never()).cargarAutomatico(anyInt(), org.mockito.ArgumentMatchers.anyList(), anyString(), anyInt());
    }

    @Test
    void carga_anual_obtiene_carga_y_aplica_en_ese_orden() {
        when(repositorio.contarFestivosActivos(2027)).thenReturn(0);
        assertTrue(cargaAnual.cargarSiFalta(2027));
        final InOrder orden = inOrder(obtener, repositorio);
        orden.verify(obtener).obtener(2027);
        orden.verify(repositorio).cargarAutomatico(2027, FESTIVOS, "CALENDARIOS_NACIONALES", 0);
        orden.verify(repositorio).aplicarAutomatico(2027);
        assertThrows(NullPointerException.class, () -> new CargaAnualFestivosService(null, repositorio));
        assertThrows(NullPointerException.class, () -> new CargaAnualFestivosService(obtener, null));
    }

    // ---------------- CargarFestivosAutomaticoUseCaseImpl (job) ----------------

    @Test
    void el_job_carga_el_anio_actual_y_los_siguientes_que_faltan() {
        final Clock reloj = Clock.fixed(Instant.parse("2026-12-31T23:30:00Z"), ZoneId.of("America/Bogota"));
        when(repositorio.contarFestivosActivos(2026)).thenReturn(19);
        when(repositorio.contarFestivosActivos(2027)).thenReturn(0);

        final int cargados = new CargarFestivosAutomaticoUseCaseImpl(parametros, cargaAnual, reloj).execute();

        assertEquals(1, cargados);
        verify(obtener).obtener(2027);
        verify(obtener, never()).obtener(2026);
    }

    @Test
    void el_job_respeta_el_interruptor_y_no_falla_si_un_anio_falla() {
        final Clock reloj = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneId.of("America/Bogota"));
        when(catalogo.getParameter("FESTIVOS", "HABILITADO")).thenReturn(Optional.of("false"));
        assertEquals(0, new CargarFestivosAutomaticoUseCaseImpl(parametros, cargaAnual, reloj).execute());
        verifyNoInteractions(repositorio);

        when(catalogo.getParameter("FESTIVOS", "HABILITADO")).thenReturn(Optional.of("true"));
        when(catalogo.getParameter("FESTIVOS", "ANIOS_ADELANTE")).thenReturn(Optional.of("-3"));
        when(repositorio.contarFestivosActivos(2026)).thenThrow(new IllegalStateException("db caida"));
        assertEquals(0, new CargarFestivosAutomaticoUseCaseImpl(parametros, cargaAnual, reloj).execute());
        verify(repositorio, never()).contarFestivosActivos(2027);
    }

    @Test
    void el_job_exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new CargarFestivosAutomaticoUseCaseImpl(null, cargaAnual, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new CargarFestivosAutomaticoUseCaseImpl(parametros, null, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new CargarFestivosAutomaticoUseCaseImpl(parametros, cargaAnual, null));
    }

    // ---------------- AsegurarFestivosPeriodoUseCaseImpl ----------------

    @Test
    void asegurar_carga_cada_anio_del_periodo_que_falta() {
        final UUID periodo = UUID.randomUUID();
        when(repositorio.consultarRangoPeriodo(periodo))
                .thenReturn(Optional.of(new RangoFechas(LocalDate.of(2026, 8, 1), LocalDate.of(2027, 1, 31))));
        when(repositorio.contarFestivosActivos(2026)).thenReturn(19);
        when(repositorio.contarFestivosActivos(2027)).thenReturn(0);

        asegurar().execute(AsegurarFestivosPeriodoInputPort.Comando.dePeriodo(periodo));

        verify(obtener).obtener(2027);
        verify(obtener, never()).obtener(2026);
    }

    @Test
    void asegurar_por_grupo_usa_el_periodo_del_grupo_y_salta_anios_fuera_de_rango() {
        final UUID grupo = UUID.randomUUID();
        when(repositorio.consultarRangoPeriodoDeGrupo(grupo))
                .thenReturn(Optional.of(new RangoFechas(LocalDate.of(2100, 12, 1), LocalDate.of(2101, 1, 31))));
        when(repositorio.contarFestivosActivos(2100)).thenReturn(0);

        asegurar().execute(AsegurarFestivosPeriodoInputPort.Comando.deGrupo(grupo));

        verify(obtener).obtener(2100);
        verify(repositorio, never()).contarFestivosActivos(2101);
    }

    @Test
    void asegurar_nunca_propaga_fallos_ni_actua_sin_datos() {
        final UUID periodo = UUID.randomUUID();
        asegurar().execute(null);
        asegurar().execute(new AsegurarFestivosPeriodoInputPort.Comando(null, null));

        when(repositorio.consultarRangoPeriodo(periodo)).thenThrow(new IllegalStateException("db"));
        asegurar().execute(AsegurarFestivosPeriodoInputPort.Comando.dePeriodo(periodo));

        final UUID otro = UUID.randomUUID();
        when(repositorio.consultarRangoPeriodo(otro))
                .thenReturn(Optional.of(new RangoFechas(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30))));
        when(repositorio.contarFestivosActivos(2026)).thenReturn(0);
        when(obtener.obtener(2026)).thenThrow(new IllegalStateException("proveedores"));
        asegurar().execute(AsegurarFestivosPeriodoInputPort.Comando.dePeriodo(otro));

        final UUID invertido = UUID.randomUUID();
        when(repositorio.consultarRangoPeriodo(invertido))
                .thenReturn(Optional.of(new RangoFechas(LocalDate.of(2027, 1, 1), LocalDate.of(2026, 1, 1))));
        asegurar().execute(AsegurarFestivosPeriodoInputPort.Comando.dePeriodo(invertido));
        final UUID sinInicio = UUID.randomUUID();
        when(repositorio.consultarRangoPeriodo(sinInicio)).thenReturn(Optional.of(new RangoFechas(null, LocalDate.of(2026, 1, 1))));
        asegurar().execute(AsegurarFestivosPeriodoInputPort.Comando.dePeriodo(sinInicio));
        final UUID sinFin = UUID.randomUUID();
        when(repositorio.consultarRangoPeriodo(sinFin)).thenReturn(Optional.of(new RangoFechas(LocalDate.of(2026, 1, 1), null)));
        asegurar().execute(AsegurarFestivosPeriodoInputPort.Comando.dePeriodo(sinFin));

        verify(repositorio, never()).cargarAutomatico(anyInt(), org.mockito.ArgumentMatchers.anyList(), anyString(), anyInt());
    }

    @Test
    void asegurar_respeta_el_interruptor() {
        when(catalogo.getParameter("FESTIVOS", "HABILITADO")).thenReturn(Optional.of("false"));
        asegurar().execute(AsegurarFestivosPeriodoInputPort.Comando.dePeriodo(UUID.randomUUID()));
        verifyNoInteractions(repositorio);
        assertThrows(NullPointerException.class, () -> new AsegurarFestivosPeriodoUseCaseImpl(null, repositorio, cargaAnual));
        assertThrows(NullPointerException.class, () -> new AsegurarFestivosPeriodoUseCaseImpl(parametros, null, cargaAnual));
        assertThrows(NullPointerException.class, () -> new AsegurarFestivosPeriodoUseCaseImpl(parametros, repositorio, null));
    }

    // ---------------- RecargarFestivosAnioUseCaseImpl ----------------

    @Test
    void recargar_obtiene_carga_como_administrador_y_aplica_al_anio() {
        final ResultadoObtencionFestivos respaldo = new ResultadoObtencionFestivos("FERIADOS_IO", FESTIVOS, true, 2);
        when(obtener.obtener(2026)).thenReturn(respaldo);
        when(repositorio.cargar(2026, FESTIVOS, "FERIADOS_IO", 2, ADMIN)).thenReturn(new CargaFestivos(2026, 19, 0, 19, 1));
        when(repositorio.aplicarAnio(2026, ADMIN)).thenReturn(new AplicacionFestivos(12, 3, 1));

        final RecargarFestivosAnioInputPort.Resultado resultado =
                recargar().execute(new RecargarFestivosAnioInputPort.Comando(2026, ADMIN));

        assertEquals(new RecargarFestivosAnioInputPort.Resultado(2026, "FERIADOS_IO", true, 19, 0, 19, 1, 2,
                new RecargarFestivosAnioInputPort.Aplicacion(12, 3, 1)), resultado);
        final InOrder orden = inOrder(obtener, repositorio);
        orden.verify(obtener).obtener(2026);
        orden.verify(repositorio).cargar(2026, FESTIVOS, "FERIADOS_IO", 2, ADMIN);
        orden.verify(repositorio).aplicarAnio(2026, ADMIN);
    }

    @Test
    void recargar_valida_el_anio_antes_de_llamar_a_los_proveedores() {
        assertEquals("FES_001", assertThrows(ValidationException.class,
                () -> recargar().execute(new RecargarFestivosAnioInputPort.Comando(1999, ADMIN))).getCode());
        verifyNoInteractions(obtener, repositorio);
        assertThrows(NullPointerException.class, () -> recargar().execute(null));
        assertThrows(NullPointerException.class, () -> new RecargarFestivosAnioUseCaseImpl(null, obtener, repositorio));
        assertThrows(NullPointerException.class, () -> new RecargarFestivosAnioUseCaseImpl(parametros, null, repositorio));
        assertThrows(NullPointerException.class, () -> new RecargarFestivosAnioUseCaseImpl(parametros, obtener, null));
    }

    @Test
    void recargar_propaga_el_fallo_de_la_aplicacion() {
        when(repositorio.cargar(anyInt(), org.mockito.ArgumentMatchers.anyList(), anyString(), anyInt(),
                org.mockito.ArgumentMatchers.any())).thenReturn(new CargaFestivos(2026, 19, 19, 0, 0));
        doThrow(new IllegalStateException("FES_009")).when(repositorio).aplicarAnio(2026, ADMIN);
        assertThrows(IllegalStateException.class, () -> recargar().execute(new RecargarFestivosAnioInputPort.Comando(2026, ADMIN)));
    }

    // ---------------- ConsultarFestivosAnioUseCaseImpl ----------------

    @Test
    void consultar_entrega_los_festivos_la_ultima_carga_y_la_atribucion_de_los_parametros() {
        final LocalDateTime carga = LocalDateTime.of(2026, 11, 7, 5, 0);
        when(repositorio.consultarFestivos(2026, true, 1, 100, ADMIN)).thenReturn(new PaginaFestivos(
                List.of(new FestivoRegistrado(LocalDate.of(2026, 1, 1), "Año Nuevo", "CALENDARIOS_NACIONALES", true)),
                1, "CALENDARIOS_NACIONALES", carga, 0));
        when(catalogo.getParameter("FESTIVOS", "ATRIBUCION_URL")).thenReturn(Optional.of("https://otra.example"));

        final ConsultarFestivosAnioInputPort.Resultado resultado = new ConsultarFestivosAnioUseCaseImpl(parametros, repositorio)
                .execute(new ConsultarFestivosAnioInputPort.Comando(2026, true, ADMIN));

        assertEquals(2026, resultado.anio());
        assertEquals("CALENDARIOS_NACIONALES", resultado.fuenteUltimaCarga());
        assertEquals(carga, resultado.fechaUltimaCargaUtc());
        assertEquals(0, resultado.diferenciasCalculoLocal());
        assertEquals("Fuente: Calendarios Nacionales", resultado.atribucionTexto());
        assertEquals("https://otra.example", resultado.atribucionUrl());
        assertEquals(List.of(new ConsultarFestivosAnioInputPort.Festivo(LocalDate.of(2026, 1, 1), "Año Nuevo",
                "CALENDARIOS_NACIONALES", true)), resultado.festivos());
        assertEquals(1, resultado.totalFestivos());
    }

    @Test
    void consultar_sin_carga_devuelve_lista_vacia_y_valida_el_anio() {
        when(repositorio.consultarFestivos(2030, false, 1, 100, ADMIN)).thenReturn(new PaginaFestivos(null, 0, null, null, null));
        final var consultar = new ConsultarFestivosAnioUseCaseImpl(parametros, repositorio);
        final ConsultarFestivosAnioInputPort.Resultado resultado =
                consultar.execute(new ConsultarFestivosAnioInputPort.Comando(2030, false, ADMIN));
        assertTrue(resultado.festivos().isEmpty());
        assertNull(resultado.fuenteUltimaCarga());
        assertEquals("FES_001", assertThrows(ValidationException.class,
                () -> consultar.execute(new ConsultarFestivosAnioInputPort.Comando(3000, false, ADMIN))).getCode());
        assertThrows(NullPointerException.class, () -> consultar.execute(null));
        assertThrows(NullPointerException.class, () -> new ConsultarFestivosAnioUseCaseImpl(null, repositorio));
        assertThrows(NullPointerException.class, () -> new ConsultarFestivosAnioUseCaseImpl(parametros, null));
        assertTrue(new ConsultarFestivosAnioInputPort.Resultado(2030, null, null, null, null, null, null, 0).festivos().isEmpty());
    }

    // ---------------- ConsultarSesionesFestivoConAsistenciaUseCaseImpl ----------------

    @Test
    void sesiones_en_festivo_usa_la_pagina_por_defecto_y_mapea_las_filas() {
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 3, 23, 13, 0);
        doReturn(new PaginaSesionesFestivo(List.of(new SesionFestivoConAsistencia(sesion, grupo, "G01 de Calculo", "Calculo",
                "Ana Docente", inicio, LocalDate.of(2026, 3, 23), "San José", 1, 30)), 21))
                .when(repositorio).consultarSesionesConAsistencia(2026, 1, 20, ADMIN);
        final var consultar = new ConsultarSesionesFestivoConAsistenciaUseCaseImpl(parametros, repositorio);

        final ConsultarSesionesFestivoConAsistenciaInputPort.Resultado resultado =
                consultar.execute(new ConsultarSesionesFestivoConAsistenciaInputPort.Comando(2026, null, null, ADMIN));

        assertEquals(1, resultado.numeroPagina());
        assertEquals(20, resultado.tamanoPagina());
        assertEquals(21, resultado.totalElementos());
        assertEquals(new ConsultarSesionesFestivoConAsistenciaInputPort.SesionFestivo(sesion, grupo, "G01 de Calculo", "Calculo",
                "Ana Docente", inicio, LocalDate.of(2026, 3, 23), "San José", 1, 30), resultado.elementos().getFirst());
    }

    @Test
    void sesiones_en_festivo_respeta_la_pagina_pedida_y_valida_el_anio() {
        doReturn(new PaginaSesionesFestivo(null, 0)).when(repositorio).consultarSesionesConAsistencia(2026, 3, 5, ADMIN);
        final var consultar = new ConsultarSesionesFestivoConAsistenciaUseCaseImpl(parametros, repositorio);
        final var resultado = consultar.execute(new ConsultarSesionesFestivoConAsistenciaInputPort.Comando(2026, 3, 5, ADMIN));
        assertEquals(3, resultado.numeroPagina());
        assertEquals(5, resultado.tamanoPagina());
        assertTrue(resultado.elementos().isEmpty());
        assertTrue(new ConsultarSesionesFestivoConAsistenciaInputPort.Resultado(null, 1, 1, 0).elementos().isEmpty());
        assertThrows(ValidationException.class,
                () -> consultar.execute(new ConsultarSesionesFestivoConAsistenciaInputPort.Comando(1900, 1, 1, ADMIN)));
        assertThrows(NullPointerException.class, () -> consultar.execute(null));
        assertThrows(NullPointerException.class, () -> new ConsultarSesionesFestivoConAsistenciaUseCaseImpl(null, repositorio));
        assertThrows(NullPointerException.class, () -> new ConsultarSesionesFestivoConAsistenciaUseCaseImpl(parametros, null));
    }

    private AsegurarFestivosPeriodoUseCaseImpl asegurar() {
        return new AsegurarFestivosPeriodoUseCaseImpl(parametros, repositorio, cargaAnual);
    }

    private RecargarFestivosAnioUseCaseImpl recargar() {
        return new RecargarFestivosAnioUseCaseImpl(parametros, obtener, repositorio);
    }
}
