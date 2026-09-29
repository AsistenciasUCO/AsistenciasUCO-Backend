package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DbExceptionTranslator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * LB-002.2B — JPA STORED PROCEDURE FEASIBILITY PROBES (U-01..U-05).
 *
 * <p><b>NO es AsistenciaJpaCommandPersistence ni un RED.</b> Es un probe de solo-test que responde si
 * Hibernate 7.2 / Jakarta Persistence / {@link StoredProcedureQuery} pueden consumir el SP congelado
 * {@code dbo.usp_registrar_asistencias_sesion} contra SQL Server REAL (sin H2, sin mocks del
 * {@code EntityManager}). No certifica el command JPA (eso es paridad en 2.2D); solo la viabilidad del
 * mecanismo. Usa {@code asistencia-query-provider=jpa} unicamente porque hoy es la forma aprobada de
 * construir el EMF; NO modifica perfiles productivos.</p>
 *
 * <p>El pool se limita a UNA conexion ({@code hikari.maximum-pool-size=1}): permite observar el estado
 * de la unica conexion tras cada llamada (autocommit, {@code @@TRANCOUNT}) y detectar fugas (una fuga
 * bloquearia la siguiente adquisicion). El estado confirmado se verifica ademas desde una conexion
 * INDEPENDIENTE ({@code DriverManager}) con {@code LOCK_TIMEOUT}. No se loguea el payload ni IDs; las
 * observaciones se emiten con el prefijo {@code [FEASIBILITY-OBS]}.</p>
 */
@Tag("integration")
@SpringBootTest(properties = {
        "app.adapters.persistence.asistencia-query-provider=jpa",
        "spring.datasource.hikari.maximum-pool-size=1",
        // Solo texto SQL (sin valores enlazados): evidencia de la sintaxis {call ...} que emite Hibernate.
        "logging.level.org.hibernate.SQL=DEBUG"
})
@MockitoBean(types = JwtDecoder.class)
class JpaAttendanceStoredProcedureFeasibilityIT {

    private static final String SP = "dbo.usp_registrar_asistencias_sesion";
    private static final String OPERATION = "registrarAsistenciasSesion";
    private static final Pattern DBCODE = Pattern.compile("^DBCODE=([A-Z0-9_]+)\\|");

    /** Binding usado por los probes que no son U-03. Fijado con la evidencia de U-03. */
    private static final Binding DEFAULT_BINDING = Binding.NAMED;

    private enum Binding { NAMED, NAMED_REORDERED, POSITIONAL }

    /** Observacion de una invocacion {@link StoredProcedureQuery}. */
    private record Outcome(
            List<String> trace,
            List<Object[]> canonicalRows,
            boolean txActiveBefore,
            boolean txActiveAfterExecute,
            boolean joinedToTransaction,
            int visibleFromIndependentConnectionWhileOpen,
            boolean entityManagerOpenAfterClose
    ) {
        Object[] single() {
            assertEquals(1, canonicalRows.size(), "Se esperaba exactamente 1 fila canonica. trace=" + trace);
            return canonicalRows.get(0);
        }
    }

    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private NamedParameterJdbcOperations namedJdbc;
    @Autowired
    private CanonicalStoredProcedureExecutor procedureExecutor;
    @Autowired
    private SesionRepositoryPort sesionRepositoryPort;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private Environment environment;

    private AttendanceCommandFixture fixture;
    private int residualBefore;

    @BeforeEach
    void prepararFixture() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new AttendanceCommandFixture(jdbcTemplate, sesionRepositoryPort);
        residualBefore = fixture.residualRowsWithPrefix();
        fixture.prepare();
    }

    @AfterEach
    void limpiarFixture() {
        try {
            fixture.cleanup();
            assertEquals(residualBefore, fixture.residualRowsWithPrefix(),
                    "El cleanup debe dejar 0 residuos con prefijo " + AttendanceCommandFixture.IT_PREFIX);
        } finally {
            CorrelationIdContext.clear();
        }
    }

    // ------------------------------------------------------------------ precondicion / metadata

    @Test
    void metadata_firma_real_y_longitud_del_parametro_json() {
        final List<String> params = jdbcTemplate.query("""
                SELECT p.parameter_id AS id, p.name AS nombre, t.name AS tipo, p.max_length AS largo
                FROM sys.parameters p INNER JOIN sys.types t ON t.user_type_id = p.user_type_id
                WHERE p.object_id = OBJECT_ID(?) ORDER BY p.parameter_id""",
                (rs, n) -> rs.getInt("id") + ":" + rs.getString("nombre") + ":" + rs.getString("tipo")
                        + ":" + rs.getInt("largo"), SP);
        obs("sp_signature=" + params);
        assertEquals(4, params.size());
        assertTrue(params.get(0).startsWith("1:@idSesion:uniqueidentifier"));
        assertTrue(params.get(1).startsWith("2:@asistenciaJSON:nvarchar"));
        assertTrue(params.get(2).startsWith("3:@idCorrelacion:uniqueidentifier"));
        assertTrue(params.get(3).startsWith("4:@idUsuarioEjecutor:uniqueidentifier"));
    }

    // ------------------------------------------------------------------ U-01

    /**
     * U-01: sin transaccion JPA exterior el SP completa y su estado queda confirmado de inmediato.
     * No hay {@code @Transactional}, ni {@code getTransaction().begin()}, ni JpaTransactionManager.
     */
    @Test
    void u01_sp_completa_sin_transaccion_jpa_exterior_y_confirma_inmediatamente() {
        final UUID sesion = fixture.newSession();
        final UUID correlacion = UUID.randomUUID();

        final Outcome outcome = invoke(DEFAULT_BINDING, sesion, json("AN", "SJC", "EX"), correlacion,
                fixture.docenteUsuarioId(), () -> independentDetalleCount(sesion));

        assertFalse(outcome.txActiveBefore(), "No debe haber transaccion JPA antes de ejecutar.");
        assertFalse(outcome.txActiveAfterExecute(), "El probe no abre transaccion JPA.");
        assertFalse(outcome.joinedToTransaction(), "El EntityManager no debe estar unido a ninguna transaccion.");
        assertTrue(JdbcValueMapper.toBoolean(outcome.single()[3]), "El SP debe completar con exito. trace=" + outcome.trace());
        assertEquals(correlacion, JdbcValueMapper.toUuid(outcome.single()[0]), "Eco de correlacion.");
        assertEquals(3, outcome.visibleFromIndependentConnectionWhileOpen(),
                "Los 3 detalles deben ser visibles desde OTRA conexion antes de cerrar el EM (commit inmediato).");
        assertFalse(outcome.entityManagerOpenAfterClose(), "El EntityManager debe quedar cerrado.");
        assertEquals(3, fixture.countAsistencia(sesion));
        assertEquals(3, fixture.countDetalle(sesion));

        assertSingleConnectionClean("tras el exito");
    }

    /** U-01: un rechazo funcional conserva el rollback (0 filas) tambien sin transaccion JPA. */
    @Test
    void u01_rechazo_conserva_rollback_sin_transaccion_jpa_exterior() {
        final UUID sesion = fixture.newSession();
        final UUID correlacion = UUID.randomUUID();

        final Outcome outcome = invoke(DEFAULT_BINDING, sesion, json("AN", "ABC", "EX"), correlacion,
                fixture.docenteUsuarioId(), () -> independentDetalleCount(sesion));

        final Object[] row = outcome.single();
        assertFalse(JdbcValueMapper.toBoolean(row[3]), "Lote con estado invalido debe rechazarse.");
        assertEquals("RC_001", dbCode(row));
        assertEquals(0, outcome.visibleFromIndependentConnectionWhileOpen());
        assertEquals(0, fixture.countAsistencia(sesion));
        assertEquals(0, fixture.countDetalle(sesion));
        assertSingleConnectionClean("tras el rechazo");
    }

    /** U-01/leak: con pool=1, rechazos + exito consecutivos no dejan conexiones ni transacciones abiertas. */
    @Test
    void u01_sin_fuga_de_conexion_ni_transaccion_abierta_con_pool_de_una_conexion() {
        final UUID sesion = fixture.newSession();
        for (int i = 0; i < 25; i++) {
            final Outcome rejected = invoke(DEFAULT_BINDING, sesion, json("AN"), UUID.randomUUID(),
                    fixture.otroDocenteUsuarioId(), null);
            assertEquals("SEC_002", dbCode(rejected.single()));
            assertFalse(rejected.entityManagerOpenAfterClose());
        }
        final Outcome ok = invoke(DEFAULT_BINDING, sesion, json("AN", "SJC", "EX"), UUID.randomUUID(),
                fixture.docenteUsuarioId(), null);
        assertTrue(JdbcValueMapper.toBoolean(ok.single()[3]));
        assertEquals(3, fixture.countDetalle(sesion));
        assertSingleConnectionClean("tras 25 rechazos + 1 exito");
    }

    /**
     * U-01 (control NEGATIVO, solo para dar poder discriminante al probe): si SE abre una transaccion
     * JPA exterior, el COMMIT del SP no confirma nada (queda anidado) y otra conexion NO ve las filas
     * (bloqueo/timeout). Demuestra que la comprobacion de U-01 detectaria una transaccion exterior.
     * Esto NO es el camino candidato: nunca se usa {@code begin()} fuera de este control.
     */
    @Test
    void u01_control_negativo_con_transaccion_exterior_el_estado_no_es_visible_desde_otra_conexion() {
        final UUID sesion = fixture.newSession();
        final EntityManager em = entityManagerFactory.createEntityManager();
        int visible;
        try {
            em.getTransaction().begin();
            try {
                final StoredProcedureQuery query = em.createStoredProcedureQuery(SP);
                query.registerStoredProcedureParameter("idSesion", UUID.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("asistenciaJSON", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("idCorrelacion", UUID.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("idUsuarioEjecutor", UUID.class, ParameterMode.IN);
                query.setParameter("idSesion", sesion);
                query.setParameter("asistenciaJSON", json("AN", "SJC", "EX"));
                query.setParameter("idCorrelacion", UUID.randomUUID());
                query.setParameter("idUsuarioEjecutor", fixture.docenteUsuarioId());
                drain(query, new ArrayList<>(), new ArrayList<>());
                visible = independentDetalleCountOrMinusOne(sesion);
            } finally {
                em.getTransaction().rollback();
            }
        } finally {
            em.close();
        }
        obs("u01_control_negativo_visible_con_tx_exterior=" + visible + " (-1 = bloqueado/timeout)");
        assertNotEquals(3, visible, "Con transaccion exterior el estado NO debe estar confirmado para otra conexion.");
        assertEquals(0, fixture.countDetalle(sesion), "El rollback exterior deja 0 filas.");
    }

    // ------------------------------------------------------------------ U-02

    /** U-02 (SP real): se alcanza la fila canonica; la traza queda registrada. */
    @Test
    void u02_sp_real_alcanza_la_fila_canonica_y_registra_la_traza_execute_hasMoreResults_getUpdateCount() {
        final UUID sesion = fixture.newSession();
        final Outcome outcome = invoke(DEFAULT_BINDING, sesion, json("AN", "SJC", "EX"), UUID.randomUUID(),
                fixture.docenteUsuarioId(), null);

        final Object[] row = outcome.single();
        obs("u02_real_sp_trace=" + outcome.trace());
        obs("u07_column_types=" + Arrays.stream(row)
                .map(o -> o == null ? "null" : o.getClass().getSimpleName()).toList());
        assertEquals(4, row.length, "El result set canonico tiene 4 columnas.");
        assertTrue(JdbcValueMapper.toBoolean(row[3]));
    }

    /**
     * U-02 (control determinista, sin tocar DB): {@code sp_executesql} emite un update count ANTES del
     * result set canonico. El algoritmo debe llegar a la fila.
     */
    @Test
    void u02_control_update_count_previo_no_oculta_el_result_set_canonico() {
        final List<String> trace = new ArrayList<>();
        final List<Object[]> rows = new ArrayList<>();
        runControl("""
                SET NOCOUNT OFF;
                DECLARE @t TABLE (x INT);
                INSERT @t VALUES (1),(2);
                SELECT CAST(NEWID() AS UNIQUEIDENTIFIER) AS idCorrelacion, N'u' AS mensajeUsuarioResultado,
                       N'ok' AS mensajeTecnicoResultado, CAST(1 AS BIT) AS estadoResultado;""", trace, rows);

        obs("u02_control_trace=" + trace);
        assertTrue(trace.stream().anyMatch(t -> t.startsWith("UC(")), "El control debe emitir un update count previo: " + trace);
        assertEquals(1, rows.stream().filter(r -> r.length == 4).count(), "Debe alcanzarse la fila canonica: " + trace);
        assertTrue(trace.indexOf(trace.stream().filter(t -> t.startsWith("UC(")).findFirst().orElseThrow())
                < trace.indexOf(trace.stream().filter(t -> t.startsWith("RS(")).findFirst().orElseThrow()),
                "El update count debe preceder al result set.");
    }

    /**
     * U-02 (control, D5): sin result set canonico el algoritmo termina SIN excepcion y sin fila; esa
     * ausencia es distinguible de un fallo tecnico y debe mapearse a ERR_DB_CANONICAL_CONTRACT.
     */
    @Test
    void u02_control_ausencia_de_result_set_se_observa_como_cero_filas_no_como_excepcion() {
        final List<String> trace = new ArrayList<>();
        final List<Object[]> rows = new ArrayList<>();
        runControl("SET NOCOUNT ON; DECLARE @x INT = 1;", trace, rows);

        obs("u02_control_no_resultset_trace=" + trace);
        assertTrue(rows.isEmpty(), "No debe haber filas. trace=" + trace);
    }

    // ------------------------------------------------------------------ U-03

    @Test
    void u03_binding_por_nombre_contra_sql_server_real() {
        final UUID sesion = fixture.newSession();
        final Outcome outcome = invoke(Binding.NAMED, sesion, json("AN", "SJC", "EX"), UUID.randomUUID(),
                fixture.docenteUsuarioId(), null);

        assertTrue(JdbcValueMapper.toBoolean(outcome.single()[3]), "binding NAMED debe ejecutar el SP con exito.");
        assertEquals(3, fixture.countDetalle(sesion));
        obs("u03_named=OK");
    }

    /**
     * U-03 (HALLAZGO): los nombres registrados NO gobiernan el binding; lo hace el ORDEN DE REGISTRO.
     * Con los mismos 4 parametros registrados en orden distinto al de {@code sys.parameters}, el SP recibe
     * argumentos cruzados (3 de 4 son UUID: no falla por tipo, solo el contrato lo rechaza). Consecuencia
     * de diseno: el candidato debe registrar SIEMPRE en el orden exacto de {@code sys.parameters}
     * (guardado por CMD-PAR-013); el "binding por nombre" no aporta proteccion frente a reordenamientos.
     * Si una version futura de Hibernate/mssql-jdbc honrara los nombres, este test lo revelara.
     */
    @Test
    void u03_hallazgo_los_nombres_registrados_no_gobiernan_el_binding_solo_el_orden_de_registro() {
        final UUID sesion = fixture.newSession();
        final Outcome outcome = invoke(Binding.NAMED_REORDERED, sesion, json("AN", "SJC", "EX"), UUID.randomUUID(),
                fixture.docenteUsuarioId(), null);

        final Object[] row = outcome.single();
        obs("u03_named_reordered success=" + JdbcValueMapper.toBoolean(row[3]) + " dbcode=" + dbCode(row));
        assertFalse(JdbcValueMapper.toBoolean(row[3]),
                "Registro reordenado => argumentos cruzados => el SP NO debe completar con exito.");
        assertEquals(0, fixture.countDetalle(sesion));
    }

    @Test
    void u03_binding_posicional_segun_sys_parameters() {
        final UUID sesion = fixture.newSession();
        final Outcome outcome = invoke(Binding.POSITIONAL, sesion, json("AN", "SJC", "EX"), UUID.randomUUID(),
                fixture.docenteUsuarioId(), null);

        assertTrue(JdbcValueMapper.toBoolean(outcome.single()[3]), "binding POSITIONAL debe ejecutar el SP con exito.");
        assertEquals(3, fixture.countDetalle(sesion));
        obs("u03_positional=OK");
    }

    // ------------------------------------------------------------------ U-04

    /** U-04: {@code idUsuarioEjecutor=null} produce la MISMA semantica contractual que el baseline JDBC. */
    @Test
    void u04_null_en_idUsuarioEjecutor_tiene_la_misma_semantica_que_el_baseline_jdbc() {
        final UUID sesionJdbc = fixture.newSession();
        final UUID sesionJpa = fixture.newSession();

        final Throwable jdbcFailure = baselineFailure(sesionJdbc, List.of("AN", "SJC", "EX"), null);
        final UUID correlacion = UUID.randomUUID();
        final Outcome outcome = invoke(DEFAULT_BINDING, sesionJpa, json("AN", "SJC", "EX"), correlacion, null, null);
        final Throwable jpaFailure = translate(outcome.single(), correlacion);

        obs("u04_jdbc=" + describe(jdbcFailure) + " u04_jpa=" + describe(jpaFailure) + " dbcode=" + dbCode(outcome.single()));
        assertNotNull(jdbcFailure, "El baseline debe rechazar ejecutor null.");
        assertSameFailure(jdbcFailure, jpaFailure);
        assertEquals("GEN_002", dbCode(outcome.single()));
        assertEquals(0, fixture.countDetalle(sesionJpa));
        assertEquals(0, fixture.countDetalle(sesionJdbc));
    }

    /** U-04 (complementario): {@code idSesion=null}; el baseline es el oraculo. Informativo de paridad. */
    @Test
    void u04_null_en_idSesion_paridad_con_el_baseline_jdbc() {
        final Throwable jdbcFailure = baselineFailure(null, List.of("AN"), fixture.docenteUsuarioId());
        final UUID correlacion = UUID.randomUUID();
        final Throwable jpaFailure;
        Object[] row = null;
        try {
            final Outcome outcome = invoke(DEFAULT_BINDING, null, json("AN"), correlacion, fixture.docenteUsuarioId(), null);
            row = outcome.single();
            jpaFailure = translate(row, correlacion);
        } catch (RuntimeException raw) {
            obs("u04b_jpa_raw_exception=" + raw.getClass().getSimpleName());
            throw raw;
        }
        obs("u04b_jdbc=" + describe(jdbcFailure) + " u04b_jpa=" + describe(jpaFailure) + " dbcode=" + dbCode(row));
        assertNotNull(jdbcFailure);
        assertSameFailure(jdbcFailure, jpaFailure);
    }

    // ------------------------------------------------------------------ U-05

    /**
     * U-05: JSON &gt; 4000 caracteres llega completo. 1 estudiante valido + &gt;100 UUID no matriculados:
     * rechazo funcional (EST_004 en el baseline), NUNCA ATT_001 (JSON roto por truncamiento). No se loguea
     * el payload.
     */
    @Test
    void u05_json_mayor_a_4000_caracteres_no_se_trunca() {
        final UUID sesionJdbc = fixture.newSession();
        final UUID sesionJpa = fixture.newSession();
        final List<String> estados = new ArrayList<>();
        final List<UUID> estudiantes = new ArrayList<>();
        estudiantes.add(fixture.estudiantes().get(0));
        estados.add("AN");
        for (int i = 0; i < 120; i++) {
            estudiantes.add(UUID.randomUUID());
            estados.add("AN");
        }
        final String payload = json(estudiantes, estados);
        assertTrue(payload.length() > 4000, "El payload debe superar 4000 caracteres: " + payload.length());

        final Throwable jdbcFailure = baselineFailure(sesionJdbc, estudiantes, estados, fixture.docenteUsuarioId());
        final UUID correlacion = UUID.randomUUID();
        final Outcome outcome = invoke(DEFAULT_BINDING, sesionJpa, payload, correlacion, fixture.docenteUsuarioId(), null);
        final Throwable jpaFailure = translate(outcome.single(), correlacion);

        obs("u05_json_length=" + payload.length() + " jdbc=" + describe(jdbcFailure) + " jpa=" + describe(jpaFailure)
                + " dbcode=" + dbCode(outcome.single()));
        assertNotEquals("ATT_001", dbCode(outcome.single()), "ATT_001 evidenciaria truncamiento/JSON roto.");
        assertNotNull(jdbcFailure);
        assertSameFailure(jdbcFailure, jpaFailure);
        assertEquals(0, fixture.countDetalle(sesionJpa));
    }

    // ------------------------------------------------------------------ mecanismo JPA (solo probe)

    private Outcome invoke(
            final Binding binding,
            final UUID sesion,
            final String json,
            final UUID correlacion,
            final UUID ejecutor,
            final IntSupplier whileOpen
    ) {
        final List<String> trace = new ArrayList<>();
        final List<Object[]> canonical = new ArrayList<>();
        final EntityManager em = entityManagerFactory.createEntityManager();
        final boolean txBefore;
        final boolean txAfter;
        final boolean joined;
        int visible = -1;
        try {
            final StoredProcedureQuery query = em.createStoredProcedureQuery(SP);
            if (binding == Binding.NAMED_REORDERED) {
                // Registro en orden DISTINTO al de sys.parameters: si Hibernate/mssql-jdbc asignaran por
                // orden de registro y no por nombre, el SP recibiria argumentos cruzados.
                query.registerStoredProcedureParameter("idUsuarioEjecutor", UUID.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("asistenciaJSON", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("idSesion", UUID.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("idCorrelacion", UUID.class, ParameterMode.IN);
                query.setParameter("idCorrelacion", correlacion);
                query.setParameter("idSesion", sesion);
                query.setParameter("asistenciaJSON", json);
                query.setParameter("idUsuarioEjecutor", ejecutor);
            } else if (binding == Binding.NAMED) {
                query.registerStoredProcedureParameter("idSesion", UUID.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("asistenciaJSON", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("idCorrelacion", UUID.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("idUsuarioEjecutor", UUID.class, ParameterMode.IN);
                query.setParameter("idSesion", sesion);
                query.setParameter("asistenciaJSON", json);
                query.setParameter("idCorrelacion", correlacion);
                query.setParameter("idUsuarioEjecutor", ejecutor);
            } else {
                query.registerStoredProcedureParameter(1, UUID.class, ParameterMode.IN);
                query.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter(3, UUID.class, ParameterMode.IN);
                query.registerStoredProcedureParameter(4, UUID.class, ParameterMode.IN);
                query.setParameter(1, sesion);
                query.setParameter(2, json);
                query.setParameter(3, correlacion);
                query.setParameter(4, ejecutor);
            }
            txBefore = em.getTransaction().isActive();
            drain(query, trace, canonical);
            txAfter = em.getTransaction().isActive();
            joined = em.isJoinedToTransaction();
            if (whileOpen != null) {
                visible = whileOpen.getAsInt();
            }
        } finally {
            em.close();
        }
        return new Outcome(trace, canonical, txBefore, txAfter, joined, visible, em.isOpen());
    }

    /**
     * Algoritmo de consumo: {@code execute()} y, mientras haya resultados, alterna result set
     * ({@code getResultList}) / update count ({@code getUpdateCount}) / {@code hasMoreResults}. Solo filas
     * de 4 columnas se consideran canonicas.
     */
    private static void drain(final StoredProcedureQuery query, final List<String> trace, final List<Object[]> canonical) {
        boolean hasResultSet = query.execute();
        trace.add("execute=" + hasResultSet);
        for (int guard = 0; guard < 20; guard++) {
            if (hasResultSet) {
                final List<?> list = query.getResultList();
                int canonicalRows = 0;
                for (final Object item : list) {
                    if (item instanceof Object[] cols && cols.length == 4) {
                        canonical.add(cols);
                        canonicalRows++;
                    }
                }
                trace.add("RS(rows=" + list.size() + ",canonical=" + canonicalRows + ")");
            } else {
                final int updateCount = query.getUpdateCount();
                if (updateCount == -1) {
                    trace.add("UC(-1)=fin");
                    return;
                }
                trace.add("UC(" + updateCount + ")");
            }
            hasResultSet = query.hasMoreResults();
            trace.add("hasMoreResults=" + hasResultSet);
        }
        trace.add("guard_agotado");
    }

    private void runControl(final String tsql, final List<String> trace, final List<Object[]> rows) {
        final EntityManager em = entityManagerFactory.createEntityManager();
        try {
            final StoredProcedureQuery query = em.createStoredProcedureQuery("sp_executesql");
            query.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
            query.setParameter(1, tsql);
            drain(query, trace, rows);
        } finally {
            em.close();
        }
    }

    // ------------------------------------------------------------------ baseline JDBC y traduccion

    private AsistenciaRepositoryPort jdbcBaseline() {
        return new AsistenciaRepositorySqlServerAdapter(namedJdbc, procedureExecutor);
    }

    private Throwable baselineFailure(final UUID sesion, final List<String> estados, final UUID ejecutor) {
        final List<UUID> estudiantes = new ArrayList<>();
        for (int i = 0; i < estados.size(); i++) {
            estudiantes.add(fixture.estudiantes().get(i));
        }
        return baselineFailure(sesion, estudiantes, estados, ejecutor);
    }

    private Throwable baselineFailure(
            final UUID sesion, final List<UUID> estudiantes, final List<String> estados, final UUID ejecutor) {
        final List<RegistroAsistenciaSesionRepositoryDTO> registros = new ArrayList<>();
        for (int i = 0; i < estudiantes.size(); i++) {
            registros.add(new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(i), estados.get(i)));
        }
        CorrelationIdContext.set(UUID.randomUUID());
        try {
            jdbcBaseline().registrarAsistenciasSesion(new RegistrarAsistenciasSesionRepositoryDTO(sesion, registros, ejecutor));
            return null;
        } catch (RuntimeException failure) {
            return failure;
        }
    }

    /** Traduce una fila canonica cruda con el MISMO traductor que usa el baseline (DbExceptionTranslator). */
    private static Throwable translate(final Object[] row, final UUID correlacion) {
        assertEquals(correlacion, JdbcValueMapper.toUuid(row[0]), "El eco de correlacion debe coincidir.");
        try {
            DbExceptionTranslator.throwIfFailed(
                    JdbcValueMapper.toBoolean(row[3]),
                    JdbcValueMapper.toString(row[1]),
                    JdbcValueMapper.toString(row[2]),
                    correlacion.toString(),
                    OPERATION);
            return null;
        } catch (RuntimeException failure) {
            return failure;
        }
    }

    private static void assertSameFailure(final Throwable baseline, final Throwable candidate) {
        assertNotNull(candidate, "El candidato debe fallar igual que el baseline: " + describe(baseline));
        assertEquals(baseline.getClass(), candidate.getClass(), "Clase de excepcion distinta.");
        if (baseline instanceof ApplicationException b && candidate instanceof ApplicationException c) {
            assertEquals(b.getCode(), c.getCode(), "Codigo distinto.");
        }
    }

    private static String describe(final Throwable failure) {
        if (failure == null) {
            return "none";
        }
        final String code = failure instanceof ApplicationException a ? a.getCode() : "-";
        return failure.getClass().getSimpleName() + "(" + code + ")";
    }

    private static String dbCode(final Object[] row) {
        final Matcher matcher = DBCODE.matcher(String.valueOf(JdbcValueMapper.toString(row[2])));
        return matcher.find() ? matcher.group(1) : "";
    }

    // ------------------------------------------------------------------ observacion de conexiones

    /** Con pool=1: la unica conexion esta libre, en autocommit y sin transaccion abierta. */
    private void assertSingleConnectionClean(final String momento) {
        final Boolean autocommit = jdbcTemplate.execute((java.sql.Connection c) -> c.getAutoCommit());
        final Integer tranCount = jdbcTemplate.queryForObject("SELECT @@TRANCOUNT", Integer.class);
        assertEquals(Boolean.TRUE, autocommit, "autocommit debe ser true " + momento);
        assertEquals(0, tranCount, "@@TRANCOUNT debe ser 0 " + momento);
        if (dataSource instanceof HikariDataSource hikari && hikari.getHikariPoolMXBean() != null) {
            assertEquals(0, hikari.getHikariPoolMXBean().getActiveConnections(),
                    "Sin conexiones activas (sin fuga) " + momento);
        }
    }

    private int independentDetalleCountOrMinusOne(final UUID sesion) {
        try {
            return independentDetalleCountChecked(sesion);
        } catch (SQLException e) {
            return -1;
        }
    }

    private int independentDetalleCount(final UUID sesion) {
        try {
            return independentDetalleCountChecked(sesion);
        } catch (SQLException e) {
            return fail("Lectura desde conexion independiente fallo (posible transaccion abierta): " + e.getClass().getSimpleName());
        }
    }

    private int independentDetalleCountChecked(final UUID sesion) throws SQLException {
        final String url = environment.getRequiredProperty("spring.datasource.url");
        final String user = environment.getProperty("spring.datasource.username");
        final String password = environment.getProperty("spring.datasource.password");
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET LOCK_TIMEOUT 5000");
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(*) FROM dbo.DetalleAsistencia d
                    INNER JOIN dbo.Asistencia a ON a.id = d.asistencia WHERE a.sesion = ?""")) {
                statement.setString(1, sesion.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            }
        }
    }

    // ------------------------------------------------------------------ payloads

    private String json(final String... estados) {
        final List<UUID> estudiantes = new ArrayList<>();
        for (int i = 0; i < estados.length; i++) {
            estudiantes.add(fixture.estudiantes().get(i));
        }
        return json(estudiantes, List.of(estados));
    }

    private static String json(final List<UUID> estudiantes, final List<String> estados) {
        return java.util.stream.IntStream.range(0, estudiantes.size())
                .mapToObj(i -> "{\"idEstudiante\":\"" + estudiantes.get(i) + "\",\"estado\":\"" + estados.get(i) + "\"}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static void obs(final String message) {
        System.out.println("[FEASIBILITY-OBS] " + message);
    }
}
