package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
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

import java.time.LocalDateTime;
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
 * LB-008 JPA-02B.1 — RED de patron para los cuatro commands de Sesion. Fija el patron certificado en
 * Asistencia: {@code EntityManager} + {@code createNativeQuery("EXEC dbo.usp_xxx ...")} + binding nombrado +
 * {@code getResultList()} + {@link co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultMapper}
 * + {@link co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultValidator}.
 *
 * <p>Unit de ESTRUCTURA con {@code EntityManager} fake. No certifica SQL Server ni transacciones: la paridad
 * real la certifica {@code SesionGrupoCommandsSpParityIT}.</p>
 */
class SesionJpaCommandPatternTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SESION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID GRUPO = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID DOCENTE = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 3, 2, 8, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 3, 2, 10, 0);

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private SesionJpaRepository command;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        command = new SesionJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "Operacion exitosa.", "", true}));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void SES_PAT_001_crear_sesion_usa_native_query_exec_y_binding_nombrado() {
        command.crearSesion(new CrearSesionRepositoryDTO(GRUPO, "Sesion 1", INICIO, FIN, EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_sesion"));
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("nombre", "Sesion 1");
        verify(query).setParameter("fechaHoraInicio", INICIO);
        verify(query).setParameter("fechaHoraFin", FIN);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void SES_PAT_002_actualizar_sesion_usa_native_query_exec_y_binding_nombrado() {
        command.actualizarSesion(new ActualizarSesionRepositoryDTO(SESION, "Sesion 1 editada", INICIO, FIN, EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_actualizar_sesion"));
        verify(query).setParameter("idSesion", SESION);
        verify(query).setParameter("nombre", "Sesion 1 editada");
        verify(query).setParameter("fechaHoraInicio", INICIO);
        verify(query).setParameter("fechaHoraFin", FIN);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
    }

    @Test
    void SES_PAT_003_cerrar_sesion_usa_native_query_exec_y_binding_nombrado() {
        command.cerrarSesion(new CerrarSesionRepositoryDTO(SESION, DOCENTE, "observacion", EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_cerrar_sesion"));
        verify(query).setParameter("idSesion", SESION);
        verify(query).setParameter("idDocente", DOCENTE);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
    }

    @Test
    void SES_PAT_004_generar_sesiones_grupo_usa_native_query_exec_y_binding_nombrado() {
        // CULM-001 B.1 (2.7.6): usp_generar_sesiones_grupo_v2 agrega sesionesCanceladasPorFestivo y sesionesFestivoConAsistencia.
        when(query.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{CORRELATION, "Operacion exitosa.", "", true, 2, 1}));

        final var generacion = command.generarSesionesGrupo(new GenerarSesionesGrupoRepositoryDTO(GRUPO, EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_generar_sesiones_grupo_v2"));
        assertEquals(2, generacion.sesionesCanceladasPorFestivo());
        assertEquals(1, generacion.sesionesFestivoConAsistencia());
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
    }

    @Test
    void SES_PAT_005_fallo_tecnico_de_jpa_se_traduce_a_database_operation_error_conservando_causa() {
        final PersistenceException causa = new PersistenceException("sp inexistente");
        when(query.getResultList()).thenThrow(causa);

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.generarSesionesGrupo(new GenerarSesionesGrupoRepositoryDTO(GRUPO, EJECUTOR)));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof PersistenceException);
    }

    @Test
    void SES_PAT_006_correlacion_distinta_en_el_resultado_es_violacion_de_contrato() {
        when(query.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{UUID.randomUUID(), "Operacion exitosa.", "", true}));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.cerrarSesion(new CerrarSesionRepositoryDTO(SESION, DOCENTE, "obs", EJECUTOR)));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }
}




