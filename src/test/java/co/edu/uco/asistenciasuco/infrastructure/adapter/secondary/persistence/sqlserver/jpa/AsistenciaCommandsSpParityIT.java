package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.TechnicalException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalJdbcBaselineExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.JdbcBaselineTestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * LB-008 JPA-01 COMMANDS — paridad REAL sobre SQL Server para los tres commands NO batch de Asistencia:
 * {@code usp_registrar_asistencia_estudiante_autonomo}, {@code usp_radicar_solicitud_revision_asistencia} y
 * {@code usp_resolver_solicitud_revision_asistencia}. El batch {@code usp_registrar_asistencias_sesion} se
 * certifica en {@link AsistenciaCommandJpaParityIT}.
 *
 * <p>Oraculo: {@link AsistenciaJdbcBaselineOracle} (JDBC baseline, sin cambios). Candidato:
 * {@link AsistenciaJpaRepository} con un {@code EntityManager} compartido creado a partir del
 * {@code EntityManagerFactory} real. Cada escenario ejecuta el oraculo sobre SESSION A y el candidato
 * sobre SESSION B (filas {@code Sesion} distintas) y exige DOBLE aserción: (A) {@code Outcome(JDBC) ==
 * Outcome(JPA)} y (B) un oraculo absoluto sobre la verdad de DB cuando el escenario lo define.</p>
 *
 * <p>Sin {@code @Transactional}: los tres SP gestionan su propia transacción. La DB final garantiza la
 * atomicidad de Asistencia + DetalleAsistencia también en el registro autónomo (TD-056 cerrada). El IT
 * no altera esa frontera.</p>
 */
@Import(JdbcBaselineTestConfiguration.class)
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class AsistenciaCommandsSpParityIT {

    /**
     * Estados del catalogo {@code dbo.Estado} de la DB final (TD-057 resuelto por el dueno de la DB): una
     * solicitud nueva queda {@code P} (pendiente) y se resuelve a {@code A} (aprobada) o {@code R} (rechazada).
     */
    private static final String ESTADO_PENDIENTE = "P";
    private static final String ESTADO_APROBADA = "A";

    /** Outcome normalizado: excluye IDs autogenerados y mensajes con correlación. */
    private record Outcome(
            String exceptionClass,
            String stableCode,
            int asistencias,
            int detalles,
            int solicitudes,
            String estadosSolicitud,
            Map<UUID, String> estadoPorEstudiante
    ) {
    }

    @Autowired
    private NamedParameterJdbcOperations namedJdbc;

    @Autowired
    private CanonicalJdbcBaselineExecutor procedureExecutor;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SesionRepositoryPort sesionRepositoryPort;

    private AttendanceCommandParityFixture fixture;
    private AsistenciaJdbcBaselineOracle jdbcOracle;
    private AsistenciaJpaRepository jpaCandidate;
    private final List<UUID> sesionesDelTest = new ArrayList<>();
    private int residualBefore;

    @BeforeEach
    void prepararFixture() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new AttendanceCommandParityFixture(jdbcTemplate, sesionRepositoryPort);
        residualBefore = fixture.residualRowsWithPrefix();
        fixture.prepare();
        jdbcOracle = new AsistenciaJdbcBaselineOracle(namedJdbc, procedureExecutor);
        jpaCandidate = new AsistenciaJpaRepository(
                entityManager, new JpaProcedureExecutor(entityManager));
        sesionesDelTest.add(fixture.newSessionA());
        sesionesDelTest.add(fixture.newSessionB());
    }

    @AfterEach
    void limpiarFixture() {
        try {
            for (final UUID sesion : sesionesDelTest) {
                jdbcTemplate.update("""
                        DELETE FROM dbo.SolicitudRevisionAsistencia
                        WHERE asistencia IN (SELECT id FROM dbo.Asistencia WHERE sesion = ?)""", sesion.toString());
            }
            fixture.cleanup();
            assertEquals(residualBefore, fixture.residualRowsWithPrefix(),
                    "El cleanup debe dejar 0 residuos con prefijo " + AttendanceCommandParityFixture.IT_PREFIX);
        } finally {
            sesionesDelTest.clear();
            CorrelationIdContext.clear();
        }
    }

    // ------------------------------------------------------------------ registrar asistencia autonoma

    @Test
    void AUT_01_codigo_correcto_registra_la_asistencia_igual_en_jdbc_y_jpa() {
        final UUID estudiante = fixture.estudiantes().get(0);
        final UUID sesionA = sesionA();
        final UUID sesionB = sesionB();

        final Outcome jdbc = autonoma(false, sesionA, estudiante, codigoDe(sesionA), usuarioDe(estudiante));
        final Outcome jpa = autonoma(true, sesionB, estudiante, codigoDe(sesionB), usuarioDe(estudiante));

        assertEquals(jdbc, jpa, "PARITY_MISMATCH en AUT_01");
        assertNull(jpa.exceptionClass(), "Con codigo correcto el SP debe completar sin excepcion.");
        assertEquals(1, jpa.asistencias(), "Oraculo absoluto: una cabecera de asistencia en la sesion.");
        assertNotNull(jpa.estadoPorEstudiante().get(estudiante), "Oraculo absoluto: el estudiante queda con detalle.");
    }

    @Test
    void AUT_02_codigo_incorrecto_es_rechazo_funcional_sin_escrituras_en_ambos() {
        final UUID estudiante = fixture.estudiantes().get(0);

        final Outcome jdbc = autonoma(false, sesionA(), estudiante, "CODIGO-ERRADO", usuarioDe(estudiante));
        final Outcome jpa = autonoma(true, sesionB(), estudiante, "CODIGO-ERRADO", usuarioDe(estudiante));

        assertEquals(jdbc, jpa, "PARITY_MISMATCH en AUT_02");
        assertNotNull(jpa.exceptionClass(), "Un codigo erroneo debe ser rechazado por la DB.");
        assertEquals(0, jpa.asistencias(), "Oraculo absoluto: ningun efecto tras el rechazo.");
    }

    @Test
    void AUT_03_estudiante_ajeno_al_grupo_es_rechazado_sin_escrituras() {
        final UUID ajeno = UUID.randomUUID();

        final Outcome jdbc = autonoma(false, sesionA(), ajeno, "X", ajeno);
        final Outcome jpa = autonoma(true, sesionB(), ajeno, "X", ajeno);

        assertEquals(jdbc, jpa, "PARITY_MISMATCH en AUT_03");
        assertEquals(0, jpa.asistencias());
    }

    @Test
    void AUT_04_usuario_ejecutor_nulo_se_clasifica_igual_que_el_baseline() {
        final UUID estudiante = fixture.estudiantes().get(0);

        final Outcome jdbc = autonoma(false, sesionA(), estudiante, codigoDe(sesionA()), null);
        final Outcome jpa = autonoma(true, sesionB(), estudiante, codigoDe(sesionB()), null);

        assertEquals(jdbc, jpa, "PARITY_MISMATCH en AUT_04");
    }

    // ------------------------------------------------------------------ radicar solicitud de revision

    @Test
    void RAD_01_estudiante_del_grupo_radica_solicitud_igual_en_jdbc_y_jpa() {
        final UUID estudiante = fixture.estudiantes().get(0);

        final Outcome jdbc = radicar(false, sesionA(), estudiante, usuarioDe(estudiante));
        final Outcome jpa = radicar(true, sesionB(), estudiante, usuarioDe(estudiante));

        assertEquals(jdbc, jpa, "PARITY_MISMATCH en RAD_01");
        assertNull(jpa.exceptionClass());
        assertEquals(1, jpa.solicitudes(), "Oraculo absoluto: una solicitud radicada en la sesion.");
        assertEquals(ESTADO_PENDIENTE, jpa.estadosSolicitud(),
                "Una solicitud nueva queda pendiente (P) segun el catalogo dbo.Estado de la DB final.");
    }

    @Test
    void RAD_02_estudiante_ajeno_no_radica_y_no_escribe_en_ambos() {
        final UUID ajeno = UUID.randomUUID();

        final Outcome jdbc = radicar(false, sesionA(), ajeno, ajeno);
        final Outcome jpa = radicar(true, sesionB(), ajeno, ajeno);

        assertEquals(jdbc, jpa, "PARITY_MISMATCH en RAD_02");
        assertEquals(0, jpa.solicitudes());
    }

    @Test
    void RAD_03_usuario_ejecutor_nulo_se_clasifica_igual_que_el_baseline() {
        final UUID estudiante = fixture.estudiantes().get(0);

        final Outcome jdbc = radicar(false, sesionA(), estudiante, null);
        final Outcome jpa = radicar(true, sesionB(), estudiante, null);

        assertEquals(jdbc, jpa, "PARITY_MISMATCH en RAD_03");
    }

    // ------------------------------------------------------------------ resolver solicitud de revision

    @Test
    void RES_01_docente_titular_aprueba_la_solicitud_igual_en_jdbc_y_jpa() {
        final UUID estudiante = fixture.estudiantes().get(0);
        final UUID solicitudA = solicitudRadicadaPorBaseline(sesionA(), estudiante);
        final UUID solicitudB = solicitudRadicadaPorBaseline(sesionB(), estudiante);

        final Outcome jdbc = resolver(false, sesionA(), solicitudA, "APROBADA");
        final Outcome jpa = resolver(true, sesionB(), solicitudB, "APROBADA");

        assertEquals(jdbc, jpa, "PARITY_MISMATCH en RES_01");
        assertNull(jpa.exceptionClass());
        assertEquals(ESTADO_APROBADA, jpa.estadosSolicitud(),
                "APROBADA mapea al estado A del catalogo dbo.Estado.");
    }

    @Test
    void RES_02_docente_no_titular_es_rechazado_sin_cambios_en_ambos() {
        final UUID estudiante = fixture.estudiantes().get(0);
        final UUID solicitudA = solicitudRadicadaPorBaseline(sesionA(), estudiante);
        final UUID solicitudB = solicitudRadicadaPorBaseline(sesionB(), estudiante);

        // El SP valida que @idDocente sea el titular del grupo de la solicitud: se envia el docente ajeno.
        final Outcome jdbc = resolverComoDocente(false, solicitudA, fixture.otroDocenteId(),
                fixture.otroDocenteUsuarioId(), "RECHAZADA");
        final Outcome jpa = resolverComoDocente(true, solicitudB, fixture.otroDocenteId(),
                fixture.otroDocenteUsuarioId(), "RECHAZADA");

        assertEquals(jdbc, jpa, "PARITY_MISMATCH en RES_02");
        assertNotNull(jpa.exceptionClass(), "Un docente no titular debe ser rechazado por la DB.");
        assertEquals(ESTADO_PENDIENTE, jpa.estadosSolicitud(),
                "Rechazo sin efectos sobre el estado: la solicitud sigue pendiente (P).");
    }

    @Test
    void RES_03_solicitud_inexistente_es_rechazada_igual_en_ambos() {
        final Outcome jdbc = resolver(false, sesionA(), UUID.randomUUID(), "APROBADA");
        final Outcome jpa = resolver(true, sesionB(), UUID.randomUUID(), "APROBADA");

        assertEquals(jdbc, jpa, "PARITY_MISMATCH en RES_03");
        assertNotNull(jpa.exceptionClass());
    }

    // ------------------------------------------------------------------ ejecucion y captura de outcome

    private Outcome autonoma(
            final boolean candidato,
            final UUID sesion,
            final UUID estudiante,
            final String codigo,
            final UUID ejecutor
    ) {
        final RegistrarAsistenciaAutonomaRepositoryDTO dto =
                new RegistrarAsistenciaAutonomaRepositoryDTO(estudiante, sesion, codigo, ejecutor);
        return ejecutar(sesion, () -> {
            if (candidato) {
                jpaCandidate.registrarAsistenciaAutonoma(dto);
            } else {
                jdbcOracle.registrarAsistenciaAutonoma(dto);
            }
        });
    }

    private Outcome radicar(
            final boolean candidato,
            final UUID sesion,
            final UUID estudiante,
            final UUID ejecutor
    ) {
        final SolicitarRevisionAsistenciaRepositoryDTO dto = new SolicitarRevisionAsistenciaRepositoryDTO(
                estudiante, sesion, "NOTA", "Justificacion de revision", null, ejecutor);
        return ejecutar(sesion, () -> {
            if (candidato) {
                jpaCandidate.solicitarRevisionAsistencia(dto);
            } else {
                jdbcOracle.solicitarRevisionAsistencia(dto);
            }
        });
    }

    private Outcome resolver(final boolean candidato, final UUID sesion, final UUID solicitud, final String accion) {
        return resolverCon(candidato, solicitud, fixture.docenteUsuarioId(), accion, sesion);
    }

    private Outcome resolverCon(final boolean candidato, final UUID solicitud, final UUID ejecutor, final String accion) {
        return resolverCon(candidato, solicitud, ejecutor, accion, sesionDeSolicitud(solicitud));
    }

    private Outcome resolverComoDocente(
            final boolean candidato,
            final UUID solicitud,
            final UUID docente,
            final UUID ejecutor,
            final String accion
    ) {
        return resolverCon(candidato, solicitud, docente, ejecutor, accion, sesionDeSolicitud(solicitud));
    }

    private Outcome resolverCon(
            final boolean candidato,
            final UUID solicitud,
            final UUID ejecutor,
            final String accion,
            final UUID sesion
    ) {
        return resolverCon(candidato, solicitud, fixture.docenteId(), ejecutor, accion, sesion);
    }

    private Outcome resolverCon(
            final boolean candidato,
            final UUID solicitud,
            final UUID docente,
            final UUID ejecutor,
            final String accion,
            final UUID sesion
    ) {
        final ResolverSolicitudRevisionAsistenciaRepositoryDTO dto = new ResolverSolicitudRevisionAsistenciaRepositoryDTO(
                solicitud, docente, accion, "Respuesta del docente", ejecutor);
        return ejecutar(sesion, () -> {
            if (candidato) {
                jpaCandidate.resolverSolicitudRevisionAsistencia(dto);
            } else {
                jdbcOracle.resolverSolicitudRevisionAsistencia(dto);
            }
        });
    }

    private Outcome ejecutar(final UUID sesion, final Runnable accion) {
        final UUID correlacionPrevia = CorrelationIdContext.get();
        CorrelationIdContext.set(UUID.randomUUID());
        String exceptionClass = null;
        String code = null;
        try {
            accion.run();
        } catch (final RuntimeException failure) {
            exceptionClass = failure.getClass().getSimpleName();
            if (failure instanceof ApplicationException application) {
                code = application.getCode();
            } else if (failure instanceof TechnicalException technical) {
                code = technical.getCode();
            }
        } finally {
            CorrelationIdContext.set(correlacionPrevia);
        }
        return outcomeDe(sesion, exceptionClass, code);
    }

    private Outcome outcomeDe(final UUID sesion, final String exceptionClass, final String code) {
        final Integer solicitudes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dbo.SolicitudRevisionAsistencia s
                INNER JOIN dbo.Asistencia a ON a.id = s.asistencia
                WHERE a.sesion = ?""", Integer.class, sesion.toString());
        final List<String> estados = jdbcTemplate.query("""
                SELECT e.codigo FROM dbo.SolicitudRevisionAsistencia s
                INNER JOIN dbo.Asistencia a ON a.id = s.asistencia
                INNER JOIN dbo.Estado e ON e.id = s.estado
                WHERE a.sesion = ?
                ORDER BY e.codigo""", (rs, n) -> rs.getString("codigo"), sesion.toString());
        return new Outcome(
                exceptionClass,
                code,
                fixture.countAsistencia(sesion),
                fixture.countDetalle(sesion),
                solicitudes,
                String.join(",", estados),
                fixture.persistedState(sesion));
    }

    // ------------------------------------------------------------------ datos de fixture (verdad de DB)

    private UUID sesionA() {
        return sesionesDelTest.get(0);
    }

    private UUID sesionB() {
        return sesionesDelTest.get(1);
    }

    private String codigoDe(final UUID sesion) {
        return jdbcTemplate.queryForObject("SELECT TRIM(codigo) FROM dbo.Sesion WHERE id = ?", String.class, sesion.toString());
    }

    /** Usuario asociado al estudiante (perfil ESTUDIANTE: fila en dbo.Estudiante). */
    private UUID usuarioDe(final UUID estudiante) {
        return UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT usuario FROM dbo.Estudiante WHERE id = ?", String.class, estudiante.toString()));
    }

    private UUID sesionDeSolicitud(final UUID solicitud) {
        return UUID.fromString(jdbcTemplate.queryForObject("""
                SELECT a.sesion FROM dbo.SolicitudRevisionAsistencia s
                INNER JOIN dbo.Asistencia a ON a.id = s.asistencia
                WHERE s.id = ?""", String.class, solicitud.toString()));
    }

    /** Radica con el baseline JDBC (solo preparacion de datos) y devuelve la solicitud creada en la sesion. */
    private UUID solicitudRadicadaPorBaseline(final UUID sesion, final UUID estudiante) {
        jdbcOracle.solicitarRevisionAsistencia(new SolicitarRevisionAsistenciaRepositoryDTO(
                estudiante, sesion, "NOTA", "Preparacion de datos", null,
                usuarioDe(estudiante)));
        return UUID.fromString(jdbcTemplate.queryForObject("""
                SELECT s.id FROM dbo.SolicitudRevisionAsistencia s
                INNER JOIN dbo.Asistencia a ON a.id = s.asistencia
                WHERE a.sesion = ?""", String.class, sesion.toString()));
    }
}




