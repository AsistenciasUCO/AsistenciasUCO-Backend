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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                1,
                "Jorge",
                "Luis",
                "Henao",
                "Perez",
                "jorge.henao@uco.edu.co",
                1
        };
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(fila));

        final java.util.List<co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection> resultado =
                command.consultarCoordinadoresSinPrograma(FACULTAD);

        // HU121 D2: el coordinador sin programa se filtra por la facultad del decano (antes devolvia los de todas).
        verify(typedQuery).setParameter("idFacultad", FACULTAD);
        assertEquals(1, resultado.size());
        assertEquals(COORDINADOR, resultado.getFirst().id());
        assertEquals("1017555444", resultado.getFirst().numeroIdentificacion());
        assertEquals("Jorge Henao", resultado.getFirst().nombreCompleto());
        assertTrue(resultado.getFirst().estaActivoCoordinador());
        assertEquals("Jorge", resultado.getFirst().primerNombre());
        assertEquals("jorge.henao@uco.edu.co", resultado.getFirst().correo());
        assertTrue(resultado.getFirst().estaActivoRolCoordinador());
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

    // Certificacion F3: resumen por programa (facultad, docentes activos y grupos habilitados) solo desde vistas uv_*.
    @Test
    void COO_PAT_011_resumen_de_programas_lee_vistas_y_mapea_conteos() {
        final UUID programa = UUID.fromString("80808080-8080-8080-8080-808080808080");
        when(query.getResultList()).thenReturn(java.util.List.of(
                new Object[]{programa, "Facultad de Ingenieria", 1, 2L},
                new Object[]{programa.toString(), null, null, null}));

        final java.util.List<co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenCoordinacionProjection> resumen =
                command.consultarResumenProgramasFacultad(FACULTAD);

        org.mockito.Mockito.verify(entityManager).createNativeQuery(CoordinadorJpaRepository.SQL_RESUMEN_PROGRAMAS_FACULTAD);
        org.mockito.Mockito.verify(query).setParameter("idFacultad", FACULTAD);
        assertEquals(2, resumen.size());
        assertEquals(programa, resumen.get(0).idPrograma());
        assertEquals("Facultad de Ingenieria", resumen.get(0).nombreFacultad());
        assertEquals(1, resumen.get(0).totalDocentes());
        assertEquals(2, resumen.get(0).totalGrupos());
        assertEquals(programa, resumen.get(1).idPrograma());
        assertEquals(0, resumen.get(1).totalDocentes());
        assertTrue(CoordinadorJpaRepository.SQL_RESUMEN_PROGRAMAS_FACULTAD.contains("dbo.uv_docente_programa"));
        assertTrue(CoordinadorJpaRepository.SQL_RESUMEN_PROGRAMAS_FACULTAD.contains("dbo.uv_grupo_programa"));
    }

    // Regresion hallada en el E2E de HU121/HU122: con Coordinador.facultad, uv_coordinador devuelve filas con
    // idPrograma nulo (parte de la identidad compuesta de la entidad) y Hibernate las entrega como null. Los
    // coordinadores sin programa se leen con la otra consulta (por columnas), asi que esta los excluye.
    @Test
    void COO_PAT_010_listado_por_facultad_solo_lee_coordinadores_con_programa() {
        assertTrue(CoordinadorJpaRepository.HQL_POR_FACULTAD.contains("c.idPrograma is not null"));
        assertTrue(CoordinadorJpaRepository.HQL_SIN_PROGRAMA.contains("c.idPrograma is null"));
        assertTrue(CoordinadorJpaRepository.HQL_SIN_PROGRAMA.contains("c.idFacultad = :idFacultad"));
    }

    // HU121/HU122 (CONTRACT HU121-HU122 §3.1): comandos de edicion y estado.

    @Test
    void COO_PAT_006_actualizar_coordinador_usa_native_query_con_binding_nombrado_y_devuelve_el_usuario() {
        final UUID idUsuario = UUID.fromString("70707070-7070-7070-7070-707070707070");
        when(query.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{CORRELATION, "Coordinador actualizado.", "", true, idUsuario}));

        final co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection resultado =
                command.actualizarCoordinador(COORDINADOR, "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@example.test", FACULTAD, EJECUTOR,
                        co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO.ninguna());

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_actualizar_coordinador"));
        verify(query).setParameter("idCoordinador", COORDINADOR);
        verify(query).setParameter("primerNombre", "ANA");
        verify(query).setParameter("segundoNombre", "MARIA");
        verify(query).setParameter("primerApellido", "PEREZ");
        verify(query).setParameter("segundoApellido", "GOMEZ");
        verify(query).setParameter("correo", "ana@example.test");
        verify(query).setParameter("idFacultad", FACULTAD);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        assertEquals(idUsuario, resultado.idUsuario());
        assertFalse(resultado.personaSinRolesActivos());
    }

    @Test
    void COO_PAT_007_cambiar_estado_usa_native_query_y_devuelve_usuario_y_persona_sin_roles_activos() {
        final UUID idUsuario = UUID.fromString("70707070-7070-7070-7070-707070707070");
        when(query.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{CORRELATION, "Coordinador actualizado.", "", true, idUsuario, true}));

        final co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection resultado =
                command.cambiarEstadoCoordinador(COORDINADOR, false, FACULTAD, EJECUTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_cambiar_estado_coordinador"));
        verify(query).setParameter("idCoordinador", COORDINADOR);
        verify(query).setParameter("estaActivo", false);
        verify(query).setParameter("idFacultad", FACULTAD);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        assertEquals(idUsuario, resultado.idUsuario());
        assertTrue(resultado.personaSinRolesActivos());
    }

    // Regresion hallada en el E2E: SQL Server entrega los uniqueidentifier como texto en consultas nativas.
    @Test
    void COO_PAT_011_el_usuario_del_coordinador_puede_llegar_como_texto() {
        final UUID idUsuario = UUID.fromString("70707070-7070-7070-7070-707070707070");
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                CORRELATION.toString(), "ok", "", true, idUsuario.toString(), 1}));

        final co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection resultado =
                command.cambiarEstadoCoordinador(COORDINADOR, false, FACULTAD, EJECUTOR);

        assertEquals(idUsuario, resultado.idUsuario());
        assertTrue(resultado.personaSinRolesActivos());
    }

    @Test
    void COO_PAT_012_un_usuario_con_formato_invalido_es_violacion_de_contrato() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                CORRELATION, "ok", "", true, "no-es-un-uuid"}));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.actualizarCoordinador(COORDINADOR, "ANA", null, "PEREZ", null, "ana@example.test", FACULTAD, EJECUTOR,
                        co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO.ninguna()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    @Test
    void COO_PAT_008_estado_rechazado_por_la_db_se_traduce_a_conflicto_de_negocio() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                CORRELATION, "No se puede desactivar al coordinador mientras tenga un programa activo.",
                "DBCODE=COO_001|el coordinador coordina un programa activo", false, null, false}));

        assertThrows(co.edu.uco.asistenciasuco.application.exception.business.ConflictException.class,
                () -> command.cambiarEstadoCoordinador(COORDINADOR, false, FACULTAD, EJECUTOR));
    }

    @Test
    void COO_PAT_009_coordinador_ajeno_o_inexistente_se_traduce_a_recurso_no_encontrado() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                CORRELATION, "El Coordinador especificado no existe en el sistema.",
                "DBCODE=GEN_001|Coordinador", false, null}));

        assertThrows(co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException.class,
                () -> command.actualizarCoordinador(COORDINADOR, "ANA", null, "PEREZ", null, "ana@example.test", FACULTAD, EJECUTOR,
                        co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO.ninguna()));
    }
}




