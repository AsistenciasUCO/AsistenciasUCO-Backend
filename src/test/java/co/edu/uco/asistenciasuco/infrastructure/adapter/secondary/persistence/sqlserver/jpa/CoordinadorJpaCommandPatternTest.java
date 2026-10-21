package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * LB-008 JPA-03 — patron del command {@code usp_crear_coordinador} sobre {@code EntityManager + createNativeQuery}.
 */
class CoordinadorJpaCommandPatternTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COORDINADOR = UUID.fromString("40404040-4040-4040-4040-404040404040");
    private static final UUID PROGRAMA = UUID.fromString("50505050-5050-5050-5050-505050505050");
    private static final UUID FACULTAD = UUID.fromString("60606060-6060-6060-6060-606060606060");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private CoordinadorJpaRepository command;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        command = new CoordinadorJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "Coordinador creado.", "", true}));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void COO_PAT_001_crear_coordinador_usa_native_query_exec_con_binding_nombrado() {
        command.crearCoordinador(COORDINADOR, "1001", "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@example.test",
                PROGRAMA, FACULTAD, "Clave123!", EJECUTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_coordinador"));
        verify(query).setParameter("idCoordinador", COORDINADOR);
        verify(query).setParameter("numeroIdentificacion", "1001");
        verify(query).setParameter("primerNombre", "ANA");
        verify(query).setParameter("segundoNombre", "MARIA");
        verify(query).setParameter("primerApellido", "PEREZ");
        verify(query).setParameter("segundoApellido", "GOMEZ");
        verify(query).setParameter("correo", "ana@example.test");
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("idFacultad", FACULTAD);
        verify(query).setParameter("password", "Clave123!");
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void COO_PAT_002_fallo_tecnico_se_traduce_a_database_operation_error() {
        when(query.getResultList()).thenThrow(new PersistenceException("deadlock"));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.crearCoordinador(COORDINADOR, "1001", "ANA", null, "PEREZ", null, "ana@example.test",
                        PROGRAMA, FACULTAD, "Clave123!", EJECUTOR));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof PersistenceException);
    }

    @Test
    void COO_PAT_003_resultado_sin_forma_canonica_se_rechaza_como_contrato() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.crearCoordinador(COORDINADOR, "1001", "ANA", null, "PEREZ", null, "ana@example.test",
                        PROGRAMA, FACULTAD, "Clave123!", EJECUTOR));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    @Test
    void COO_PAT_004_consultar_coordinadores_sin_programa_mapea_correctamente() {
        final jakarta.persistence.TypedQuery typedQuery = mock(jakarta.persistence.TypedQuery.class);
        when(entityManager.createQuery(anyString(), org.mockito.ArgumentMatchers.eq(Object[].class))).thenReturn(typedQuery);
        final Object[] fila = new Object[]{
                COORDINADOR,
                UUID.randomUUID(),
                "1017555444",
                "Jorge Henao",
                null,
                null,
                1
        };
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(fila));

        final java.util.List<co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection> resultado =
                command.consultarCoordinadoresSinPrograma();

        assertEquals(1, resultado.size());
        assertEquals(COORDINADOR, resultado.getFirst().id());
        assertEquals("1017555444", resultado.getFirst().numeroIdentificacion());
        assertEquals("Jorge Henao", resultado.getFirst().nombreCompleto());
        assertTrue(resultado.getFirst().estaActivoCoordinador());
    }

    @Test
    void COO_PAT_005_consultar_coordinadores_por_facultad_mapea_correctamente() {
        final jakarta.persistence.TypedQuery typedQuery = mock(jakarta.persistence.TypedQuery.class);
        when(entityManager.createQuery(anyString(), org.mockito.ArgumentMatchers.eq(co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvCoordinadorEntity.class)))
                .thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());

        final java.util.List<co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection> resultado =
                command.consultarCoordinadoresPorFacultad(FACULTAD);

        assertTrue(resultado.isEmpty());
    }
}




