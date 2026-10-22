package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Patron del command {@code usp_crear_docente} y consultas sobre {@code EntityManager}.
 */
class DocenteProgramaJpaCommandPatternTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DOCENTE = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PROGRAMA = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID EJECUTOR = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query nativeQuery = mock(Query.class);
    private final DocenteProgramaJpaRepository repository = new DocenteProgramaJpaRepository(
            entityManager, new JpaProcedureExecutor(entityManager));

    @BeforeEach
    void setUp() {
        CorrelationIdContext.set(CORRELATION);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
    }

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    void DOC_PAT_001_ejecucion_exitosa_invoca_usp_crear_docente_sin_flush_ni_clear() {
        when(nativeQuery.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                CORRELATION.toString(),
                "Docente registrado exitosamente.",
                "Operacion completada. Correlacion: " + CORRELATION,
                true
        }));

        repository.crearDocente(DOCENTE, "1017888777", "Esteban", "", "Quintero", "",
                "esteban.quintero@uco.edu.co", PROGRAMA, "HASH", EJECUTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_docente"));
        verify(nativeQuery).setParameter("idDocente", DOCENTE);
        verify(nativeQuery).setParameter("numeroIdentificacion", "1017888777");
        verify(nativeQuery).setParameter("idPrograma", PROGRAMA);
        verify(nativeQuery).setParameter("idCorrelacion", CORRELATION);
        verify(nativeQuery).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(entityManager, never()).flush();
        verify(entityManager, never()).clear();
    }

    @Test
    void DOC_PAT_002_estado_cero_traduce_dbcode_a_resource_not_found_exception() {
        when(nativeQuery.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                CORRELATION.toString(),
                "El programa no existe.",
                "DBCODE=PROG_001|Programa no encontrado",
                false
        }));

        final co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException ex =
                assertThrows(co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException.class, () ->
                repository.crearDocente(DOCENTE, "1017888777", "Esteban", "", "Quintero", "",
                        "esteban.quintero@uco.edu.co", PROGRAMA, "HASH", EJECUTOR));

        assertEquals("El recurso solicitado no existe.", ex.getMessage());
    }

    @Test
    void DOC_PAT_003_excepcion_de_persistencia_se_envuelve_en_database_operation_error() {
        when(nativeQuery.getResultList()).thenThrow(new PersistenceException("Fallo SQL"));

        final DatabaseOperationException ex = assertThrows(DatabaseOperationException.class, () ->
                repository.crearDocente(DOCENTE, "1017888777", "Esteban", "", "Quintero", "",
                        "esteban.quintero@uco.edu.co", PROGRAMA, "HASH", EJECUTOR));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), ex.getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void DOC_PAT_004_consultar_docentes_disponibles_mapea_correctamente() {
        final TypedQuery<Object[]> typedQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(typedQuery);
        final Object[] fila = new Object[]{
                DOCENTE,
                UUID.randomUUID(),
                1017888777,
                "Esteban Quintero",
                "esteban.quintero@uco.edu.co",
                true
        };
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(fila));

        final List<DocenteProgramaProjection> resultado = repository.consultarDocentesDisponibles();

        assertEquals(1, resultado.size());
        assertEquals(DOCENTE, resultado.getFirst().id());
        assertEquals("1017888777", resultado.getFirst().numeroIdentificacion());
        assertEquals("Esteban Quintero", resultado.getFirst().nombreCompleto());
        assertEquals("esteban.quintero@uco.edu.co", resultado.getFirst().correo());
        assertEquals(0, resultado.getFirst().totalGruposAsignados());
        assertTrue(resultado.getFirst().estaActivoDocente());
    }

    @Test
    @SuppressWarnings("unchecked")
    void DOC_PAT_005_consultar_docentes_por_programa_retorna_vacio_cuando_no_hay_filas() {
        final TypedQuery<UvDocenteEntity> typedQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(UvDocenteEntity.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());

        final List<DocenteProgramaProjection> resultado = repository.consultarDocentesPorPrograma(PROGRAMA);

        assertTrue(resultado.isEmpty());
    }
}
