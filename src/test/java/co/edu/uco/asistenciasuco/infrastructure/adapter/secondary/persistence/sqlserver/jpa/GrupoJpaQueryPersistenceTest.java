package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrupoJpaQueryPersistenceTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID CORRELACION = UUID.randomUUID();

    private final EntityManagerFactory factory = mock(EntityManagerFactory.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final StoredProcedureQuery procedureQuery = mock(StoredProcedureQuery.class);
    private final GrupoJpaQueryPersistence persistence = new GrupoJpaQueryPersistence(factory);

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    private void wireProcedure(final String spName) {
        when(factory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.createStoredProcedureQuery(spName)).thenReturn(procedureQuery);
        when(procedureQuery.registerStoredProcedureParameter(anyString(), any(), any(ParameterMode.class)))
                .thenReturn(procedureQuery);
        when(procedureQuery.setParameter(anyString(), any())).thenReturn(procedureQuery);
    }

    @Test
    void constructor_exige_entity_manager_factory() {
        assertThrows(NullPointerException.class, () -> new GrupoJpaQueryPersistence(null));
    }

    @Test
    void consultarGrupos_ejecuta_sp_y_mapea_proyecciones() {
        wireProcedure("dbo.usp_consultar_grupos_paginado");
        CorrelationIdContext.set(CORRELACION);

        final UUID grupoId = UUID.randomUUID();
        final UUID asignaturaId = UUID.randomUUID();
        final UUID docenteId = UUID.randomUUID();
        final Date fechaInicio = Date.valueOf("2026-02-01");
        final Date fechaFin = Date.valueOf("2026-06-30");

        final Object[] row = new Object[]{
                grupoId, "GRP-01", "Grupo Matematicas", asignaturaId, "Calculo I",
                docenteId, 30, 25, 5, true, fechaInicio, fechaFin, 1
        };
        when(procedureQuery.getResultList()).thenReturn(List.of((Object) row));

        final List<GrupoRepositoryProjection> result = persistence.consultarGrupos();

        assertNotNull(result);
        assertEquals(1, result.size());
        final GrupoRepositoryProjection proy = result.getFirst();
        assertEquals(grupoId, proy.getId());
        assertEquals("GRP-01", proy.getCodigo());
        assertEquals("Grupo Matematicas", proy.getNombre());
        assertEquals(asignaturaId, proy.getIdAsignatura());
        assertEquals("Calculo I", proy.getNombreAsignatura());
        assertEquals(docenteId, proy.getIdDocente());
        assertEquals(30, proy.getCapacidadMaximaPermitida());
        assertEquals(25, proy.getEstudiantesActivos());
        assertEquals(5, proy.getCuposDisponibles());
        assertTrue(proy.isGrupoHabilitado());

        verify(entityManager).close();
    }

    @Test
    void consultarEstudiantesGrupo_ejecuta_sp_y_mapea_proyecciones() {
        wireProcedure("dbo.usp_consultar_estudiantes_grupo_paginado");
        CorrelationIdContext.set(CORRELACION);

        final UUID idEstudianteGrupo = UUID.randomUUID();
        final UUID idEstudiante = UUID.randomUUID();
        final UUID idUsuario = UUID.randomUUID();
        final UUID tipoId = UUID.randomUUID();
        final UUID idEstado = UUID.randomUUID();

        final Object[] row = new Object[]{
                idEstudianteGrupo, idEstudiante, idUsuario, tipoId, "10203040",
                "Perez", "Gomez", "Juan", "Carlos", "Juan Carlos Perez",
                "juan@uco.edu.co", idEstado, "ACT", "Activo", true,
                0, 0, 10, 10, 1
        };
        when(procedureQuery.getResultList()).thenReturn(List.of((Object) row));

        final List<EstudianteGrupoRepositoryProjection> result = persistence.consultarEstudiantesGrupo(GRUPO);

        assertNotNull(result);
        assertEquals(1, result.size());
        final EstudianteGrupoRepositoryProjection proy = result.getFirst();
        assertEquals(idEstudianteGrupo, proy.id());
        assertEquals(idEstudiante, proy.idEstudiante());
        assertEquals("10203040", proy.documento());
        assertEquals("Juan Carlos Perez", proy.nombreCompleto());
        assertEquals("juan@uco.edu.co", proy.correo());
        assertEquals("ACT", proy.codigoEstado());
        assertEquals("Activo", proy.nombreEstado());

        verify(entityManager).close();
    }

    @Test
    void consultarGrupos_persistencia_exception_se_traduce_a_database_operation_exception() {
        wireProcedure("dbo.usp_consultar_grupos_paginado");
        CorrelationIdContext.set(CORRELACION);
        when(procedureQuery.getResultList()).thenThrow(new PersistenceException("Fallo DB"));

        assertThrows(DatabaseOperationException.class, () -> persistence.consultarGrupos());
        verify(entityManager).close();
    }

    @Test
    void consultarEstudiantesGrupo_persistencia_exception_se_traduce_a_database_operation_exception() {
        wireProcedure("dbo.usp_consultar_estudiantes_grupo_paginado");
        CorrelationIdContext.set(CORRELACION);
        when(procedureQuery.getResultList()).thenThrow(new PersistenceException("Fallo DB"));

        assertThrows(DatabaseOperationException.class, () -> persistence.consultarEstudiantesGrupo(GRUPO));
        verify(entityManager).close();
    }
}
