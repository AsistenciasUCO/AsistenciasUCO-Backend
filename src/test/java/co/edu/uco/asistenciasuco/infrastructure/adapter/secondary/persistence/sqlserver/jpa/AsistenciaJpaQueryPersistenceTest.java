package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Flujo y traduccion de errores de la persistencia JPA de asistencias con fakes. CULM-001 A.2-2: la consulta por grupo
 * ejecuta {@code usp_consultar_asistencias_grupo_ambito_paginado} con el ejecutor (lista completa, {@code @tamanoPagina =
 * NULL}); un grupo ajeno o inexistente llega como {@code DBCODE=SEC_002} y responde 403. A.2-7: retiro del reclamo propio.
 * NO certifica que JPA/SQL Server funcione: eso lo certifica {@code AsistenciaQueryJpaParityIT}.
 */
class AsistenciaJpaRepositoryTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID CORRELACION = UUID.randomUUID();

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final AsistenciaJpaRepository persistence = new AsistenciaJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private void wireQuery() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @Test
    void dto_nulo_lanza_la_misma_excepcion_semantica_sin_abrir_entity_manager() {
        final CrosscuttingException exception =
                assertThrows(CrosscuttingException.class, () -> persistence.consultarAsistenciasPorGrupo(null));

        assertEquals("El dominio para consultar asistencias por grupo es obligatorio.", exception.getMessage());
        verifyNoInteractions(entityManager);
    }

    @Test
    void sin_ejecutor_se_rechaza_sin_abrir_entity_manager() {
        assertThrows(CrosscuttingException.class,
                () -> persistence.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION)));
        verifyNoInteractions(entityManager);
    }

    @Test
    void constructor_exige_entity_manager() {
        assertThrows(NullPointerException.class, () -> new AsistenciaJpaRepository(null, new JpaProcedureExecutor(entityManager)));
    }

    @Test
    void constructor_exige_procedure_executor() {
        assertThrows(NullPointerException.class, () -> new AsistenciaJpaRepository(entityManager, null));
    }

    @Test
    void mapea_las_8_columnas_y_pasa_grupo_sesion_ejecutor_y_correlacion() {
        wireQuery();
        final UUID asistencia = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.<Object>of(
                new Object[]{asistencia, estudiante.toString(), GRUPO, SESION, null, null, null, 2},
                new Object[]{UUID.randomUUID(), estudiante, GRUPO, SESION, Boolean.TRUE, "EX ", "Cita medica", 2}));

        final List<AsistenciaRepositoryProjection> result = persistence.consultarAsistenciasPorGrupo(
                new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION, ACTOR));

        assertEquals(2, result.size());
        assertEquals(asistencia, result.getFirst().getAsistencia());
        assertEquals(estudiante, result.getFirst().getEstudiante());
        assertFalse(result.getFirst().isPresente());
        assertNull(result.getFirst().getEstado());
        assertEquals("", result.getFirst().getObservacion());
        assertTrue(result.get(1).isPresente());
        assertEquals("EX", result.get(1).getEstado());
        assertEquals("Cita medica", result.get(1).getObservacion());
        assertTrue(AsistenciaJpaRepository.SQL_CONSULTAR_ASISTENCIAS.contains("usp_consultar_asistencias_grupo_ambito_paginado"));
        assertTrue(AsistenciaJpaRepository.SQL_CONSULTAR_ASISTENCIAS.contains("@tamanoPagina = NULL"));
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("idSesion", SESION.toString());
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(entityManager, never()).close();
    }

    @Test
    void sesion_nula_se_pasa_como_parametro_nulo() {
        wireQuery();
        when(query.getResultList()).thenReturn(List.of());

        assertTrue(persistence.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, null, ACTOR)).isEmpty());
        verify(query).setParameter("idSesion", (Object) null);
    }

    @Test
    void presente_numerico_y_fila_con_otra_cantidad_de_columnas() {
        wireQuery();
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{UUID.randomUUID(), UUID.randomUUID(), GRUPO, SESION, 1, "AN", null, 1}));
        assertTrue(persistence.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, null, ACTOR))
                .getFirst().isPresente());

        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[6]));
        final DatabaseOperationException error = assertThrows(DatabaseOperationException.class,
                () -> persistence.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, null, ACTOR)));
        assertInstanceOf(IllegalStateException.class, error.getCause());
    }

    @Test
    void grupo_fuera_de_ambito_responde_403() {
        wireQuery();
        when(query.getResultList()).thenThrow(new PersistenceException("wrapper",
                new RuntimeException("DBCODE=SEC_002|El usuario no tiene titularidad sobre el grupo.")));

        assertThrows(ForbiddenException.class,
                () -> persistence.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION, ACTOR)));
    }

    @Test
    void persistence_exception_sin_codigo_se_traduce_a_database_operation_exception() {
        wireQuery();
        final PersistenceException cause = new PersistenceException("jpa-detail");
        when(query.getResultList()).thenThrow(cause);

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION, ACTOR)));

        assertEquals("No fue posible consultar las asistencias de base de datos.", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void illegal_state_del_entity_manager_compartido_tambien_se_traduce() {
        when(entityManager.createNativeQuery(anyString())).thenThrow(new IllegalStateException("entity manager closed"));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, null, ACTOR)));

        assertNotNull(exception.getCause());
    }

    // --- CULM-001 A.2-7 (A2-B-12): retiro del reclamo propio pendiente ---

    private void canonico(final Object exitoso, final String usuario, final String tecnico) {
        wireQuery();
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{CORRELACION, usuario, tecnico, exitoso}));
    }

    @Test
    void retirar_reclamo_ejecuta_el_sp_con_el_ejecutor_y_devuelve_el_mensaje_institucional() {
        final UUID reclamo = UUID.randomUUID();
        canonico(true, "La solicitud de revision fue retirada.", "REV_008");

        assertEquals("La solicitud de revision fue retirada.", persistence.retirarSolicitudRevision(reclamo, ACTOR));
        verify(entityManager).createNativeQuery(AsistenciaJpaRepository.SQL_RETIRAR_SOLICITUD_REVISION);
        verify(query).setParameter("idSolicitudRevision", reclamo);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELACION);
    }

    @Test
    void retirar_reclamo_ajeno_o_inexistente_404_y_no_pendiente_409() {
        canonico(false, "La solicitud de revision no existe.", "DBCODE=REV_006|ajeno o inexistente.");
        assertThrows(ResourceNotFoundException.class, () -> persistence.retirarSolicitudRevision(UUID.randomUUID(), ACTOR));

        canonico(false, "Solo se puede retirar una solicitud de revision pendiente.", "DBCODE=REV_007|no pendiente.");
        assertThrows(ConflictException.class, () -> persistence.retirarSolicitudRevision(UUID.randomUUID(), ACTOR));
    }

    @Test
    void retirar_reclamo_sin_datos_no_llega_a_la_db() {
        assertThrows(CrosscuttingException.class, () -> persistence.retirarSolicitudRevision(null, ACTOR));
        assertThrows(CrosscuttingException.class, () -> persistence.retirarSolicitudRevision(UUID.randomUUID(), null));
        verifyNoInteractions(entityManager);
    }
}
