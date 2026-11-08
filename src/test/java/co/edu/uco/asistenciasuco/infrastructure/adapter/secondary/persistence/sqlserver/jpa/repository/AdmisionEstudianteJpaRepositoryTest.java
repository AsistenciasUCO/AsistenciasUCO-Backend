package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AdmisionEstudianteCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AdmisionEstudianteResultadoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaInstitucionProjection;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CULM-001 D.4 (CONTRACT §5.2): adaptadores JPA de la admision. {@code usp_admitir_estudiante} por {@code JpaProcedureExecutor}
 * (4 canonicas + 4 columnas; los rechazos ADS_* se traducen a su HTTP) y {@code usp_consultar_programas_institucion_paginado}.
 */
class AdmisionEstudianteJpaRepositoryTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID TIPO = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private AdmisionEstudianteJpaRepository admision() {
        return new AdmisionEstudianteJpaRepository(new JpaProcedureExecutor(manager));
    }

    private static AdmisionEstudianteCommand orden() {
        return new AdmisionEstudianteCommand(TIPO, 1017123456, "Ana", null, "Perez", "Gomez", "ana@uco.edu.co", "{hash}", PROGRAMA, ADMIN);
    }

    private static Object[] fila(final boolean exitoso, final String tecnico, final Object idUsuario, final Object idEstudiante,
                                 final Object usuarioNuevo, final Object estudianteNuevo) {
        return new Object[]{CORRELACION.toString(), "mensaje", tecnico, exitoso, idUsuario, idEstudiante, usuarioNuevo, estudianteNuevo};
    }

    @Test
    void admitir_ejecuta_el_sp_con_todos_los_parametros_y_mapea_las_4_columnas_extra() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(
                fila(true, "DBCODE=ADS_001|ok", USUARIO.toString(), ESTUDIANTE.toString(), Boolean.TRUE, 0)));

        final AdmisionEstudianteResultadoProjection resultado = admision().admitir(orden());

        assertEquals(new AdmisionEstudianteResultadoProjection(USUARIO, ESTUDIANTE, true, false), resultado);
        verify(manager).createNativeQuery(AdmisionEstudianteJpaRepository.SQL_ADMITIR);
        assertTrue(AdmisionEstudianteJpaRepository.SQL_ADMITIR.contains("usp_admitir_estudiante"));
        verify(query).setParameter("idTipoIdIdentificacion", TIPO);
        verify(query).setParameter("numeroIdentificacion", 1017123456);
        verify(query).setParameter("primerNombre", "Ana");
        verify(query).setParameter("segundoNombre", null);
        verify(query).setParameter("primerApellido", "Perez");
        verify(query).setParameter("segundoApellido", "Gomez");
        verify(query).setParameter("correo", "ana@uco.edu.co");
        verify(query).setParameter("password", "{hash}");
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(query).setParameter("idUsuarioEjecutor", ADMIN);
    }

    @Test
    void los_rechazos_ADS_se_traducen_a_su_http() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(
                fila(false, "DBCODE=ADS_002|ya admitido", null, null, null, null)));
        final ConflictException ya = assertThrows(ConflictException.class, () -> admision().admitir(orden()));
        assertEquals("ADS_002", ya.getCode());

        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(
                fila(false, "DBCODE=ADS_004|personas distintas", null, null, null, null)));
        assertEquals("ADS_004", assertThrows(ConflictException.class, () -> admision().admitir(orden())).getCode());

        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(
                fila(false, "DBCODE=ADS_003|programa ajeno", null, null, null, null)));
        assertThrows(ForbiddenException.class, () -> admision().admitir(orden()));

        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(
                fila(false, "DBCODE=ADS_005|correo", null, null, null, null)));
        assertThrows(ValidationException.class, () -> admision().admitir(orden()));

        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(
                fila(false, "DBCODE=ADS_006|clave", null, null, null, null)));
        assertThrows(ValidationException.class, () -> admision().admitir(orden()));
    }

    @Test
    void una_fila_exitosa_sin_ids_es_un_fallo_de_contrato() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(
                fila(true, "DBCODE=ADS_001|ok", null, null, null, null)));

        assertThrows(DatabaseOperationException.class, () -> admision().admitir(orden()));
    }

    @Test
    void orden_nula_o_sin_ejecutor_no_llega_a_la_db() {
        assertThrows(CrosscuttingException.class, () -> admision().admitir(null));
        assertThrows(CrosscuttingException.class, () -> admision().admitir(new AdmisionEstudianteCommand(
                TIPO, 1, "Ana", null, "Perez", null, "a@uco.edu.co", null, PROGRAMA, null)));
        verify(manager, never()).createNativeQuery(anyString());
        assertThrows(NullPointerException.class, () -> new AdmisionEstudianteJpaRepository(null));
    }

    // ---- programas de la institucion ----

    private ProgramaInstitucionJpaRepository programas() {
        return new ProgramaInstitucionJpaRepository(manager);
    }

    @Test
    void programas_llama_al_sp_con_el_ambito_la_pagina_y_los_filtros_y_mapea_5_columnas() {
        final UUID facultad = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.<Object>of(
                new Object[]{PROGRAMA.toString(), " Sistemas ", facultad.toString(), "Ingenieria", 2},
                new Object[]{UUID.randomUUID().toString(), "Derecho", null, null, 2}));

        final PaginaResultado<ProgramaInstitucionProjection> pagina = programas().consultarProgramas(ADMIN, "sis", facultad, 1, 10);

        assertEquals(2, pagina.totalElementos());
        assertEquals(new ProgramaInstitucionProjection(PROGRAMA, "Sistemas", facultad, "Ingenieria"), pagina.elementos().getFirst());
        assertEquals(null, pagina.elementos().get(1).idFacultad());
        verify(manager).createNativeQuery(ProgramaInstitucionJpaRepository.SQL_PROGRAMAS);
        assertTrue(ProgramaInstitucionJpaRepository.SQL_PROGRAMAS.contains("usp_consultar_programas_institucion_paginado"));
        assertFalse(ProgramaInstitucionJpaRepository.SQL_PROGRAMAS.toLowerCase().contains("uv_programa"));
        verify(query).setParameter("idUsuarioEjecutor", ADMIN);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(query).setParameter("numeroPagina", 1);
        verify(query).setParameter("tamanoPagina", 10);
        verify(query).setParameter("texto", "sis");
        verify(query).setParameter("idFacultad", facultad);
    }

    @Test
    void programas_sin_filas_tiene_total_cero_y_sin_ejecutor_no_llega_a_la_db() {
        when(query.getResultList()).thenReturn(List.of());
        assertEquals(0, programas().consultarProgramas(ADMIN, null, null, 1, 50).totalElementos());

        assertThrows(CrosscuttingException.class, () -> programas().consultarProgramas(null, null, null, 1, 50));
    }

    @Test
    void programas_traduce_los_rechazos_del_sp_y_valida_las_columnas() {
        doThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_001|perfil"))).when(query).getResultList();
        assertThrows(ForbiddenException.class, () -> programas().consultarProgramas(ADMIN, null, null, 1, 50));

        doThrow(new PersistenceException("x", new RuntimeException("DBCODE=PAG_001|pagina"))).when(query).getResultList();
        assertThrows(ValidationException.class, () -> programas().consultarProgramas(ADMIN, null, null, 0, 50));

        doThrow(new PersistenceException("db down")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> programas().consultarProgramas(ADMIN, null, null, 1, 50));

        org.mockito.Mockito.reset(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[4]));
        assertInstanceOf(IllegalStateException.class, assertThrows(DatabaseOperationException.class,
                () -> programas().consultarProgramas(ADMIN, null, null, 1, 50)).getCause());
        assertThrows(NullPointerException.class, () -> new ProgramaInstitucionJpaRepository(null));
    }
}
