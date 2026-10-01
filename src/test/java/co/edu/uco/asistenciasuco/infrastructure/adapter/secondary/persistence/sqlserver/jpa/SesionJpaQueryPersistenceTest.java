package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SesionJpaQueryPersistenceTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID CORRELACION = UUID.randomUUID();

    private final EntityManagerFactory factory = mock(EntityManagerFactory.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final StoredProcedureQuery procedureQuery = mock(StoredProcedureQuery.class);
    private final SesionJpaQueryPersistence persistence = new SesionJpaQueryPersistence(factory);

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    private void wireProcedure() {
        when(factory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.createStoredProcedureQuery("dbo.usp_consultar_sesiones_grupo_paginado"))
                .thenReturn(procedureQuery);
        when(procedureQuery.registerStoredProcedureParameter(anyString(), any(), any(ParameterMode.class)))
                .thenReturn(procedureQuery);
        when(procedureQuery.setParameter(anyString(), any())).thenReturn(procedureQuery);
    }

    @Test
    void constructor_exige_entity_manager_factory() {
        assertThrows(NullPointerException.class, () -> new SesionJpaQueryPersistence(null));
    }

    @Test
    void consultarSesionesPorGrupo_ejecuta_sp_y_mapea_proyecciones() {
        wireProcedure();
        CorrelationIdContext.set(CORRELACION);

        final UUID sesionId = UUID.randomUUID();
        final Timestamp inicio = Timestamp.valueOf(LocalDateTime.of(2026, 9, 14, 8, 0, 0));
        final Timestamp fin = Timestamp.valueOf(LocalDateTime.of(2026, 9, 14, 10, 0, 0));

        final Object[] row = new Object[]{
                sesionId, GRUPO, "GRP-01", "Matematicas", "Sesion 1",
                1, "SES-01", 1, inicio, fin, 1
        };
        when(procedureQuery.getResultList()).thenReturn(List.of((Object) row));

        final List<SesionRepositoryProjection> result = persistence.consultarSesionesPorGrupo(GRUPO);

        assertNotNull(result);
        assertEquals(1, result.size());
        final SesionRepositoryProjection proy = result.getFirst();
        assertEquals(sesionId, proy.getSesion());
        assertEquals(GRUPO, proy.getGrupo());
        assertEquals("GRP-01", proy.getCodigoGrupo());
        assertEquals("Matematicas", proy.getNombreGrupo());
        assertEquals("Sesion 1", proy.getNombre());
        assertEquals(1, proy.getNumero());
        assertEquals("SES-01", proy.getCodigo());
        assertEquals(1, proy.getNumeroSemana());
        assertNotNull(proy.getFechaHoraInicio());
        assertNotNull(proy.getFechaHoraFin());

        verify(entityManager).close();
    }

    @Test
    void consultarSesionesPorGrupo_persistencia_exception_se_traduce_a_database_operation_exception() {
        wireProcedure();
        CorrelationIdContext.set(CORRELACION);
        when(procedureQuery.getResultList()).thenThrow(new PersistenceException("Fallo DB"));

        assertThrows(DatabaseOperationException.class, () -> persistence.consultarSesionesPorGrupo(GRUPO));
        verify(entityManager).close();
    }
}
