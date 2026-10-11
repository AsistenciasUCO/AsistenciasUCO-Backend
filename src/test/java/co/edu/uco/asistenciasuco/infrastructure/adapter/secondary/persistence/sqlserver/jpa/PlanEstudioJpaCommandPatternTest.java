package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
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
 * LB-008 JPA-03 — patron del command {@code usp_registrar_o_actualizar_plan_estudio}. TD-043 CERRADO: el
 * provider publico existe en la DB con la firma {@code idPlanEstudio, idPrograma, inp, idCorrelacion}
 * (sin {@code codigo}/{@code nombre}); el test verifica el SQL emitido y el binding nombrado.
 */
class PlanEstudioJpaCommandPatternTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLAN = UUID.fromString("20202020-2020-2020-2020-202020202020");
    private static final UUID PROGRAMA = UUID.fromString("50505050-5050-5050-5050-505050505050");
    private static final UUID EJECUTOR = UUID.fromString("60606060-6060-6060-6060-606060606060");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private PlanEstudioJpaRepository command;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        command = new PlanEstudioJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "Plan registrado.", "", true}));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void PLA_PAT_001_registrar_plan_usa_native_query_exec_con_parametros_del_contrato() {
        command.registrarOActualizarPlanEstudio(PLAN, PROGRAMA, 2026, EJECUTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_registrar_o_actualizar_plan_estudio"));
        verify(query).setParameter("idPlanEstudio", PLAN);
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("inp", 2026);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void PLA_PAT_002_sp_ausente_se_clasifica_como_database_operation_error() {
        when(query.getResultList()).thenThrow(new PersistenceException("Could not find stored procedure"));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.registrarOActualizarPlanEstudio(PLAN, PROGRAMA, 2026, EJECUTOR));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof PersistenceException);
    }

    @Test
    void PLA_PAT_004_sin_ejecutor_no_ejecuta_el_sp_y_falla_antes_de_la_db() {
        assertThrows(CrosscuttingException.class,
                () -> command.registrarOActualizarPlanEstudio(PLAN, PROGRAMA, 2026, null));

        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void PLA_PAT_003_resultado_sin_forma_canonica_se_rechaza_como_contrato() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.registrarOActualizarPlanEstudio(PLAN, PROGRAMA, 2026, EJECUTOR));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }
}




