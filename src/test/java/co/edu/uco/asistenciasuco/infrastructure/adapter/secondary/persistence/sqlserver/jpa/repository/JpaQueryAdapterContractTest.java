package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.*;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.HydratedViewRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsignaturaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaSesionEstudianteQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * QUALITY-PR15 J05/J07: contrato unitario de los repositorios JPA de consulta frente al
 * {@link EntityManager}: consulta usada, parametros enlazados, cardinalidad 0/1/N, entradas nulas y
 * comandos no disponibles. EntityManager simulado: NO certifica SQL Server; la paridad real esta
 * en {@code CoreViewQueriesJpaParityIT} y {@code AcademicQueryJpaParityIT}.
 */
class JpaQueryAdapterContractTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private final EntityManager manager = mock(EntityManager.class);

    @Test
    void docentes_listado_detalle_y_asignaciones() {
        final UvDocenteIdentidadEntity identidad = HydratedViewRow.of(UvDocenteIdentidadEntity.class,
                Map.of("id", ID, "idUsuario", OTHER, "numeroIdentificacion", 1037, "nombreCompleto", "Luz Mar",
                        "estaActivoUsuario", Boolean.TRUE));
        query(DocenteJpaRepository.HQL_TODOS, UvDocenteIdentidadEntity.class, List.of(identidad));
        final TypedQuery<UvDocenteIdentidadEntity> porId =
                query(DocenteJpaRepository.HQL_POR_ID, UvDocenteIdentidadEntity.class, List.of(identidad));
        final TypedQuery<UvDocenteEntity> asignaciones = query(DocenteJpaRepository.HQL_ASIGNACIONES,
                UvDocenteEntity.class, List.of(HydratedViewRow.of(UvDocenteEntity.class,
                        Map.of("id", ID, "nombreAsignatura", "Calculo", "inpPlanEstudio", 77))));
        final DocenteJpaRepository repository = new DocenteJpaRepository(manager);

        assertEquals("Luz Mar", repository.consultarDocentes().getFirst().getNombreCompleto());
        assertEquals(ID, repository.consultarDocentePorId(new ConsultarDocentePorIdRepositoryDTO(ID))
                .orElseThrow().getId());
        verify(porId).setParameter("idDocente", ID);
        verify(porId).setMaxResults(1);
        assertEquals("77", repository.consultarAsignacionesAcademicas(
                new ConsultarAsignacionesAcademicasDocenteRepositoryDTO(ID)).getFirst().getInpPlanEstudio());
        verify(asignaciones).setParameter("idDocente", ID);
    }

    @Test
    void docente_inexistente_y_entradas_nulas() {
        query(DocenteJpaRepository.HQL_POR_ID, UvDocenteIdentidadEntity.class, List.of());
        final DocenteJpaRepository repository = new DocenteJpaRepository(manager);

        assertEquals(Optional.empty(), repository.consultarDocentePorId(new ConsultarDocentePorIdRepositoryDTO(ID)));
        assertThrows(CrosscuttingException.class, () -> repository.consultarDocentePorId(null));
        assertThrows(CrosscuttingException.class, () -> repository.consultarAsignacionesAcademicas(null));
    }

    @Test
    void sesiones_por_id_y_por_grupo() {
        final UvSesionEntity sesion = HydratedViewRow.of(UvSesionEntity.class, Map.of("id", ID, "idGrupo", OTHER,
                "nombre", "Parcial", "fechaHoraInicio", LocalDateTime.of(2026, 10, 8, 13, 0)));
        // CULM-001 B.1 (3.2.4): la lectura v1 se une a uv_sesion_estado y cada fila es [UvSesionEntity, UvSesionEstadoEntity].
        final TypedQuery<Object[]> porId = query(SesionJpaRepository.HQL_POR_ID, Object[].class,
                List.<Object[]>of(new Object[]{sesion, null}));
        final TypedQuery<Object[]> porGrupo = query(SesionJpaRepository.HQL_POR_GRUPO, Object[].class,
                List.<Object[]>of(new Object[]{sesion, null}, new Object[]{sesion, null}));
        final SesionJpaRepository repository = new SesionJpaRepository(manager, new JpaProcedureExecutor(manager));

        assertEquals(LocalDateTime.of(2026, 10, 8, 13, 0),
                repository.consultarSesion(new ConsultarSesionRepositoryDTO(ID)).getFechaHoraInicio());
        verify(porId).setParameter("idSesion", ID);
        verify(porId).setMaxResults(1);
        assertEquals(2, repository.consultarSesionesPorGrupo(OTHER).size());
        verify(porGrupo).setParameter("idGrupo", OTHER);
    }

    @Test
    void sesion_inexistente_conserva_null_y_entradas_nulas_se_rechazan() {
        query(SesionJpaRepository.HQL_POR_ID, Object[].class, List.of());
        final SesionJpaRepository repository = new SesionJpaRepository(manager, new JpaProcedureExecutor(manager));

        assertNull(repository.consultarSesion(new ConsultarSesionRepositoryDTO(ID)));
        assertThrows(CrosscuttingException.class, () -> repository.consultarSesion(null));
        assertThrows(CrosscuttingException.class, () -> repository.consultarSesionesPorGrupo(null));
    }

    @Test
    void asignaturas_por_programa_y_por_plan() {
        final AsignaturaQueryRow row = new AsignaturaQueryRow(ID, "MAT01", "Calculo", 4, OTHER, "Ciencias", null,
                null, null, OTHER, ID, "Sistemas", "1", Boolean.TRUE, "Activa");
        final TypedQuery<AsignaturaQueryRow> porPrograma =
                query(AsignaturaJpaRepository.HQL_POR_PROGRAMA, AsignaturaQueryRow.class, List.of(row));
        final TypedQuery<AsignaturaQueryRow> porPlan =
                query(AsignaturaJpaRepository.HQL_POR_PLAN, AsignaturaQueryRow.class, List.of());
        final AsignaturaJpaRepository repository =
                new AsignaturaJpaRepository(manager, new JpaProcedureExecutor(manager));

        assertTrue(repository.consultarAsignaturasPorPrograma(ID).getFirst().estaActivaAsignatura());
        verify(porPrograma).setParameter("idPrograma", ID);
        assertTrue(repository.consultarAsignaturasPorPlan(OTHER).isEmpty());
        verify(porPlan).setParameter("idPlanEstudio", OTHER);
    }

    @Test
    void periodos_y_facultades_listado_y_detalle() {
        final UvPeriodoAcademicoEntity periodo = HydratedViewRow.of(UvPeriodoAcademicoEntity.class,
                Map.of("id", ID, "nombre", "2026-2", "codigo", 20262));
        query(PeriodoAcademicoJpaRepository.HQL_LISTAR, UvPeriodoAcademicoEntity.class, List.of(periodo));
        final TypedQuery<UvPeriodoAcademicoEntity> periodoPorId =
                query(PeriodoAcademicoJpaRepository.HQL_POR_ID, UvPeriodoAcademicoEntity.class, List.of());
        final PeriodoAcademicoJpaRepository periodos = new PeriodoAcademicoJpaRepository(manager);

        assertEquals("20262", periodos.consultarPeriodosAcademicos().getFirst().codigo());
        assertEquals(Optional.empty(), periodos.consultarPeriodoAcademicoPorId(ID));
        verify(periodoPorId).setParameter("idPeriodoAcademico", ID);
        verify(periodoPorId).setMaxResults(1);

        final UvFacultadEntity facultad = HydratedViewRow.of(UvFacultadEntity.class,
                Map.of("id", ID, "nombreFacultad", "Ingenieria", "estaActivaFacultad", Boolean.TRUE));
        query(FacultadJpaRepository.HQL_LISTAR, UvFacultadEntity.class, List.of(facultad));
        final TypedQuery<UvFacultadEntity> facultadPorId =
                query(FacultadJpaRepository.HQL_POR_ID, UvFacultadEntity.class, List.of(facultad));
        final FacultadJpaRepository facultades = new FacultadJpaRepository(manager);

        assertEquals("Ingenieria", facultades.consultarFacultades().getFirst().nombreFacultad());
        assertTrue(facultades.consultarFacultadPorId(ID).orElseThrow().estaActivaFacultad());
        verify(facultadPorId).setParameter("idFacultad", ID);
    }

    @Test
    void decanos_con_y_sin_facultad_y_variantes_de_estado() {
        final UUID decano2 = UUID.randomUUID();
        final UUID decano3 = UUID.randomUUID();
        query(DecanoJpaRepository.HQL_LISTAR, Object[].class, List.of(
                new Object[]{ID, OTHER, 1037600111, "Ana Perez", ID, "Ingenieria", 1},
                new Object[]{decano2, OTHER, null, "Luis Gomez", null, null, 0},
                new Object[]{decano3, OTHER, 987654321, "Carlos Ruiz", null, null, null}
        ));
        final DecanoJpaRepository decanos = new DecanoJpaRepository(manager, new JpaProcedureExecutor(manager));

        final var resultado = decanos.consultarDecanos();

        assertEquals(3, resultado.size());
        assertEquals("1037600111", resultado.get(0).numeroIdentificacion());
        assertEquals(ID, resultado.get(0).idFacultad());
        assertEquals("Ingenieria", resultado.get(0).nombreFacultad());
        assertTrue(resultado.get(0).estaActivoDecano());

        assertNull(resultado.get(1).numeroIdentificacion());
        assertNull(resultado.get(1).idFacultad());
        assertNull(resultado.get(1).nombreFacultad());
        assertFalse(resultado.get(1).estaActivoDecano());

        assertEquals("987654321", resultado.get(2).numeroIdentificacion());
        assertNull(resultado.get(2).idFacultad());
        assertNull(resultado.get(2).nombreFacultad());
        assertFalse(resultado.get(2).estaActivoDecano());
    }

    @Test
    void asistencia_de_sesiones_filtra_por_estudiante_resuelto_y_asignatura_y_conserva_nulos() {
        final var sinDetalle = new AsistenciaSesionEstudianteQueryRow(ID, null, null, null, null, null, null);
        final var conSolicitud = new AsistenciaSesionEstudianteQueryRow(OTHER, 3, Boolean.FALSE, "SJC", ID,
                "pendiente", java.time.LocalDate.of(2026, 8, 28));
        final TypedQuery<AsistenciaSesionEstudianteQueryRow> asistencia = query(
                AsistenciaSesionEstudianteJpaRepository.HQL_POR_ESTUDIANTE_Y_ASIGNATURA,
                AsistenciaSesionEstudianteQueryRow.class, List.of(sinDetalle, conSolicitud));

        final var result = new AsistenciaSesionEstudianteJpaRepository(manager).consultarAsistenciaSesiones(ID, OTHER);

        verify(asistencia).setParameter("idEstudiante", ID);
        verify(asistencia).setParameter("idAsignatura", OTHER);
        assertEquals(2, result.size());
        assertNull(result.getFirst().asistio());
        assertNull(result.getFirst().codigoRazonCausa());
        assertNull(result.getFirst().idSolicitud());
        assertEquals(Boolean.FALSE, result.get(1).asistio());
        assertEquals("pendiente", result.get(1).estadoSolicitud());
        assertEquals(3, result.get(1).codigoDetalle());
    }

    // CULM-001 A.2-2 (A2-B-05): consultarGrupos(actor) recorre las paginas de usp_consultar_grupos_paginado hasta
    // totalRegistros y une los horarios; los estudiantes del grupo salen de usp_consultar_estudiantes_grupo_ambito_paginado.
    @Test
    void grupos_y_estudiantes_de_grupo() {
        final UUID actor = UUID.randomUUID();
        final UUID segundo = UUID.randomUUID();
        co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.set(UUID.randomUUID());
        try {
            final jakarta.persistence.Query paginaGrupos = mock(jakarta.persistence.Query.class, RETURNS_SELF);
            when(paginaGrupos.getResultList()).thenReturn(
                    java.util.Collections.<Object>singletonList(grupoFila(ID, 2)),
                    java.util.Collections.<Object>singletonList(grupoFila(segundo, 2)));
            when(manager.createNativeQuery(eq(GrupoJpaRepository.SQL_GRUPOS_PAGINADO))).thenReturn(paginaGrupos);
            query(GrupoJpaRepository.HQL_HORARIOS_DE_GRUPOS, UvHorarioEntity.class, List.of(
                    HydratedViewRow.of(UvHorarioEntity.class, Map.of("id", OTHER, "idGrupo", ID, "codigoDia", "MI", "nombreDia",
                            "Miercoles", "horaInicio", LocalTime.of(14, 0), "horaFin", LocalTime.of(16, 0))),
                    HydratedViewRow.of(UvHorarioEntity.class, Map.of("id", UUID.randomUUID(), "idGrupo", ID, "codigoDia", "LU",
                            "nombreDia", "Lunes", "horaInicio", LocalTime.of(8, 0), "horaFin", LocalTime.of(10, 0)))));
            final jakarta.persistence.Query estudiantes = mock(jakarta.persistence.Query.class, RETURNS_SELF);
            when(estudiantes.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(
                    new Object[]{ID, OTHER.toString(), null, "Ana", "ana@uco.edu.co", "A", "Activo", 1}));
            when(manager.createNativeQuery(eq(GrupoJpaRepository.SQL_ESTUDIANTES_GRUPO))).thenReturn(estudiantes);
            final GrupoJpaRepository repository = new GrupoJpaRepository(manager, mock(TransactionOperations.class),
                    new JpaProcedureExecutor(manager));

            final var grupos = repository.consultarGrupos(actor);

            assertEquals(List.of(ID, segundo), grupos.stream().map(g -> g.getId()).toList());
            assertTrue(grupos.getFirst().isGrupoHabilitado());
            assertEquals("508", grupos.getFirst().getCodigo());
            // F1: el listado trae el horario semanal ordenado de lunes a domingo
            assertEquals(List.of("LU", "MI"), grupos.getFirst().getFranjas().stream().map(franja -> franja.codigoDia()).toList());
            verify(paginaGrupos, org.mockito.Mockito.times(2)).setParameter("idUsuarioEjecutor", actor);
            verify(paginaGrupos).setParameter("numeroPagina", 1);
            verify(paginaGrupos).setParameter("numeroPagina", 2);
            verify(paginaGrupos, org.mockito.Mockito.times(2)).setParameter("tamanoPagina", GrupoJpaRepository.TAMANO_PAGINA_GRUPOS);

            final var filas = repository.consultarEstudiantesGrupo(OTHER, actor);
            assertNull(filas.getFirst().documento());
            assertEquals(OTHER, filas.getFirst().idEstudiante());
            verify(estudiantes).setParameter("idGrupo", OTHER);
            verify(estudiantes).setParameter("idUsuarioEjecutor", actor);
        } finally {
            co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.clear();
        }
    }

    private static Object[] grupoFila(final UUID id, final int total) {
        return new Object[]{id, 508, "Grupo", UUID.randomUUID(), "Backend", UUID.randomUUID(), 30, 10, 20, 1,
                java.time.LocalDate.of(2026, 1, 20), java.time.LocalDate.of(2026, 5, 30), total};
    }

    @Test
    void usuarios_por_correo_id_e_identificacion() {
        final UvUsuarioEntity usuario = HydratedViewRow.of(UvUsuarioEntity.class,
                Map.of("id", ID, "correo", "luz@uco.edu.co"));
        final TypedQuery<UvUsuarioEntity> porCorreo =
                query(UsuarioJpaRepository.HQL_POR_CORREO, UvUsuarioEntity.class, List.of(usuario));
        final TypedQuery<UvUsuarioEntity> porId =
                query(UsuarioJpaRepository.HQL_POR_ID, UvUsuarioEntity.class, List.of());
        final TypedQuery<UvUsuarioEntity> porIdentificacion =
                query(UsuarioJpaRepository.HQL_POR_IDENTIFICACION, UvUsuarioEntity.class, List.of(usuario));
        final UsuarioJpaRepository repository = new UsuarioJpaRepository(manager, new JpaProcedureExecutor(manager));

        assertEquals(ID, repository.consultarUsuarioPorCorreo("Luz@UCO.edu.co").orElseThrow().id());
        verify(porCorreo).setParameter("correo", "Luz@UCO.edu.co");
        assertEquals(Optional.empty(), repository.consultarUsuarioPorId(OTHER));
        verify(porId).setParameter("idUsuario", OTHER);
        assertEquals("luz@uco.edu.co", repository.consultarUsuarioPorIdentificacion(OTHER, 1037).orElseThrow().correo());
        verify(porIdentificacion).setParameter("tipoIdentificacionId", OTHER);
        verify(porIdentificacion).setParameter("numeroIdentificacion", 1037);
    }

    @Test
    void usuario_por_numero_de_identificacion_sin_tipo() {
        final UvUsuarioEntity usuario = HydratedViewRow.of(UvUsuarioEntity.class,
                Map.of("id", ID, "correo", "luz@uco.edu.co"));
        final TypedQuery<UvUsuarioEntity> porNumero = query(
                UsuarioJpaRepository.HQL_POR_NUMERO_IDENTIFICACION, UvUsuarioEntity.class, List.of(usuario));
        final UsuarioJpaRepository repository = new UsuarioJpaRepository(manager, new JpaProcedureExecutor(manager));

        assertEquals(ID, repository.consultarUsuarioPorNumeroIdentificacion(1037).orElseThrow().id());
        verify(porNumero).setParameter("numeroIdentificacion", 1037);
    }

    @Test
    void fallo_del_provider_en_consultas_academicas_no_devuelve_listas_vacias() {
        when(manager.createQuery(anyString(), any(Class.class))).thenThrow(new PersistenceException("db down"));

        assertThrows(DatabaseOperationException.class, () -> new PeriodoAcademicoJpaRepository(manager)
                .consultarPeriodosAcademicos());
        assertThrows(DatabaseOperationException.class, () -> new FacultadJpaRepository(manager)
                .consultarFacultadPorId(ID));
        assertThrows(DatabaseOperationException.class, () -> new AsistenciaSesionEstudianteJpaRepository(manager)
                .consultarAsistenciaSesiones(ID, OTHER));
        assertThrows(DatabaseOperationException.class, () -> new AsignaturaJpaRepository(manager,
                new JpaProcedureExecutor(manager)).consultarAsignaturasPorPlan(ID));
    }

    @SuppressWarnings("unchecked")
    private <T> TypedQuery<T> query(final String hql, final Class<T> type, final List<T> rows) {
        final TypedQuery<T> query = mock(TypedQuery.class, RETURNS_SELF);
        when(query.getResultList()).thenReturn(rows);
        when(manager.createQuery(eq(hql), eq(type))).thenReturn(query);
        return query;
    }
}
