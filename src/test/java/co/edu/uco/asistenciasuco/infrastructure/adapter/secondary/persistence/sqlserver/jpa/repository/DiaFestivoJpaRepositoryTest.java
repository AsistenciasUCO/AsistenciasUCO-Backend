package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.AplicacionFestivos;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.CargaFestivos;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.PaginaFestivos;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.PaginaSesionesFestivo;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.RangoFechas;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvGrupoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvPeriodoAcademicoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CULM-001 B.1: adaptador JPA de festivos. Nombres y parametros exactos de los SP, JSON de dos claves, mapeo de filas y
 * traduccion de {@code DBCODE=FES_001} en las consultas.
 */
class DiaFestivoJpaRepositoryTest {

    private static final UUID ADMIN = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final List<FestivoColombia> FESTIVOS = List.of(
            new FestivoColombia(LocalDate.of(2026, 1, 1), "Año Nuevo"),
            new FestivoColombia(LocalDate.of(2026, 7, 13), "Nuestra Señora del Rosario de Chiquinquirá"));

    private final EntityManager entityManager = mock(EntityManager.class);
    private final JpaProcedureExecutor executor = mock(JpaProcedureExecutor.class);
    private final Query query = mock(Query.class);
    private final DiaFestivoJpaRepository repository = new DiaFestivoJpaRepository(entityManager, executor);
    private UUID correlacion;

    @BeforeEach
    void preparar() {
        correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void cargar_del_administrador_envia_el_json_de_dos_claves_y_el_usuario() {
        when(executor.executeWithExtras(eq("cargarDiasFestivos"), anyString(), anyMap(), eq(correlacion), eq(5)))
                .thenReturn(new ProcedureRow(null, List.of(2026, 19, 2, 17, 0)));

        final CargaFestivos carga = repository.cargar(2026, FESTIVOS, "CALENDARIOS_NACIONALES", 1, ADMIN);

        assertEquals(new CargaFestivos(2026, 19, 2, 17, 0), carga);
        final Map<String, Object> parametros = parametros("cargarDiasFestivos", DiaFestivoJpaRepository.SQL_CARGAR, 5);
        assertEquals(2026, parametros.get("anio"));
        assertEquals("[{\"fecha\":\"2026-01-01\",\"nombre\":\"Año Nuevo\"},"
                + "{\"fecha\":\"2026-07-13\",\"nombre\":\"Nuestra Señora del Rosario de Chiquinquirá\"}]", parametros.get("festivosJSON"));
        assertEquals("CALENDARIOS_NACIONALES", parametros.get("fuente"));
        assertEquals(1, parametros.get("diferenciasCalculoLocal"));
        assertEquals(correlacion, parametros.get("idCorrelacion"));
        assertEquals(ADMIN, parametros.get("idUsuarioEjecutor"));
    }

    @Test
    void cargar_automatico_no_envia_usuario_y_tolera_conteos_nulos() {
        when(executor.executeWithExtras(eq("cargarDiasFestivosAutomatico"), anyString(), anyMap(), eq(correlacion), eq(5)))
                .thenReturn(new ProcedureRow(null, Arrays.asList(2027, 19, 19, null, 0)));

        assertEquals(new CargaFestivos(2027, 19, 19, 0, 0), repository.cargarAutomatico(2027, null, "CALCULO_LOCAL", 0));
        final Map<String, Object> parametros = parametros("cargarDiasFestivosAutomatico",
                DiaFestivoJpaRepository.SQL_CARGAR_AUTOMATICO, 5);
        assertEquals("[]", parametros.get("festivosJSON"));
        assertEquals(false, parametros.containsKey("idUsuarioEjecutor"));
    }

    @Test
    void aplicar_por_anio_y_automatico_devuelven_los_conteos_del_sp() {
        when(executor.executeWithExtras(eq("aplicarFestivosPeriodo"), anyString(), anyMap(), eq(correlacion), eq(3)))
                .thenReturn(new ProcedureRow(null, List.of(12, 3, 1)));
        when(executor.executeWithExtras(eq("aplicarFestivosAutomatico"), anyString(), anyMap(), eq(correlacion), eq(3)))
                .thenReturn(new ProcedureRow(null, List.of(40, 9, 0)));

        assertEquals(new AplicacionFestivos(12, 3, 1), repository.aplicarAnio(2026, ADMIN));
        assertEquals(ADMIN, parametros("aplicarFestivosPeriodo", DiaFestivoJpaRepository.SQL_APLICAR_PERIODO, 3)
                .get("idUsuarioEjecutor"));
        assertEquals(new AplicacionFestivos(40, 9, 0), repository.aplicarAutomatico(2026));
        assertEquals(2026, parametros("aplicarFestivosAutomatico", DiaFestivoJpaRepository.SQL_APLICAR_AUTOMATICO, 3)
                .get("anio"));
    }

    @Test
    void contar_festivos_activos_lee_la_segunda_columna_y_cero_sin_filas() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{2026, 19, "CALENDARIOS_NACIONALES", null}));
        assertEquals(19, repository.contarFestivosActivos(2026));
        verify(entityManager).createNativeQuery(DiaFestivoJpaRepository.SQL_CARGA_ANIO);
        verify(query).setParameter("idCorrelacion", correlacion);

        when(query.getResultList()).thenReturn(List.of());
        assertEquals(0, repository.contarFestivosActivos(2030));
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{2026, null, null, null}));
        assertEquals(0, repository.contarFestivosActivos(2026));
    }

    @Test
    void consultar_festivos_mapea_fechas_y_la_ultima_carga() {
        final LocalDateTime carga = LocalDateTime.of(2026, 11, 7, 5, 0);
        when(query.getResultList()).thenReturn(List.<Object>of(
                new Object[]{UUID.randomUUID(), java.sql.Date.valueOf("2026-01-01"), 2026, "Año Nuevo", "CALENDARIOS_NACIONALES",
                        true, Timestamp.valueOf(carga), UUID.randomUUID(), "CALENDARIOS_NACIONALES", Timestamp.valueOf(carga), 0, 2},
                new Object[]{UUID.randomUUID(), LocalDate.of(2026, 3, 23), 2026, "San José", "CALCULO_LOCAL",
                        false, carga, UUID.randomUUID(), "CALENDARIOS_NACIONALES", carga, 0, 2}));

        final PaginaFestivos pagina = repository.consultarFestivos(2026, true, 1, 100, ADMIN);

        assertEquals(2, pagina.festivos().size());
        assertEquals(LocalDate.of(2026, 1, 1), pagina.festivos().getFirst().fecha());
        assertEquals(true, pagina.festivos().getFirst().estaActivo());
        assertEquals(false, pagina.festivos().get(1).estaActivo());
        assertEquals("CALENDARIOS_NACIONALES", pagina.fuenteUltimaCarga());
        assertEquals(carga, pagina.fechaUltimaCargaUtc());
        assertEquals(0, pagina.diferenciasCalculoLocal());
        assertEquals(2, pagina.totalRegistros());
        verify(query).setParameter("incluirInactivos", true);
        verify(query).setParameter("idUsuarioEjecutor", ADMIN);
    }

    @Test
    void consultar_festivos_sin_filas_o_sin_carga_previa() {
        when(query.getResultList()).thenReturn(List.of());
        final PaginaFestivos vacia = repository.consultarFestivos(2030, false, 1, 100, ADMIN);
        assertEquals(0, vacia.totalRegistros());
        assertNull(vacia.fuenteUltimaCarga());

        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{UUID.randomUUID(), "2026-12-25", 2026, "Navidad",
                "CALCULO_LOCAL", null, "2026-11-07 05:00:00", UUID.randomUUID(), null, null, null, 1}));
        final PaginaFestivos sinCarga = repository.consultarFestivos(2026, false, 1, 100, ADMIN);
        assertEquals(LocalDate.of(2026, 12, 25), sinCarga.festivos().getFirst().fecha());
        assertNull(sinCarga.diferenciasCalculoLocal());
        assertNull(sinCarga.fechaUltimaCargaUtc());
    }

    @Test
    void sesiones_con_asistencia_mapea_la_pagina_y_su_total() {
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.<Object>of(new Object[]{sesion.toString(), grupo, "G01 de Calculo",
                "Calculo", "Ana Docente", new java.util.Date(0), LocalDateTime.of(2026, 3, 23, 0, 0), "San José", 0, 30, 7},
                new Object[]{sesion, grupo, "G01 de Calculo", "Calculo", "Ana Docente", "2026-03-23 13:00:00",
                        "2026-03-23", "San José", null, null, 7},
                new Object[]{sesion, grupo, "G01 de Calculo", "Calculo", "Ana Docente", Timestamp.valueOf("2026-03-23 13:00:00"),
                        LocalDate.of(2026, 3, 23), "San José", 1, 2, 7}));

        final PaginaSesionesFestivo pagina = repository.consultarSesionesConAsistencia(2026, 1, 20, ADMIN);

        assertEquals(7, pagina.totalRegistros());
        assertEquals(sesion, pagina.elementos().getFirst().idSesion());
        assertEquals(LocalDate.of(2026, 3, 23), pagina.elementos().getFirst().fecha());
        assertEquals(30, pagina.elementos().getFirst().totalRegistros());
        assertEquals(LocalDateTime.of(2026, 3, 23, 13, 0), pagina.elementos().get(1).fechaHoraInicioUtc());
        assertEquals(0, pagina.elementos().get(1).totalTomas());
        assertEquals(LocalDateTime.of(2026, 3, 23, 13, 0), pagina.elementos().get(2).fechaHoraInicioUtc());
        verify(entityManager).createNativeQuery(DiaFestivoJpaRepository.SQL_SESIONES_CON_ASISTENCIA);

        when(query.getResultList()).thenReturn(List.of());
        assertEquals(0, repository.consultarSesionesConAsistencia(2026, 2, 20, ADMIN).totalRegistros());
    }

    @Test
    void el_dbcode_fes_001_de_las_consultas_es_un_400() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=FES_001|anio fuera de rango"));
        final ValidationException error = assertThrows(ValidationException.class,
                () -> repository.consultarFestivos(1800, false, 1, 100, ADMIN));
        assertEquals("FES_001", error.getCode());
    }

    @Test
    void un_fallo_tecnico_de_la_consulta_es_un_error_de_base_de_datos() {
        when(query.getResultList()).thenThrow(new PersistenceException("conexion"));
        assertThrows(DatabaseOperationException.class, () -> repository.contarFestivosActivos(2026));
    }

    @Test
    void rango_de_periodo_y_de_grupo_salen_de_las_vistas() throws Exception {
        @SuppressWarnings("unchecked")
        final TypedQuery<UvPeriodoAcademicoEntity> periodos = mock(TypedQuery.class);
        @SuppressWarnings("unchecked")
        final TypedQuery<UvGrupoEntity> grupos = mock(TypedQuery.class);
        when(entityManager.createQuery(DiaFestivoJpaRepository.HQL_RANGO_PERIODO, UvPeriodoAcademicoEntity.class)).thenReturn(periodos);
        when(entityManager.createQuery(DiaFestivoJpaRepository.HQL_RANGO_GRUPO, UvGrupoEntity.class)).thenReturn(grupos);
        when(periodos.setParameter(anyString(), any())).thenReturn(periodos);
        when(periodos.setMaxResults(anyInt())).thenReturn(periodos);
        when(grupos.setParameter(anyString(), any())).thenReturn(grupos);
        when(grupos.setMaxResults(anyInt())).thenReturn(grupos);
        final UvPeriodoAcademicoEntity periodo = instancia(UvPeriodoAcademicoEntity.class);
        campo(periodo, "fechaInicio", LocalDate.of(2026, 8, 1));
        campo(periodo, "fechaFin", LocalDate.of(2027, 1, 31));
        final UvGrupoEntity grupo = instancia(UvGrupoEntity.class);
        campo(grupo, "fechaInicioPeriodoAcademico", LocalDate.of(2026, 1, 15));
        campo(grupo, "fechaFinPeriodoAcademico", LocalDate.of(2026, 6, 15));
        when(periodos.getResultList()).thenReturn(List.of(periodo));
        when(grupos.getResultList()).thenReturn(List.of(grupo), List.of());

        assertEquals(Optional.of(new RangoFechas(LocalDate.of(2026, 8, 1), LocalDate.of(2027, 1, 31))),
                repository.consultarRangoPeriodo(UUID.randomUUID()));
        assertEquals(Optional.of(new RangoFechas(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 6, 15))),
                repository.consultarRangoPeriodoDeGrupo(UUID.randomUUID()));
        assertEquals(Optional.empty(), repository.consultarRangoPeriodoDeGrupo(UUID.randomUUID()));
        assertThrows(NullPointerException.class, () -> repository.consultarRangoPeriodo(null));
        assertThrows(NullPointerException.class, () -> repository.consultarRangoPeriodoDeGrupo(null));
    }

    @Test
    void el_constructor_exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new DiaFestivoJpaRepository(null, executor));
        assertThrows(NullPointerException.class, () -> new DiaFestivoJpaRepository(entityManager, null));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parametros(final String operacion, final String sql, final int extras) {
        final ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(executor).executeWithExtras(eq(operacion), eq(sql), captor.capture(), eq(correlacion), eq(extras));
        return captor.getValue();
    }

    private static <T> T instancia(final Class<T> tipo) throws ReflectiveOperationException {
        final var constructor = tipo.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static void campo(final Object objeto, final String nombre, final Object valor) throws ReflectiveOperationException {
        final var field = objeto.getClass().getDeclaredField(nombre);
        field.setAccessible(true);
        field.set(objeto, valor);
    }
}
