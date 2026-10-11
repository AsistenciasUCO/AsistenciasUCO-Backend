package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.AsignarDocenteAGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarDocenteDesdeUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.*;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.HydratedViewRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsignaturaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteGrupoQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.SesionMateriaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalDateTime;
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
    void comandos_de_docente_sin_contrato_db_publico_no_ejecutan_nada() {
        final DocenteJpaRepository repository = new DocenteJpaRepository(manager);

        assertThrows(FeatureUnavailableException.class, () -> repository.registrarDocenteDesdeUsuario(
                new RegistrarDocenteDesdeUsuarioRepositoryDTO(ID)));
        assertThrows(FeatureUnavailableException.class, () -> repository.asignarDocenteAGrupo(
                new AsignarDocenteAGrupoRepositoryDTO()));
        assertThrows(CrosscuttingException.class, () -> repository.registrarDocenteDesdeUsuario(null));
        assertThrows(CrosscuttingException.class, () -> repository.asignarDocenteAGrupo(null));
        verifyNoInteractions(manager);
    }

    @Test
    void sesiones_por_id_y_por_grupo() {
        final UvSesionEntity sesion = HydratedViewRow.of(UvSesionEntity.class, Map.of("id", ID, "idGrupo", OTHER,
                "nombre", "Parcial", "fechaHoraInicio", LocalDateTime.of(2026, 10, 8, 13, 0)));
        final TypedQuery<UvSesionEntity> porId = query(SesionJpaRepository.HQL_POR_ID, UvSesionEntity.class,
                List.of(sesion));
        final TypedQuery<UvSesionEntity> porGrupo = query(SesionJpaRepository.HQL_POR_GRUPO, UvSesionEntity.class,
                List.of(sesion, sesion));
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
        query(SesionJpaRepository.HQL_POR_ID, UvSesionEntity.class, List.of());
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
    void sesiones_de_materia_filtran_por_estudiante_y_asignatura() {
        final SesionMateriaQueryRow row = new SesionMateriaQueryRow(ID, "Parcial", 1, "S-1", 2, OTHER, null, "G1",
                LocalDateTime.of(2026, 10, 8, 13, 0), null);
        final TypedQuery<SesionMateriaQueryRow> sesiones = query(
                SesionMateriaEstudianteJpaRepository.HQL_POR_ESTUDIANTE_Y_ASIGNATURA, SesionMateriaQueryRow.class,
                List.of(row));

        final var result = new SesionMateriaEstudianteJpaRepository(manager).consultarSesionesMateria(ID, OTHER);

        verify(sesiones).setParameter("idEstudiante", ID);
        verify(sesiones).setParameter("idAsignatura", OTHER);
        assertNull(result.getFirst().codigoGrupo());
        assertEquals(LocalDateTime.of(2026, 10, 8, 13, 0), result.getFirst().fechaHoraInicio());
    }

    @Test
    void grupos_y_estudiantes_de_grupo() {
        query(GrupoJpaRepository.HQL_GRUPOS, UvGrupoEntity.class, List.of(HydratedViewRow.of(UvGrupoEntity.class,
                Map.of("id", ID, "codigo", 508, "grupoEstaHablitado", 1))));
        final TypedQuery<EstudianteGrupoQueryRow> estudiantes = query(GrupoJpaRepository.HQL_ESTUDIANTES,
                EstudianteGrupoQueryRow.class,
                List.of(new EstudianteGrupoQueryRow(ID, OTHER, null, "Ana", "ana@uco.edu.co", "A", "Activo")));
        final GrupoJpaRepository repository = new GrupoJpaRepository(manager, mock(TransactionOperations.class),
                new JpaProcedureExecutor(manager));

        assertTrue(repository.consultarGrupos().getFirst().isGrupoHabilitado());
        assertNull(repository.consultarEstudiantesGrupo(OTHER).getFirst().documento());
        verify(estudiantes).setParameter("idGrupo", OTHER);
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
    void fallo_del_provider_en_consultas_academicas_no_devuelve_listas_vacias() {
        when(manager.createQuery(anyString(), any(Class.class))).thenThrow(new PersistenceException("db down"));

        assertThrows(DatabaseOperationException.class, () -> new PeriodoAcademicoJpaRepository(manager)
                .consultarPeriodosAcademicos());
        assertThrows(DatabaseOperationException.class, () -> new FacultadJpaRepository(manager)
                .consultarFacultadPorId(ID));
        assertThrows(DatabaseOperationException.class, () -> new SesionMateriaEstudianteJpaRepository(manager)
                .consultarSesionesMateria(ID, OTHER));
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
