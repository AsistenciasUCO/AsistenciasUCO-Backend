package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU094/HU095: EXEC del historial del docente (HR-01..HR-06) y del consolidado por tipo de vinculacion (RR-01..RR-03). */
class DocenteProgramaReporteJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DOCENTE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private DocenteProgramaReporteJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new DocenteProgramaReporteJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] fila(final UUID grupo, final Object inicio, final String estado) {
        return new Object[]{grupo, 7, "Grupo 7 ", UUID.randomUUID(), "Algebra", PERIODO, 20261, "2026-1", inicio,
                LocalDate.of(2026, 6, 10), estado, 30, 25, 1, 2, 3, 4, 2, 1, 25, 4L};
    }

    @Test
    void HR01_ejecuta_el_sp_con_parametros_nombrados_y_mapea_filas_y_resumen() {
        final UUID grupo = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.<Object>of(
                fila(grupo, LocalDate.of(2026, 1, 20), "VIGENTE"),
                fila(UUID.randomUUID(), LocalDate.of(2025, 8, 1), "FINALIZADO")));

        final var pagina = repository.consultarHistorial(ACTOR, DOCENTE, PERIODO, 2, 5);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_historial_grupos_docente_programa"));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idDocente", DOCENTE);
        verify(query).setParameter("idPeriodoAcademico", PERIODO);
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
        assertEquals(2, pagina.grupos().size());
        assertEquals(4, pagina.total());
        assertEquals(4, pagina.totalGrupos());
        assertEquals(2, pagina.totalPeriodos());
        assertEquals(1, pagina.gruposVigentes());
        assertEquals(25, pagina.estudiantesActivosVigentes());
        final var primero = pagina.grupos().getFirst();
        assertEquals(grupo, primero.idGrupo());
        assertEquals(7, primero.codigoGrupo());
        assertEquals("Grupo 7", primero.nombreGrupo());
        assertEquals(PERIODO, primero.idPeriodoAcademico());
        assertEquals(20261, primero.codigoPeriodoAcademico());
        assertEquals(LocalDate.of(2026, 1, 20), primero.fechaInicioPeriodo());
        assertEquals(LocalDate.of(2026, 6, 10), primero.fechaFinPeriodo());
        assertEquals("VIGENTE", primero.estadoPeriodo());
        assertEquals(30, primero.capacidadMaxima());
        assertEquals(25, primero.estudiantesActivos());
        assertEquals(1, primero.estudiantesFinalizados());
        assertEquals(2, primero.estudiantesCanceladosVoluntad());
        assertEquals(3, primero.estudiantesCanceladosInasistencia());
    }

    @Test
    void HR02_sin_filas_devuelve_pagina_vacia_con_resumen_en_cero() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final var pagina = repository.consultarHistorial(ACTOR, DOCENTE, null, 1, 10);

        assertTrue(pagina.grupos().isEmpty());
        assertEquals(0, pagina.total());
        assertEquals(0, pagina.totalGrupos());
        verify(query).setParameter("idPeriodoAcademico", null);
    }

    @Test
    void HR03_ejecutor_nulo_no_llega_a_la_db() {
        assertThrows(CrosscuttingException.class, () -> repository.consultarHistorial(null, DOCENTE, null, 1, 10));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void HR04_fallo_tecnico_se_traduce() {
        when(query.getResultList()).thenThrow(new PersistenceException("timeout"));

        assertThrows(DatabaseOperationException.class, () -> repository.consultarHistorial(ACTOR, DOCENTE, null, 1, 10));
    }

    @Test
    void HR04b_docente_fuera_del_programa_del_coordinador_es_404_no_500() {
        // Certificación Fase 3 (HU094): GEN_001 del SP llegaba como DatabaseOperationException (500).
        when(query.getResultList()).thenThrow(new PersistenceException(
                "DBCODE=GEN_001|Error de existencia: No se encontró el registro para la entidad [Docente]."));

        assertThrows(co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException.class,
                () -> repository.consultarHistorial(ACTOR, DOCENTE, null, 1, 10));
    }

    @Test
    void HR05_fecha_acepta_los_tipos_que_entrega_el_driver() {
        final LocalDate esperada = LocalDate.of(2026, 3, 15);
        assertEquals(esperada, DocenteProgramaReporteJpaRepository.fecha(esperada));
        assertEquals(esperada, DocenteProgramaReporteJpaRepository.fecha(LocalDateTime.of(2026, 3, 15, 0, 0)));
        assertEquals(esperada, DocenteProgramaReporteJpaRepository.fecha(
                Date.from(esperada.atStartOfDay(ZoneId.systemDefault()).toInstant())));
        assertNull(DocenteProgramaReporteJpaRepository.fecha(null));
        assertNull(DocenteProgramaReporteJpaRepository.fecha("2026-03-15"));
    }

    @Test
    void HR06_columnas_nulas_se_mapean_con_valores_seguros() {
        final Object[] fila = fila(UUID.randomUUID(), null, "PROXIMO");
        fila[1] = null;
        fila[2] = null;
        fila[12] = null;
        when(query.getResultList()).thenReturn(Collections.singletonList(fila));

        final var g = repository.consultarHistorial(ACTOR, DOCENTE, null, 1, 10).grupos().getFirst();

        assertNull(g.codigoGrupo());
        assertNull(g.nombreGrupo());
        assertNull(g.fechaInicioPeriodo());
        assertEquals(0, g.estudiantesActivos());
    }

    @Test
    void RR01_resumen_ejecuta_el_sp_y_mapea_por_tipo_de_vinculacion() {
        final UUID tipo = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.<Object>of(
                new Object[]{tipo, "CAT ", "Catedra", 2, 1},
                new Object[]{UUID.randomUUID(), "OCA", "Ocasional", null, 0}));

        final var resumen = repository.consultarResumen(ACTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_resumen_docentes_programa"));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        assertEquals(2, resumen.size());
        assertEquals(tipo, resumen.getFirst().idTipoVinculacion());
        assertEquals("CAT", resumen.getFirst().codigoTipoVinculacion());
        assertEquals(2, resumen.getFirst().totalActivos());
        assertEquals(1, resumen.getFirst().totalInactivos());
        assertEquals(0, resumen.get(1).totalActivos());
    }

    @Test
    void RR01b_resumen_acepta_uuid_entregado_como_string_por_sql_server() {
        final UUID tipo = UUID.randomUUID();
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{tipo.toString(), "CAT ", "Catedra", 2, 1}));

        final var resumen = repository.consultarResumen(ACTOR);

        assertEquals(tipo, resumen.getFirst().idTipoVinculacion());
    }

    @Test
    void RR02_resumen_con_ejecutor_nulo_no_llega_a_la_db() {
        assertThrows(CrosscuttingException.class, () -> repository.consultarResumen(null));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void RR03_resumen_fallo_tecnico_se_traduce() {
        when(query.getResultList()).thenThrow(new PersistenceException("timeout"));

        assertThrows(DatabaseOperationException.class, () -> repository.consultarResumen(ACTOR));
    }

    @Test
    void HR07_RR04_uniqueidentifier_como_texto_del_driver_sqlserver() {
        final UUID grupo = UUID.randomUUID();
        final Object[] historial = fila(null, LocalDate.of(2026, 1, 20), "VIGENTE");
        historial[0] = grupo.toString().toUpperCase();
        historial[3] = UUID.randomUUID().toString();
        historial[5] = PERIODO.toString();
        when(query.getResultList()).thenReturn(Collections.singletonList(historial));

        final var g = repository.consultarHistorial(ACTOR, DOCENTE, null, 1, 10).grupos().getFirst();
        assertEquals(grupo, g.idGrupo());
        assertEquals(PERIODO, g.idPeriodoAcademico());

        final UUID tipo = UUID.randomUUID();
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{tipo.toString(), "CAT", "Catedra", 1, 0}));
        assertEquals(tipo, repository.consultarResumen(ACTOR).getFirst().idTipoVinculacion());
    }

    @Test
    void constructor_rechaza_entity_manager_nulo() {
        assertThrows(NullPointerException.class, () -> new DocenteProgramaReporteJpaRepository(null));
    }
}
