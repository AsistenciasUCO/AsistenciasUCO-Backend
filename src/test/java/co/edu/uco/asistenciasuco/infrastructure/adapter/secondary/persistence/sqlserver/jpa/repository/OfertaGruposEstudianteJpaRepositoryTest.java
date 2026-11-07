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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

/**
 * HU008: EXEC de la oferta de grupos con binding nombrado, correlacion y mapeo de columnas (OR-01..OR-09). CULM-001 A.1:
 * el resultset pasa de 21 a 19 columnas (sin idSolicitudAbierta ni codigoEstadoSolicitud; HU053-HU055 descartadas).
 */
class OfertaGruposEstudianteJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private OfertaGruposEstudianteJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new OfertaGruposEstudianteJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] fila(final UUID grupo, final Object inicio, final Object matriculado) {
        return new Object[]{grupo, 7, "Grupo 7 ", UUID.randomUUID(), "MAT1", "Algebra", "Ana Perez", UUID.randomUUID(), "2026-2",
                inicio, LocalDate.of(2026, 12, 10), "VIGENTE", "Lunes 08:00-10:00", 30, 25, 5, matriculado, "INS", 3};
    }

    @Test
    void OR01_ejecuta_el_sp_con_parametros_nombrados_y_mapea_las_columnas() {
        final UUID grupo = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.<Object>of(
                fila(grupo, LocalDate.of(2026, 7, 20), false),
                fila(UUID.randomUUID(), LocalDate.of(2026, 7, 20), true)));

        final var pagina = repository.consultarOferta(ACTOR, "algebra", 2, 5);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_oferta_grupos_estudiante"));
        verify(query).setParameter("busqueda", "algebra");
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        assertEquals(3, pagina.total());
        assertEquals(2, pagina.grupos().size());
        final var g = pagina.grupos().getFirst();
        assertEquals(grupo, g.idGrupo());
        assertEquals(7, g.codigoGrupo());
        assertEquals("Grupo 7", g.nombreGrupo());
        assertEquals("MAT1", g.codigoAsignatura());
        assertEquals("Ana Perez", g.nombreDocente());
        assertEquals(LocalDate.of(2026, 7, 20), g.fechaInicioPeriodo());
        assertEquals(LocalDate.of(2026, 12, 10), g.fechaFinPeriodo());
        assertEquals("Lunes 08:00-10:00", g.horario());
        assertEquals(30, g.capacidadMaxima());
        assertEquals(25, g.estudiantesActivos());
        assertEquals(5, g.cuposDisponibles());
        assertFalse(g.matriculado());
        assertEquals("INS", g.codigoComponente());
        assertTrue(pagina.grupos().get(1).matriculado());
    }

    @Test
    void OR02_sin_filas_devuelve_pagina_vacia() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final var pagina = repository.consultarOferta(ACTOR, null, 1, 10);

        assertTrue(pagina.grupos().isEmpty());
        assertEquals(0, pagina.total());
    }

    @Test
    void OR03_ejecutor_nulo_no_llega_a_la_db() {
        assertThrows(CrosscuttingException.class, () -> repository.consultarOferta(null, null, 1, 10));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void OR04_fallo_tecnico_se_traduce() {
        when(query.getResultList()).thenThrow(new PersistenceException("timeout"));

        assertThrows(DatabaseOperationException.class, () -> repository.consultarOferta(ACTOR, null, 1, 10));
    }

    @Test
    void OR05_fecha_acepta_los_tipos_que_entrega_el_driver() {
        final LocalDate esperada = LocalDate.of(2026, 3, 15);
        assertEquals(esperada, OfertaGruposEstudianteJpaRepository.fecha(esperada));
        assertEquals(esperada, OfertaGruposEstudianteJpaRepository.fecha(LocalDateTime.of(2026, 3, 15, 0, 0)));
        assertEquals(esperada, OfertaGruposEstudianteJpaRepository.fecha(Date.from(esperada.atStartOfDay(ZoneId.systemDefault()).toInstant())));
        assertNull(OfertaGruposEstudianteJpaRepository.fecha(null));
        assertNull(OfertaGruposEstudianteJpaRepository.fecha("2026-03-15"));
    }

    @Test
    void OR06_columnas_nulas_se_mapean_con_valores_seguros() {
        final Object[] fila = fila(UUID.randomUUID(), null, null);
        fila[1] = null;
        fila[12] = null;
        fila[15] = null;
        fila[17] = null;
        when(query.getResultList()).thenReturn(Collections.singletonList(fila));

        final var g = repository.consultarOferta(ACTOR, null, 1, 10).grupos().getFirst();

        assertNull(g.codigoGrupo());
        assertNull(g.horario());
        assertNull(g.fechaInicioPeriodo());
        assertEquals(0, g.cuposDisponibles());
        assertFalse(g.matriculado());
        assertNull(g.codigoComponente());
    }

    @Test
    void OR07_uniqueidentifier_como_texto_del_driver_sqlserver() {
        final UUID grupo = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        final UUID periodo = UUID.randomUUID();
        final Object[] fila = fila(null, LocalDate.of(2026, 7, 20), false);
        fila[0] = grupo.toString().toUpperCase();
        fila[3] = asignatura.toString();
        fila[7] = periodo.toString();
        when(query.getResultList()).thenReturn(Collections.singletonList(fila));

        final var g = repository.consultarOferta(ACTOR, null, 1, 10).grupos().getFirst();

        assertEquals(grupo, g.idGrupo());
        assertEquals(asignatura, g.idAsignatura());
        assertEquals(periodo, g.idPeriodoAcademico());
    }

    @Test
    void OR08_fila_de_19_columnas_mapea_componente_en_17_y_total_en_18() {
        final Object[] fila = fila(UUID.randomUUID(), LocalDate.of(2026, 7, 20), true);
        fila[17] = "ESP";
        fila[18] = 41;
        when(query.getResultList()).thenReturn(Collections.singletonList(fila));

        final var pagina = repository.consultarOferta(ACTOR, null, 1, 10);

        assertEquals(19, OfertaGruposEstudianteJpaRepository.COLUMNAS_OFERTA);
        assertEquals("ESP", pagina.grupos().getFirst().codigoComponente());
        assertEquals(41, pagina.total());
        assertTrue(pagina.grupos().getFirst().matriculado());
    }

    @Test
    void OR09_fila_con_longitud_distinta_falla() {
        final Object[] fila21 = new Object[21];
        System.arraycopy(fila(UUID.randomUUID(), LocalDate.of(2026, 7, 20), false), 0, fila21, 0, 19);
        when(query.getResultList()).thenReturn(Collections.singletonList(fila21));

        final DatabaseOperationException error = assertThrows(DatabaseOperationException.class,
                () -> repository.consultarOferta(ACTOR, null, 1, 10));

        assertInstanceOf(IllegalStateException.class, error.getCause());
    }

    @Test
    void constructor_rechaza_entity_manager_nulo() {
        assertThrows(NullPointerException.class, () -> new OfertaGruposEstudianteJpaRepository(null));
    }
}
