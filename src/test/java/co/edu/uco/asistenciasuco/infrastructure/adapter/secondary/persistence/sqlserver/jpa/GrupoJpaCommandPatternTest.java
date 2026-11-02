package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
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
import org.springframework.transaction.support.TransactionOperations;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.FranjaHorarioRepositoryDTO;
import org.mockito.InOrder;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LB-008 JPA-02B.2 — RED de patron para los tres commands de Grupo. Mismo patron certificado que Asistencia.
 * {@code usp_registrar_estudiante_en_grupo} es el provider publico vigente (TD-043 CERRADO):
 * reemplaza a {@code usp_registrar_estudiante_en_grupo_usuario_no_existente} y recibe
 * {@code idUsuarioEjecutor} e {@code idTipoIdIdentificacion}.
 */
class GrupoJpaCommandPatternTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID GRUPO = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID ASIGNATURA = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID PERIODO = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID DOCENTE = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("12121212-1212-1212-1212-121212121212");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final TransactionOperations transactionOperations = mock(TransactionOperations.class);
    private GrupoJpaRepository command;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        command = new GrupoJpaRepository(entityManager, transactionOperations, new JpaProcedureExecutor(entityManager));
        when(transactionOperations.execute(any())).thenAnswer(invocation ->
                ((org.springframework.transaction.support.TransactionCallback<?>) invocation.getArgument(0))
                        .doInTransaction(null));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "Grupo creado.", "", true}));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void GRP_PAT_001_crear_grupo_usa_native_query_exec_y_devuelve_el_resultado_validado() {
        final var result = command.crearGrupo(new CrearGrupoRepositoryDTO(
                GRUPO, ASIGNATURA, PERIODO, 1, "Grupo A", DOCENTE, EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_grupo"));
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("idAsignatura", ASIGNATURA);
        verify(query).setParameter("idPeriodoAcademico", PERIODO);
        verify(query).setParameter("codigo", 1);
        verify(query).setParameter("nombre", "Grupo A");
        verify(query).setParameter("idDocente", DOCENTE);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
        assertEquals("Grupo creado.", result.mensajeUsuario());
    }

    @Test
    void GRP_PAT_002_actualizar_grupo_usa_native_query_exec_y_binding_nombrado() {
        final var result = command.actualizarGrupo(new ActualizarGrupoRepositoryDTO(
                GRUPO, 2, "Grupo B", DOCENTE, 30, EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_actualizar_grupo"));
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("codigo", 2);
        verify(query).setParameter("nombre", "Grupo B");
        verify(query).setParameter("idDocente", DOCENTE);
        verify(query).setParameter("cupoMaximo", 30);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
        assertEquals("Grupo creado.", result.mensajeUsuario());
    }

    @Test
    void GRP_PAT_005_crear_grupo_con_franjas_guarda_el_horario_y_precarga_sus_sesiones_en_la_misma_transaccion() {
        command.crearGrupo(new CrearGrupoRepositoryDTO(GRUPO, ASIGNATURA, PERIODO, 1, "Grupo A", DOCENTE, EJECUTOR,
                List.of(new FranjaHorarioRepositoryDTO("LU", LocalTime.of(8, 0), LocalTime.of(10, 0)),
                        new FranjaHorarioRepositoryDTO("MI", LocalTime.of(14, 0), LocalTime.of(16, 30)))));

        final InOrder orden = inOrder(entityManager);
        orden.verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_grupo"));
        orden.verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_guardar_horarios_grupo"));
        orden.verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_precargar_sesiones_grupo"));
        verify(transactionOperations).execute(any());
        verify(query).setParameter("horariosJSON",
                "[{\"codigoDia\":\"LU\",\"horaInicio\":\"08:00\",\"horaFin\":\"10:00\"},"
                        + "{\"codigoDia\":\"MI\",\"horaInicio\":\"14:00\",\"horaFin\":\"16:30\"}]");
    }

    @Test
    void GRP_PAT_006_sin_franjas_no_toca_el_horario_ni_las_sesiones() {
        command.actualizarGrupo(new ActualizarGrupoRepositoryDTO(GRUPO, 2, "Grupo B", DOCENTE, 30, EJECUTOR));

        verify(entityManager, never()).createNativeQuery(contains("usp_guardar_horarios_grupo"));
        verify(entityManager, never()).createNativeQuery(contains("usp_precargar_sesiones_grupo"));
    }

    @Test
    void GRP_PAT_007_actualizar_grupo_con_franjas_regenera_las_sesiones_futuras_con_el_ejecutor() {
        command.actualizarGrupo(new ActualizarGrupoRepositoryDTO(GRUPO, 2, "Grupo B", DOCENTE, 30, EJECUTOR,
                List.of(new FranjaHorarioRepositoryDTO("LU", LocalTime.of(8, 0), LocalTime.of(10, 0)))));

        final InOrder orden = inOrder(entityManager);
        orden.verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_actualizar_grupo"));
        orden.verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_guardar_horarios_grupo"));
        orden.verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_precargar_sesiones_grupo"));
        verify(query, org.mockito.Mockito.atLeastOnce()).setParameter("idUsuarioEjecutor", EJECUTOR);
    }

    @Test
    void GRP_PAT_003_registrar_estudiante_usa_el_provider_publico_y_propaga_ejecutor_y_tipo_identificacion() {
        command.registrarEstudianteEnGrupo(new RegistrarEstudianteRepositoryDTO(
                TIPO_IDENTIFICACION, 123456, "PEREZ", "GOMEZ", "ANA", "MARIA",
                "ana@example.test", "Clave123!", GRUPO, EJECUTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_registrar_estudiante_en_grupo"));
        verify(query).setParameter("idTipoIdIdentificacion", TIPO_IDENTIFICACION);
        verify(query).setParameter("numeroIdentificacion", 123456);
        verify(query).setParameter("primerApellido", "PEREZ");
        verify(query).setParameter("segundoApellido", "GOMEZ");
        verify(query).setParameter("primerNombre", "ANA");
        verify(query).setParameter("segundoNombre", "MARIA");
        verify(query).setParameter("correo", "ana@example.test");
        verify(query).setParameter("password", "Clave123!");
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).getResultList();
    }

    @Test
    void GRP_PAT_004_sp_ausente_o_fallo_tecnico_se_clasifica_como_database_operation_error() {
        when(query.getResultList()).thenThrow(new PersistenceException("Could not find stored procedure"));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> command.registrarEstudianteEnGrupo(new RegistrarEstudianteRepositoryDTO(
                        TIPO_IDENTIFICACION, 123456, "PEREZ", "GOMEZ", "ANA", "MARIA",
                        "ana@example.test", "Clave123!", GRUPO, EJECUTOR)));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertTrue(exception.getCause() instanceof PersistenceException);
    }
}




