package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CULM-001 D.1 (CONTRACT §2.3, DEC-044 (2)): las sesiones de la materia del estudiante salen de
 * {@code usp_consultar_sesiones_materia_estudiante} (ambito, matricula inscrita, paginacion y fecha limite dentro del SP);
 * el adaptador ya no usa HQL sobre {@code UvSesionEntity}.
 */
class SesionMateriaEstudianteJpaRepositoryTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID MATERIA = UUID.randomUUID();

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] fila(final UUID id, final Object inicio, final Object fin, final Object limite, final Object vigente,
                                 final Object total) {
        return new Object[]{id.toString(), "Sesion 1", 1, "SES-01", 2, UUID.randomUUID(), 3, "Grupo 3", inicio, fin, limite, vigente, total};
    }

    @Test
    void llama_al_sp_con_el_usuario_ejecutor_la_materia_y_la_pagina_sin_hql() {
        when(query.getResultList()).thenReturn(List.of());

        new SesionMateriaEstudianteJpaRepository(manager).consultarSesionesMateria(ACTOR, MATERIA, 2, 25);

        verify(manager).createNativeQuery(SesionMateriaEstudianteJpaRepository.SQL_SESIONES_MATERIA);
        assertTrue(SesionMateriaEstudianteJpaRepository.SQL_SESIONES_MATERIA.contains("usp_consultar_sesiones_materia_estudiante"));
        assertFalse(SesionMateriaEstudianteJpaRepository.SQL_SESIONES_MATERIA.toLowerCase().contains("uv_sesion"));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(query).setParameter("idMateria", MATERIA);
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 25);
        verify(manager, never()).createQuery(anyString(), any(Class.class));
    }

    @Test
    void mapea_las_13_columnas_incluida_la_fecha_limite_utc_y_el_plazo_vigente() {
        final UUID id = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 11, 4, 12, 0);
        final LocalDateTime fin = LocalDateTime.of(2026, 11, 4, 13, 0);
        final LocalDateTime limite = LocalDateTime.of(2026, 11, 10, 5, 0);
        when(query.getResultList()).thenReturn(List.<Object>of(
                fila(id, inicio, fin, limite, Boolean.TRUE, 7),
                fila(UUID.randomUUID(), Timestamp.valueOf(inicio), Timestamp.valueOf(fin), null, 0, 7),
                fila(UUID.randomUUID(), inicio, fin, limite, null, 7)));

        final var pagina = new SesionMateriaEstudianteJpaRepository(manager).consultarSesionesMateria(ACTOR, MATERIA, 1, 3);

        assertEquals(1, pagina.numeroPagina());
        assertEquals(3, pagina.tamanoPagina());
        assertEquals(7L, pagina.totalElementos());
        assertEquals(3, pagina.elementos().size());
        final var primera = pagina.elementos().getFirst();
        assertEquals(id, primera.id());
        assertEquals("Sesion 1", primera.nombre());
        assertEquals(1, primera.numero());
        assertEquals("SES-01", primera.codigo());
        assertEquals(2, primera.numeroSemana());
        assertEquals("3", primera.codigoGrupo());
        assertEquals("Grupo 3", primera.nombreGrupo());
        assertEquals(inicio, primera.fechaHoraInicio());
        assertEquals(fin, primera.fechaHoraFin());
        assertEquals(limite, primera.fechaLimiteJustificacionUtc());
        assertTrue(primera.plazoJustificacionVigente());
        assertNull(pagina.elementos().get(1).fechaLimiteJustificacionUtc());
        assertFalse(pagina.elementos().get(1).plazoJustificacionVigente());
        assertFalse(pagina.elementos().get(2).plazoJustificacionVigente());
    }

    @Test
    void sin_filas_el_total_es_cero() {
        when(query.getResultList()).thenReturn(List.of());

        final var pagina = new SesionMateriaEstudianteJpaRepository(manager).consultarSesionesMateria(ACTOR, MATERIA, 1, 100);

        assertTrue(pagina.elementos().isEmpty());
        assertEquals(0L, pagina.totalElementos());
    }

    @Test
    void sin_usuario_ejecutor_o_materia_no_llega_a_la_db() {
        final var repository = new SesionMateriaEstudianteJpaRepository(manager);

        assertThrows(CrosscuttingException.class, () -> repository.consultarSesionesMateria(null, MATERIA, 1, 100));
        assertThrows(CrosscuttingException.class, () -> repository.consultarSesionesMateria(ACTOR, null, 1, 100));
        verify(manager, never()).createNativeQuery(anyString());
    }

    @Test
    void columnas_distintas_de_13_son_un_fallo_tecnico() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[12]));

        assertInstanceOf(IllegalStateException.class, assertThrows(DatabaseOperationException.class,
                () -> new SesionMateriaEstudianteJpaRepository(manager).consultarSesionesMateria(ACTOR, MATERIA, 1, 100)).getCause());
    }

    @Test
    void los_rechazos_del_sp_se_traducen_con_su_http() {
        when(query.getResultList()).thenThrow(new PersistenceException("x", new RuntimeException("DBCODE=PAG_001|paginacion")));
        assertThrows(ValidationException.class,
                () -> new SesionMateriaEstudianteJpaRepository(manager).consultarSesionesMateria(ACTOR, MATERIA, 0, 100));

        when(query.getResultList()).thenThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_001|perfil")));
        assertThrows(ForbiddenException.class,
                () -> new SesionMateriaEstudianteJpaRepository(manager).consultarSesionesMateria(ACTOR, MATERIA, 1, 100));

        when(query.getResultList()).thenThrow(new PersistenceException("db down"));
        assertThrows(DatabaseOperationException.class,
                () -> new SesionMateriaEstudianteJpaRepository(manager).consultarSesionesMateria(ACTOR, MATERIA, 1, 100));
    }

    @Test
    void ninguna_constante_del_adaptador_contiene_hql_sobre_la_vista_congelada() throws IllegalAccessException {
        for (final java.lang.reflect.Field campo : SesionMateriaEstudianteJpaRepository.class.getDeclaredFields()) {
            if (campo.getType() == String.class && java.lang.reflect.Modifier.isStatic(campo.getModifiers())) {
                campo.setAccessible(true);
                final String valor = String.valueOf(campo.get(null));
                assertFalse(valor.contains("UvSesionEntity"), campo.getName());
                assertFalse(valor.toLowerCase().contains("select new"), campo.getName());
            }
        }
    }

    @Test
    void el_entity_manager_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new SesionMateriaEstudianteJpaRepository(null));
    }
}
