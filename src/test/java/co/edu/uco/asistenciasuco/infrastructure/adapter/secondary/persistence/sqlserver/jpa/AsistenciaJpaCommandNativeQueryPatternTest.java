package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * LB-008 JPA-01 COMMANDS — RED estructural (CMD-PAT-001..004). Fija el patron DEFINITIVO de los cuatro
 * commands de Asistencia: {@code EntityManager} + {@code createNativeQuery("EXEC dbo.usp_xxx ...")} +
 * binding nombrado + {@code getResultList()} + {@link co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultMapper}
 * + {@link co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultValidator}.
 *
 * <p>Unit test de ESTRUCTURA: el EntityManager es un fake. La paridad real contra SQL Server la certifica
 * {@code AsistenciaCommandsSpParityIT}. Prohibido en el patron: {@code StoredProcedureQuery},
 * {@code ParameterMode}, transacciones JPA y JDBC.</p>
 */
class AsistenciaJpaCommandNativeQueryPatternTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SESION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SOPORTE = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ESTUDIANTE = UUID.fromString("44444444-4444-4444-4444-444444444441");
    private static final UUID SOLICITUD = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID DOCENTE = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private AsistenciaJpaRepository command;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        command = new AsistenciaJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(exito()));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] exito() {
        return new Object[]{CORRELATION, "Operacion exitosa.", "", true};
    }

    @Test
    void CMD_PAT_001_registrar_asistencias_sesion_usa_create_native_query_con_exec_y_binding_nombrado() {
        command.registrarAsistenciasSesion(new RegistrarAsistenciasSesionRepositoryDTO(
                SESION,
                List.of(new RegistroAsistenciaSesionRepositoryDTO(ESTUDIANTE, "AN")),
                EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_registrar_asistencias_sesion"));
        verify(query).setParameter("idSesion", SESION);
        verify(query).setParameter("asistenciaJSON", "[{\"idEstudiante\":\"" + ESTUDIANTE + "\",\"estado\":\"AN\"}]");
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void CMD_PAT_002_registrar_asistencia_autonoma_usa_create_native_query_con_sus_parametros_reales() {
        command.registrarAsistenciaAutonoma(new RegistrarAsistenciaAutonomaRepositoryDTO(
                ESTUDIANTE, SESION, "1234", EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_registrar_asistencia_estudiante_autonomo"));
        verify(query).setParameter("idEstudiante", ESTUDIANTE);
        verify(query).setParameter("idSesion", SESION);
        verify(query).setParameter("codigoVerificacion", "1234");
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void CMD_PAT_003_solicitar_revision_usa_create_native_query_con_sus_parametros_reales() {
        command.solicitarRevisionAsistencia(new SolicitarRevisionAsistenciaRepositoryDTO(
                ESTUDIANTE, SESION, "NOTA", "Justificacion", SOPORTE, EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_radicar_solicitud_revision_asistencia_v2"));
        verify(query).setParameter("idEstudiante", ESTUDIANTE);
        verify(query).setParameter("idSesion", SESION);
        verify(query).setParameter("categoria", "NOTA");
        verify(query).setParameter("justificacion", "Justificacion");
        verify(query).setParameter("idArchivoSoporte", SOPORTE.toString());
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void CMD_PAT_004_resolver_solicitud_usa_create_native_query_con_sus_parametros_reales() {
        command.resolverSolicitudRevisionAsistencia(new ResolverSolicitudRevisionAsistenciaRepositoryDTO(
                SOLICITUD, DOCENTE, "APROBADA", "Respuesta", EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_resolver_solicitud_revision_asistencia"));
        verify(query).setParameter("idSolicitud", SOLICITUD);
        verify(query).setParameter("idDocente", DOCENTE);
        verify(query).setParameter("accion", "APROBADA");
        verify(query).setParameter("respuestaDocente", "Respuesta");
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void CMD_PAT_005_ningun_command_abre_transaccion_jpa_ni_consulta_el_entity_manager_mas_alla_del_patron() {
        command.registrarAsistenciaAutonoma(new RegistrarAsistenciaAutonomaRepositoryDTO(
                ESTUDIANTE, SESION, "1234", EJECUTOR));

        verify(entityManager).createNativeQuery(anyString());
        verifyNoMoreInteractions(entityManager);
    }

    @Test
    void CMD_PAT_006_fila_que_no_cumple_el_contrato_canonico_se_traduce_a_contrato_db_no_a_fallo_tecnico() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "x"}));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.resolverSolicitudRevisionAsistencia(new ResolverSolicitudRevisionAsistenciaRepositoryDTO(
                        SOLICITUD, DOCENTE, "APROBADA", "Respuesta", EJECUTOR)));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    @Test
    void CMD_PAT_007_correlacion_devuelta_distinta_es_contrato_db_y_no_se_ignora() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{UUID.randomUUID(), "m", "", true}));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.registrarAsistenciaAutonoma(new RegistrarAsistenciaAutonomaRepositoryDTO(
                        ESTUDIANTE, SESION, "1234", EJECUTOR)));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    @Test
    void CMD_PAT_008_fallo_de_persistencia_se_traduce_a_error_tecnico_con_causa_y_no_se_traga() {
        when(query.getResultList()).thenThrow(new PersistenceException("sin conexion"));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.solicitarRevisionAsistencia(new SolicitarRevisionAsistenciaRepositoryDTO(
                        ESTUDIANTE, SESION, "NOTA", "J", null, EJECUTOR)));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof PersistenceException);
    }

    @Test
    void CMD_PAT_009_dto_nulo_mantiene_la_excepcion_del_baseline_antes_de_tocar_el_entity_manager() {
        assertThrows(CrosscuttingException.class,
                () -> command.registrarAsistenciaAutonoma(null));

        verify(entityManager, never()).createNativeQuery(anyString());
    }
}




