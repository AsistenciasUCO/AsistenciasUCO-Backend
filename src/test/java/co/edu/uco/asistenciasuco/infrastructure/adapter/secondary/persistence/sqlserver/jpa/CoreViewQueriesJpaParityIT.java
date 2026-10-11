package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.*;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Oraculo temporal de paridad para JPA-04. JDBC representa el comportamiento BEFORE congelado y
 * las consultas JPA representan el AFTER. No se reutilizan RowMapper productivos para evitar que
 * ambos lados compartan la implementacion bajo prueba.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class CoreViewQueriesJpaParityIT {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private SesionJpaRepository sesiones;
    private GrupoJpaRepository grupos;
    private UsuarioJpaRepository usuarios;
    private DocenteJpaRepository docentes;
    private EstudianteJpaRepository estudiantes;
    private TipoIdentificacionJpaRepository tiposIdentificacion;

    @BeforeEach
    void setUp() {
        sesiones = new SesionJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        grupos = new GrupoJpaRepository(entityManager,
                org.mockito.Mockito.mock(org.springframework.transaction.support.TransactionOperations.class),
                new JpaProcedureExecutor(entityManager));
        usuarios = new UsuarioJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        docentes = new DocenteJpaRepository(entityManager);
        estudiantes = new EstudianteJpaRepository(entityManager);
        tiposIdentificacion = new TipoIdentificacionJpaRepository(entityManager);
    }

    @Test
    void sesiones_conservan_columnas_orden_utc_y_not_found() {
        final List<UUID> idsGrupo = jdbc.query("SELECT DISTINCT idGrupo FROM dbo.uv_sesion ORDER BY idGrupo",
                (rs, row) -> uuid(rs, "idGrupo"));
        assumeFalse(idsGrupo.isEmpty(), "No hay sesiones para comparar.");
        final UUID grupoId = idsGrupo.getFirst();

        final List<List<Object>> before = jdbc.query("""
                SELECT id, idGrupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo,
                       fechaHoraInicio, fechaHoraFin
                FROM dbo.uv_sesion
                WHERE idGrupo = ?
                ORDER BY fechaHoraInicio, numero, id
                """, (rs, row) -> sessionJdbc(rs), grupoId);
        final List<List<Object>> after = sesiones.consultarSesionesPorGrupo(grupoId).stream()
                .map(CoreViewQueriesJpaParityIT::sessionJpa).toList();

        assertEquals(before, after);
        assertEquals(before.getFirst(), sessionJpa(sesiones.consultarSesion(
                new ConsultarSesionRepositoryDTO((UUID) before.getFirst().getFirst()))));
        assertNull(sesiones.consultarSesion(new ConsultarSesionRepositoryDTO(UUID.randomUUID())));
    }

    @Test
    void grupos_y_estudiantes_de_grupo_conservan_columnas_nulos_y_orden() {
        final List<List<Object>> beforeGroups = jdbc.query("""
                SELECT id, codigo, nombre, idAsignatura, nombreAsignatura, idDocente,
                       capacidadMaximaPermitida, estudiantesActivos, cuposDisponibles,
                       grupoEstaHablitado, fechaInicioPeriodoAcademico, fechaFinPeriodoAcademico
                FROM dbo.uv_grupo
                ORDER BY nombreAsignatura, codigo, nombre, id
                """, (rs, row) -> groupJdbc(rs));
        assertEquals(beforeGroups, grupos.consultarGrupos().stream()
                .map(CoreViewQueriesJpaParityIT::groupJpa).toList());

        assumeFalse(beforeGroups.isEmpty(), "No hay grupos para comparar estudiantes.");
        final UUID grupoId = (UUID) beforeGroups.getFirst().getFirst();
        final List<List<Object>> beforeStudents = jdbc.query("""
                SELECT eg.id, eg.idEstudiante, ei.numeroIdentificacion AS documento,
                       ei.nombreCompleto, u.correo,
                       eg.codigoEstadoEstudiante AS codigoEstado,
                       eg.nombreEstadoEstudiante AS nombreEstado
                FROM dbo.uv_estudiante_grupo eg
                INNER JOIN dbo.uv_estudiante_identidad ei ON ei.id = eg.idEstudiante
                INNER JOIN dbo.uv_usuario u ON u.id = ei.idUsuario
                WHERE eg.idGrupo = ?
                ORDER BY ei.nombreCompleto, eg.id
                """, (rs, row) -> studentGroupJdbc(rs), grupoId);
        assertEquals(beforeStudents, grupos.consultarEstudiantesGrupo(grupoId).stream()
                .map(CoreViewQueriesJpaParityIT::studentGroupJpa).toList());
    }

    @Test
    void usuarios_conservan_identidad_y_semantica_optional() {
        final List<List<Object>> before = jdbc.query("""
                SELECT id, idTipoIdentificacion, numeroIdentificacion, primerNombre, primerApellido, correo
                FROM dbo.uv_usuario
                ORDER BY id
                """, (rs, row) -> userJdbc(rs));
        assumeFalse(before.isEmpty(), "No hay usuarios para comparar.");
        final List<Object> expected = before.getFirst();
        final UUID id = (UUID) expected.get(0);
        final UUID type = (UUID) expected.get(1);
        final Integer number = (Integer) expected.get(2);
        final String email = (String) expected.get(5);

        assertEquals(expected, userJpa(usuarios.consultarUsuarioPorId(id).orElseThrow()));
        assertEquals(expected, userJpa(usuarios.consultarUsuarioPorIdentificacion(type, number).orElseThrow()));
        assertEquals(expected, userJpa(usuarios.consultarUsuarioPorCorreo("  " + email.toUpperCase() + "  ").orElseThrow()));
        assertTrue(usuarios.consultarUsuarioPorId(UUID.randomUUID()).isEmpty());
    }

    @Test
    void docentes_conservan_identidad_asignaciones_orden_y_not_found() {
        final List<List<Object>> before = jdbc.query("""
                SELECT id, idUsuario, numeroIdentificacion, nombreCompleto, estaActivoUsuario
                FROM dbo.uv_docente_identidad
                ORDER BY nombreCompleto, id
                """, (rs, row) -> teacherIdentityJdbc(rs));
        assertEquals(before, docentes.consultarDocentes().stream()
                .map(CoreViewQueriesJpaParityIT::teacherIdentityJpa).toList());
        assumeFalse(before.isEmpty(), "No hay docentes para comparar.");
        final UUID teacherId = (UUID) before.getFirst().getFirst();
        assertEquals(before.getFirst(), teacherIdentityJpa(docentes.consultarDocentePorId(
                new ConsultarDocentePorIdRepositoryDTO(teacherId)).orElseThrow()));

        final List<List<Object>> beforeAssignments = jdbc.query("""
                SELECT id, idUsuario, numeroIdentificacion, nombreCompleto, estaActivoUsuario,
                       idInstitucion, nombreInstitucion, idFacultad, nombreFacultad, idPrograma,
                       nombrePrograma, idPlanEstudio, inpPlanEstudio, idAsignatura, nombreAsignatura,
                       idGrupo, nombreGrupo, idPerfil, codigoPerfil, nombrePerfil,
                       estaActivoDocente, estaActivoTextoDocente
                FROM dbo.uv_docente
                WHERE id = ?
                ORDER BY nombreInstitucion, nombreFacultad, nombrePrograma,
                         nombreAsignatura, nombreGrupo, idGrupo
                """, (rs, row) -> teacherAssignmentJdbc(rs), teacherId);
        assertEquals(beforeAssignments, docentes.consultarAsignacionesAcademicas(
                        new ConsultarAsignacionesAcademicasDocenteRepositoryDTO(teacherId)).stream()
                .map(CoreViewQueriesJpaParityIT::teacherAssignmentJpa).toList());
        assertTrue(docentes.consultarDocentePorId(
                new ConsultarDocentePorIdRepositoryDTO(UUID.randomUUID())).isEmpty());
    }

    @Test
    void estudiantes_paginados_desde_vistas_sql_server_conservan_conteo_y_orden_en_tres_paginas() {
        // SQL Server views are the read source, not in-memory pagination.
        // Both SQL/JPA use the same deterministic ORDER BY including e.id.
        final Long total = jdbc.queryForObject("""
                SELECT COUNT_BIG(e.id)
                FROM dbo.uv_estudiante_identidad e
                INNER JOIN dbo.uv_usuario u ON e.idUsuario = u.id
                """, Long.class);
        assertNotNull(total);
        // A three-page integration test must not pass vacuously on an empty or
        // tiny database. Populate an isolated SQL Server fixture (at least 11
        // distinct students) before running -Pintegration.
        assertTrue(total >= 11L,
                "Paginacion SQL real requiere fixture aislada con >=11 estudiantes distintos; encontrados: " + total);
        final int pageSize = 5;
        final List<UUID> observedIds = new java.util.ArrayList<>();

        for (int pageNumber : List.of(0, 1, 2)) {
            final List<List<Object>> expected = jdbc.query("""
                    SELECT e.id, u.id AS idUsuario, u.idTipoIdentificacion, u.numeroIdentificacion,
                           u.primerApellido, u.segundoApellido, u.primerNombre, u.segundoNombre,
                           u.nombreCompleto, u.correo, u.estaActivoUsuario
                    FROM dbo.uv_estudiante_identidad e
                    INNER JOIN dbo.uv_usuario u ON e.idUsuario = u.id
                    ORDER BY u.primerApellido, u.primerNombre, u.numeroIdentificacion, e.id
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """, (rs, row) -> studentJdbc(rs), pageNumber * pageSize, pageSize);
            final EstudiantePaginaRepositoryProjection actual =
                    estudiantes.consultarEstudiantes(new ConsultarEstudiantesRepositoryDTO(
                            null, null, null, null, null, null, null, null, null,
                            pageNumber, pageSize));

            assertEquals(total.longValue(), actual.totalItems());
            assertEquals((int) Math.ceil((double) total / pageSize), actual.totalPages());
            assertEquals(pageNumber, actual.page());
            assertEquals(pageSize, actual.size());
            final int expectedCount = (int) Math.min((long) pageSize, total - (long) pageNumber * pageSize);
            assertEquals(expectedCount, expected.size(), "Pagina SQL inesperada: " + pageNumber);
            assertEquals(expectedCount, actual.items().size(), "Pagina JPA incompleta: " + pageNumber);
            assertEquals(expected, actual.items().stream()
                    .map(CoreViewQueriesJpaParityIT::studentJpa).toList());
            actual.items().forEach(item -> observedIds.add(item.id()));
        }
        assertEquals(Math.min(total.longValue(), 3L * pageSize), observedIds.size());
        assertEquals(observedIds.size(), observedIds.stream().distinct().count(),
                "Una identidad de estudiante aparece en paginas diferentes.");
    }

    @Test
    void estudiantes_conservan_paginacion_detalle_contextos_y_not_found() {
        final List<List<Object>> before = jdbc.query("""
                SELECT e.id, u.id AS idUsuario, u.idTipoIdentificacion, u.numeroIdentificacion,
                       u.primerApellido, u.segundoApellido, u.primerNombre, u.segundoNombre,
                       u.nombreCompleto, u.correo, u.estaActivoUsuario
                FROM dbo.uv_estudiante_identidad e
                INNER JOIN dbo.uv_usuario u ON e.idUsuario = u.id
                ORDER BY u.primerApellido, u.primerNombre, u.numeroIdentificacion, e.id
                """, (rs, row) -> studentJdbc(rs));
        // Recorre todas las paginas (tamano 100): el oraculo JDBC completo debe coincidir fila a fila y en orden con
        // la concatenacion de paginas, con cualquier volumen de datos (seed minimo o fixtures de 5000 estudiantes).
        final List<List<Object>> paged = new java.util.ArrayList<>();
        long totalItems = 0;
        for (int pagina = 0; pagina == 0 || paged.size() < totalItems; pagina++) {
            final var page = estudiantes.consultarEstudiantes(new ConsultarEstudiantesRepositoryDTO(
                    null, null, null, null, null, null, null, null, null, pagina, 100));
            totalItems = page.totalItems();
            if (page.items().isEmpty()) {
                break;
            }
            page.items().stream().map(CoreViewQueriesJpaParityIT::studentJpa).forEach(paged::add);
        }
        assertEquals(before.size(), totalItems);
        assertEquals(before, paged);
        assumeFalse(before.isEmpty(), "No hay estudiantes para comparar detalle.");

        final UUID studentId = (UUID) before.getFirst().getFirst();
        final List<Object> expectedStudent = before.getFirst();
        final var filtered = estudiantes.consultarEstudiantes(new ConsultarEstudiantesRepositoryDTO(
                (UUID) expectedStudent.get(2),
                (Integer) expectedStudent.get(3),
                (String) expectedStudent.get(8),
                (String) expectedStudent.get(9),
                null, null, null, null,
                (Boolean) expectedStudent.get(10),
                0,
                10));
        assertEquals(List.of(expectedStudent), filtered.items().stream()
                .map(CoreViewQueriesJpaParityIT::studentJpa).toList());

        final var detail = estudiantes.consultarEstudiantePorId(studentId).orElseThrow();
        assertEquals(before.getFirst(), studentJpa(detail.datosPersonales()));
        final List<List<Object>> beforeContexts = jdbc.query("""
                SELECT DISTINCT idInstitucion, nombreInstitucion, idFacultad, nombreFacultad,
                       idPrograma, nombrePrograma, idPlanEstudio, inpPlanEstudio,
                       idAsignatura, nombreAsignatura, idGrupo, nombreGrupo
                FROM dbo.uv_estudiante
                WHERE id = ?
                ORDER BY nombreInstitucion, nombreFacultad, nombrePrograma,
                         nombreAsignatura, nombreGrupo, idGrupo
                """, (rs, row) -> studentContextJdbc(rs), studentId);
        assertEquals(beforeContexts, detail.contextosAcademicos().stream()
                .map(CoreViewQueriesJpaParityIT::studentContextJpa).toList());
        assertTrue(estudiantes.consultarEstudiantePorId(UUID.randomUUID()).isEmpty());
    }

    @Test
    void tipos_identificacion_conservan_columnas_y_orden() {
        final List<List<Object>> before = jdbc.query("""
                SELECT id, tipoIdentificacion, nombre
                FROM dbo.uv_tipo_identificacion
                ORDER BY tipoIdentificacion
                """, (rs, row) -> row(uuid(rs, "id"), string(rs, "tipoIdentificacion"), string(rs, "nombre")));
        assertEquals(before, tiposIdentificacion.consultarTiposIdentificacion().stream()
                .map(value -> row(value.getId(), value.getTipoIdentificacion(), value.getNombre())).toList());
    }

    // DATETIME2(7) is a timezone-free SQL value; fetch LocalDateTime directly, not Timestamp->Instant.
    // This oracle intentionally tests the DB clock fields rather than reproducing the old +5h JPA bug.
    private static List<Object> sessionJdbc(final ResultSet rs) throws SQLException {
        return row(uuid(rs, "id"), uuid(rs, "idGrupo"), string(rs, "nombre"), integer(rs, "numero"),
                string(rs, "codigo"), integer(rs, "numeroSemana"), string(rs, "codigoGrupo"),
                string(rs, "nombreGrupo"), rs.getObject("fechaHoraInicio", java.time.LocalDateTime.class),
                rs.getObject("fechaHoraFin", java.time.LocalDateTime.class));
    }

    private static List<Object> sessionJpa(final SesionRepositoryProjection value) {
        return row(value.getSesion(), value.getGrupo(), value.getNombre(), value.getNumero(), value.getCodigo(),
                value.getNumeroSemana(), value.getCodigoGrupo(), value.getNombreGrupo(),
                value.getFechaHoraInicio(), value.getFechaHoraFin());
    }

    private static List<Object> groupJdbc(final ResultSet rs) throws SQLException {
        return row(uuid(rs, "id"), string(rs, "codigo"), string(rs, "nombre"), uuid(rs, "idAsignatura"),
                string(rs, "nombreAsignatura"), uuid(rs, "idDocente"), integer(rs, "capacidadMaximaPermitida"),
                integer(rs, "estudiantesActivos"), integer(rs, "cuposDisponibles"),
                bool(rs, "grupoEstaHablitado"), JdbcBaselineValueMapper.toLocalDate(rs.getObject("fechaInicioPeriodoAcademico")),
                JdbcBaselineValueMapper.toLocalDate(rs.getObject("fechaFinPeriodoAcademico")));
    }

    private static List<Object> groupJpa(final GrupoRepositoryProjection value) {
        return row(value.getId(), value.getCodigo(), value.getNombre(), value.getIdAsignatura(),
                value.getNombreAsignatura(), value.getIdDocente(), value.getCapacidadMaximaPermitida(),
                value.getEstudiantesActivos(), value.getCuposDisponibles(), value.isGrupoHabilitado(),
                value.getFechaInicioPeriodoAcademico(), value.getFechaFinPeriodoAcademico());
    }

    private static List<Object> studentGroupJdbc(final ResultSet rs) throws SQLException {
        return row(uuid(rs, "id"), uuid(rs, "idEstudiante"), string(rs, "documento"),
                string(rs, "nombreCompleto"), string(rs, "correo"), string(rs, "codigoEstado"),
                string(rs, "nombreEstado"));
    }

    private static List<Object> studentGroupJpa(final EstudianteGrupoRepositoryProjection value) {
        return row(value.id(), value.idEstudiante(), value.documento(), value.nombreCompleto(), value.correo(),
                value.codigoEstado(), value.nombreEstado());
    }

    private static List<Object> userJdbc(final ResultSet rs) throws SQLException {
        return row(uuid(rs, "id"), uuid(rs, "idTipoIdentificacion"), integer(rs, "numeroIdentificacion"),
                string(rs, "primerNombre"), string(rs, "primerApellido"), string(rs, "correo"));
    }

    private static List<Object> userJpa(final UsuarioIdentidadRepositoryProjection value) {
        return row(value.id(), value.tipoIdentificacionId(), value.numeroIdentificacion(), value.primerNombre(),
                value.primerApellido(), value.correo());
    }

    private static List<Object> teacherIdentityJdbc(final ResultSet rs) throws SQLException {
        return row(uuid(rs, "id"), uuid(rs, "idUsuario"), integer(rs, "numeroIdentificacion"),
                string(rs, "nombreCompleto"), bool(rs, "estaActivoUsuario"));
    }

    private static List<Object> teacherIdentityJpa(final DocenteIdentidadRepositoryProjection value) {
        return row(value.getId(), value.getIdUsuario(), value.getNumeroIdentificacion(), value.getNombreCompleto(),
                value.isEstaActivoUsuario());
    }

    private static List<Object> teacherAssignmentJdbc(final ResultSet rs) throws SQLException {
        return row(uuid(rs, "id"), uuid(rs, "idUsuario"), integer(rs, "numeroIdentificacion"),
                string(rs, "nombreCompleto"), bool(rs, "estaActivoUsuario"), uuid(rs, "idInstitucion"),
                string(rs, "nombreInstitucion"), uuid(rs, "idFacultad"), string(rs, "nombreFacultad"),
                uuid(rs, "idPrograma"), string(rs, "nombrePrograma"), uuid(rs, "idPlanEstudio"),
                string(rs, "inpPlanEstudio"), uuid(rs, "idAsignatura"), string(rs, "nombreAsignatura"),
                uuid(rs, "idGrupo"), string(rs, "nombreGrupo"), uuid(rs, "idPerfil"),
                string(rs, "codigoPerfil"), string(rs, "nombrePerfil"), integer(rs, "estaActivoDocente"),
                string(rs, "estaActivoTextoDocente"));
    }

    private static List<Object> teacherAssignmentJpa(final DocenteAsignacionAcademicaRepositoryProjection value) {
        return row(value.getId(), value.getIdUsuario(), value.getNumeroIdentificacion(), value.getNombreCompleto(),
                value.isEstaActivoUsuario(), value.getIdInstitucion(), value.getNombreInstitucion(),
                value.getIdFacultad(), value.getNombreFacultad(), value.getIdPrograma(), value.getNombrePrograma(),
                value.getIdPlanEstudio(), value.getInpPlanEstudio(), value.getIdAsignatura(), value.getNombreAsignatura(),
                value.getIdGrupo(), value.getNombreGrupo(), value.getIdPerfil(), value.getCodigoPerfil(),
                value.getNombrePerfil(), value.getEstaActivoDocente(), value.getEstaActivoTextoDocente());
    }

    private static List<Object> studentJdbc(final ResultSet rs) throws SQLException {
        return row(uuid(rs, "id"), uuid(rs, "idUsuario"), uuid(rs, "idTipoIdentificacion"),
                integer(rs, "numeroIdentificacion"), string(rs, "primerApellido"), string(rs, "segundoApellido"),
                string(rs, "primerNombre"), string(rs, "segundoNombre"), string(rs, "nombreCompleto"),
                string(rs, "correo"), bool(rs, "estaActivoUsuario"));
    }

    private static List<Object> studentJpa(final EstudianteResumenRepositoryProjection value) {
        return row(value.id(), value.idUsuario(), value.tipoIdentificacionId(), value.numeroIdentificacion(),
                value.primerApellido(), value.segundoApellido(), value.primerNombre(), value.segundoNombre(),
                value.nombreCompleto(), value.correo(), value.estaActivoUsuario());
    }

    private static List<Object> studentContextJdbc(final ResultSet rs) throws SQLException {
        return row(uuid(rs, "idInstitucion"), string(rs, "nombreInstitucion"), uuid(rs, "idFacultad"),
                string(rs, "nombreFacultad"), uuid(rs, "idPrograma"), string(rs, "nombrePrograma"),
                uuid(rs, "idPlanEstudio"), string(rs, "inpPlanEstudio"), uuid(rs, "idAsignatura"),
                string(rs, "nombreAsignatura"), uuid(rs, "idGrupo"), string(rs, "nombreGrupo"));
    }

    private static List<Object> studentContextJpa(final EstudianteContextoAcademicoRepositoryProjection value) {
        return row(value.idInstitucion(), value.nombreInstitucion(), value.idFacultad(), value.nombreFacultad(),
                value.idPrograma(), value.nombrePrograma(), value.idPlanEstudio(), value.inpPlanEstudio(),
                value.idAsignatura(), value.nombreAsignatura(), value.idGrupo(), value.nombreGrupo());
    }

    private static UUID uuid(final ResultSet rs, final String column) throws SQLException {
        return JdbcBaselineValueMapper.toUuid(rs.getObject(column));
    }

    private static Integer integer(final ResultSet rs, final String column) throws SQLException {
        return JdbcBaselineValueMapper.toInteger(rs.getObject(column));
    }

    private static String string(final ResultSet rs, final String column) throws SQLException {
        return JdbcBaselineValueMapper.toString(rs.getObject(column));
    }

    private static boolean bool(final ResultSet rs, final String column) throws SQLException {
        return JdbcBaselineValueMapper.toBoolean(rs.getObject(column));
    }

    private static List<Object> row(final Object... values) {
        return Arrays.asList(values);
    }
}




