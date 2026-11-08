package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AreaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.InstitucionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AreaJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.AreaJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.AsignaturaDocenteJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.AsignaturaJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.CoordinadorJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.DecanoJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.FacultadJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.HorarioDocenteJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.HorarioEstudianteJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.InstitucionJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.MateriaEstudianteJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.PeriodoAcademicoJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.PlanEstudioJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline.SesionMateriaEstudianteJdbcBaseline;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Paridad JPA-05 contra SQL Server real (LB-008 JPA-05). Para cada capacidad academica compara
 * BEFORE (oraculo JDBC copiado literalmente en {@code jpa.baseline}, sin reutilizar codigo productivo)
 * contra AFTER (queries JPA-only), campo por campo y en orden.
 *
 * <p>Los casos sin datos vivos se declaran como SKIP mediante {@code assumeTrue}, nunca como PASS vacio.
 * {@code EstudiantePrograma} no figura aqui: {@code BLOCKED_BY_VIEW_IDENTITY}.</p>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class AcademicQueryJpaParityIT {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private NamedParameterJdbcOperations named;

    @BeforeEach
    void setUp() {
        named = new NamedParameterJdbcTemplate(jdbc);
    }

    @Test
    void catalogos_simples_conservan_columnas_nulos_y_orden() {
        final List<AreaProjection> areasBefore = new AreaJdbcBaseline(named).consultarAreas();
        assertEquals(areasBefore, new AreaJpaRepository(entityManager).consultarAreas());

        final List<InstitucionProjection> institucionesBefore = new InstitucionJdbcBaseline(named).consultarInstituciones();
        assertEquals(institucionesBefore, new InstitucionJpaRepository(entityManager).consultarInstituciones());
    }

    @Test
    void periodos_conservan_fechas_anio_codigo_y_optional() {
        final List<PeriodoAcademicoProjection> before = new PeriodoAcademicoJdbcBaseline(named).consultarPeriodosAcademicos();
        final PeriodoAcademicoJpaRepository after = new PeriodoAcademicoJpaRepository(entityManager);
        assertEquals(before, after.consultarPeriodosAcademicos());

        assumeTrue(!before.isEmpty(), "No hay periodos vivos para comparar consulta por id.");
        final UUID id = before.getFirst().id();
        final Optional<PeriodoAcademicoProjection> byIdBefore = new PeriodoAcademicoJdbcBaseline(named)
                .consultarPeriodoAcademicoPorId(id);
        assertEquals(byIdBefore, after.consultarPeriodoAcademicoPorId(id));
        assertEquals(Optional.empty(), after.consultarPeriodoAcademicoPorId(UUID.randomUUID()));
    }

    @Test
    void facultades_y_decanos_conservan_nulos_de_decano_y_flags() {
        final List<FacultadProjection> facultadesBefore = new FacultadJdbcBaseline(named).consultarFacultades();
        final FacultadJpaRepository facultades = new FacultadJpaRepository(entityManager);
        assertEquals(facultadesBefore, facultades.consultarFacultades());

        assumeTrue(!facultadesBefore.isEmpty(), "No hay facultades vivas.");
        final UUID idFacultad = facultadesBefore.getFirst().id();
        assertEquals(new FacultadJdbcBaseline(named).consultarFacultadPorId(idFacultad),
                facultades.consultarFacultadPorId(idFacultad));
        assertEquals(Optional.empty(), facultades.consultarFacultadPorId(UUID.randomUUID()));

        final List<DecanoProjection> decanosBefore = new DecanoJdbcBaseline(named,
                new DecanoJpaRepository(entityManager, new JpaProcedureExecutor(entityManager))).consultarDecanos();
        assertEquals(decanosBefore, new DecanoJpaRepository(entityManager, new JpaProcedureExecutor(entityManager)).consultarDecanos());
    }

    @Test
    void coordinadores_y_planes_conservan_flags_int_y_texto_numerico() {
        final List<CoordinadorProjection> coordinadoresBefore = coordinadoresBefore();
        assumeTrue(!coordinadoresBefore.isEmpty(), "No hay coordinadores vivos.");
        final UUID idFacultad = facultadDeCoordinador();
        assertEquals(new CoordinadorJdbcBaseline(named, new CoordinadorJpaRepository(entityManager, new JpaProcedureExecutor(entityManager)))
                        .consultarCoordinadoresPorFacultad(idFacultad),
                new CoordinadorJpaRepository(entityManager, new JpaProcedureExecutor(entityManager)).consultarCoordinadoresPorFacultad(idFacultad));

        final UUID idPrograma = uuidDe("SELECT TOP 1 idPrograma FROM dbo.uv_plan_estudio ORDER BY id");
        assumeTrue(idPrograma != null, "No hay planes de estudio vivos.");
        final List<PlanEstudioProjection> planesBefore = new PlanEstudioJdbcBaseline(named,
                new PlanEstudioJpaRepository(entityManager, new JpaProcedureExecutor(entityManager)))
                .consultarPlanesPorPrograma(idPrograma);
        assertEquals(planesBefore, new PlanEstudioJpaRepository(entityManager, new JpaProcedureExecutor(entityManager)).consultarPlanesPorPrograma(idPrograma));
    }

    @Test
    void asignaturas_por_programa_y_plan_conservan_join_semestre_y_orden() {
        final UUID idPrograma = uuidDe("SELECT TOP 1 idPrograma FROM dbo.uv_semestre_plan_estudio ORDER BY id");
        assumeTrue(idPrograma != null, "No hay semestres de plan vivos.");
        final AsignaturaJpaRepository after = new AsignaturaJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        final AsignaturaJpaRepository commands = new AsignaturaJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        final List<AsignaturaProjection> porPrograma = new AsignaturaJdbcBaseline(named, commands)
                .consultarAsignaturasPorPrograma(idPrograma);
        assertEquals(porPrograma, after.consultarAsignaturasPorPrograma(idPrograma));

        final UUID idPlan = uuidDe("SELECT TOP 1 idPlanEstudio FROM dbo.uv_semestre_plan_estudio ORDER BY id");
        final List<AsignaturaProjection> porPlanBefore = new AsignaturaJdbcBaseline(named, commands)
                .consultarAsignaturasPorPlan(idPlan);
        assertEquals(porPlanBefore, after.consultarAsignaturasPorPlan(idPlan));
    }

    @Test
    void horarios_conservan_horas_locales_y_nulos() {
        final UUID idDocente = uuidDe("SELECT TOP 1 idDocente FROM dbo.uv_horario_docente ORDER BY id");
        assumeTrue(idDocente != null, "No hay horarios de docente vivos.");
        final List<HorarioDocenteProjection> docenteBefore = new HorarioDocenteJdbcBaseline(named)
                .consultarHorarioDocente(idDocente);
        assertEquals(docenteBefore, new HorarioDocenteJpaRepository(entityManager).consultarHorarioDocente(idDocente));

        final UUID idEstudiante = uuidDe("SELECT TOP 1 idEstudiante FROM dbo.uv_horario_estudiante ORDER BY id");
        assumeTrue(idEstudiante != null, "No hay horarios de estudiante vivos.");
        // CULM-001 A.2-11: el SP recibe el usuario del estudiante y solo cuenta la matricula inscrita (A).
        final List<UUID> inscritos = gruposInscritos(idEstudiante);
        final List<HorarioEstudianteProjection> estudianteBefore = new HorarioEstudianteJdbcBaseline(named)
                .consultarHorarioEstudiante(idEstudiante).stream().filter(h -> inscritos.contains(h.idGrupo())).toList();
        co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.set(UUID.randomUUID());
        assertEquals(estudianteBefore,
                new HorarioEstudianteJpaRepository(entityManager).consultarHorarioEstudiante(usuarioDe(idEstudiante)));
    }

    @Test
    void materias_y_asignaturas_de_docente_conservan_distinct_y_orden() {
        final UUID idEstudiante = uuidDe("SELECT TOP 1 idEstudiante FROM dbo.uv_estudiante_grupo ORDER BY id");
        assumeTrue(idEstudiante != null, "No hay estudiantes en grupo vivos.");
        // CULM-001 A.2-11: el SP recibe el usuario del estudiante y solo cuenta la matricula inscrita (A).
        final List<UUID> inscritos = gruposInscritos(idEstudiante);
        final List<MateriaEstudianteProjection> materiasBefore = new MateriaEstudianteJdbcBaseline(named)
                .consultarMateriasEstudiante(idEstudiante).stream().filter(m -> inscritos.contains(m.idGrupo())).toList();
        co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.set(UUID.randomUUID());
        assertEquals(materiasBefore, new MateriaEstudianteJpaRepository(entityManager)
                .consultarMateriasEstudiante(usuarioDe(idEstudiante)));

        final UUID idDocente = uuidDe("SELECT TOP 1 id FROM dbo.uv_docente ORDER BY id");
        assumeTrue(idDocente != null, "No hay asignaciones de docente vivas.");
        final List<AsignaturaDocenteProjection> docenteBefore = new AsignaturaDocenteJdbcBaseline(named)
                .consultarAsignaturasDocente(idDocente);
        assertEquals(docenteBefore, new AsignaturaDocenteJpaRepository(entityManager)
                .consultarAsignaturasDocente(idDocente));
    }

    @Test
    void sesiones_de_materia_conservan_utc_y_orden() {
        final UUID idEstudiante = uuidDe("SELECT TOP 1 idEstudiante FROM dbo.uv_estudiante_grupo ORDER BY id");
        final UUID idAsignatura = uuidDe("SELECT TOP 1 g.idAsignatura FROM dbo.uv_grupo g "
                + "INNER JOIN dbo.uv_estudiante_grupo eg ON eg.idGrupo = g.id ORDER BY g.id");
        assumeTrue(idEstudiante != null && idAsignatura != null, "No hay sesiones de materia vivas.");
        final List<SesionMateriaEstudianteProjection> before = new SesionMateriaEstudianteJdbcBaseline(named)
                .consultarSesionesMateria(idEstudiante, idAsignatura);
        final List<SesionMateriaEstudianteProjection> after = new SesionMateriaEstudianteJpaRepository(entityManager)
                .consultarSesionesMateria(idEstudiante, idAsignatura);
        assertEquals(before, after);
        assertFalse(before.isEmpty(), "Debe existir al menos una sesion viva.");
        assertTrue(before.getFirst().fechaHoraInicio() != null, "fechaHoraInicio debe conservar valor UTC.");
    }

    private List<CoordinadorProjection> coordinadoresBefore() {
        final UUID idFacultad = facultadDeCoordinador();
        if (idFacultad == null) {
            return List.of();
        }
        return new CoordinadorJdbcBaseline(named, new CoordinadorJpaRepository(entityManager, new JpaProcedureExecutor(entityManager)))
                .consultarCoordinadoresPorFacultad(idFacultad);
    }

    private UUID facultadDeCoordinador() {
        return uuidDe("SELECT TOP 1 idFacultad FROM dbo.uv_coordinador ORDER BY id");
    }

    private UUID uuidDe(final String sql) {
        final List<UUID> rows = jdbc.query(sql, (rs, row) -> rs.getObject(1, UUID.class));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private UUID usuarioDe(final UUID idEstudiante) {
        return uuidDe("SELECT usuario FROM dbo.Estudiante WHERE id = '" + idEstudiante + "'");
    }

    private List<UUID> gruposInscritos(final UUID idEstudiante) {
        return jdbc.query("""
                SELECT eg.grupo FROM dbo.EstudianteGrupo eg
                INNER JOIN dbo.uv_estado_estudiante_grupo es ON es.id = eg.estado
                WHERE eg.estudiante = ? AND es.codigo = 'A'
                """, (rs, row) -> UUID.fromString(rs.getString(1)), idEstudiante);
    }
}
