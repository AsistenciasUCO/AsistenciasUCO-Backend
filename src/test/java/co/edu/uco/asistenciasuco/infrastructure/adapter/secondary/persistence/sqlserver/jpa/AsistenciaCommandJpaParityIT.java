package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.TechnicalException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * LB-002.2D (CMD-PAR-001..018): paridad REAL JDBC baseline vs JPA candidato para
 * {@code AsistenciaRepositoryPort.registrarAsistenciasSesion} sobre SQL Server real
 * ({@code gestionasistenciadb}, freeze DB desplegado).
 *
 * <p>Aisla la variable principal: {@code asistencia-query-provider=jdbc},
 * {@code asistencia-command-provider=jpa}. El baseline JDBC se construye manualmente
 * ({@code new AsistenciaRepositorySqlServerAdapter(...)}), nunca a traves del Composition Root. El
 * candidato JPA es el {@code AsistenciaRepositoryPort} REAL resuelto por el Composition Root bajo esa
 * propiedad (sin bypass manual). SESSION A (JDBC) y SESSION B (JPA) son SIEMPRE filas {@code Sesion}
 * distintas: JDBC y JPA nunca comparten sesion para comparar efectos (LB-002.2D &sect;9).</p>
 *
 * <p>Cada escenario hace DOBLE aserción: (A) {@code Outcome(JDBC) == Outcome(JPA)} vía
 * {@link #assertParity}, y (B) el oráculo contractual absoluto (verdad de DB), nunca solo "ambos
 * hicieron lo mismo".</p>
 */
@Tag("integration")
@SpringBootTest(properties = {
        "app.adapters.persistence.asistencia-query-provider=jdbc",
        "app.adapters.persistence.asistencia-command-provider=jpa"
})
@MockitoBean(types = JwtDecoder.class)
class AsistenciaCommandJpaParityIT {

    private static final String SP = "dbo.usp_registrar_asistencias_sesion";

    /** Outcome normalizado: excluye IDs autogenerados propios de cada fixture y timestamps no contractuales. */
    private record PersistedRow(UUID estudiante, String estado, boolean presente) {
    }

    private record Outcome(
            String exceptionClass,
            String stableApplicationCode,
            List<PersistedRow> persistedRows,
            int asistenciaHeaderCount,
            int detalleCount,
            int razonCausaTotalCount
    ) {
    }

    @Autowired
    private AsistenciaRepositoryPort jpaRoutedPort;

    @Autowired
    private NamedParameterJdbcOperations namedJdbc;

    @Autowired
    private CanonicalStoredProcedureExecutor procedureExecutor;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SesionRepositoryPort sesionRepositoryPort;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    private AttendanceCommandParityFixture fixture;
    private int residualBefore;

    @BeforeEach
    void prepararFixture() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new AttendanceCommandParityFixture(jdbcTemplate, sesionRepositoryPort);
        residualBefore = fixture.residualRowsWithPrefix();
        fixture.prepare();
    }

    @AfterEach
    void limpiarFixture() {
        try {
            fixture.cleanup();
            assertEquals(residualBefore, fixture.residualRowsWithPrefix(),
                    "El cleanup debe dejar 0 residuos con prefijo " + AttendanceCommandParityFixture.IT_PREFIX);
        } finally {
            CorrelationIdContext.clear();
        }
    }

    private AsistenciaRepositoryPort jdbcBaseline() {
        return new AsistenciaRepositorySqlServerAdapter(namedJdbc, procedureExecutor);
    }

    // ------------------------------------------------------------------ evidencia de composicion (2.2D#8)

    @Test
    void el_candidato_resuelto_con_command_provider_jpa_no_es_el_adapter_jdbc_puro() {
        assertFalse(jpaRoutedPort instanceof AsistenciaRepositorySqlServerAdapter,
                "El candidato JPA no debe ser el adapter JDBC puro.");
        assertNotNull(entityManagerFactory, "El EntityManagerFactory debe existir con command-provider=jpa.");
        assertEquals("jpa", environment.getProperty("app.adapters.persistence.asistencia-command-provider"));
        assertEquals("jdbc", environment.getProperty("app.adapters.persistence.asistencia-query-provider"));
    }

    // ------------------------------------------------------------------ CMD-PAR-001 / 002

    @Test
    void cmd_par_001_an_sjc_ex_paridad_completa_con_oraculo_absoluto() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();
        final List<String> estados = List.of("AN", "SJC", "EX");

        final Outcome jdbc = run(jdbcBaseline(), sesionJdbc, fixture.estudiantes(), estados, fixture.docenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, sesionJpa, fixture.estudiantes(), estados, fixture.docenteUsuarioId());

        assertParity("CMD-PAR-001", jdbc, jpa);

        assertNull(jpa.exceptionClass(), "Lote AN/SJC/EX valido no debe fallar.");
        assertEquals(3, jpa.asistenciaHeaderCount());
        assertEquals(3, jpa.detalleCount());
        final Map<UUID, PersistedRow> porEstudiante = jpa.persistedRows().stream()
                .collect(Collectors.toMap(PersistedRow::estudiante, r -> r));
        assertEquals("AN", porEstudiante.get(fixture.estudiantes().get(0)).estado());
        assertTrue(porEstudiante.get(fixture.estudiantes().get(0)).presente());
        assertEquals("SJC", porEstudiante.get(fixture.estudiantes().get(1)).estado());
        assertFalse(porEstudiante.get(fixture.estudiantes().get(1)).presente());
        assertEquals("EX", porEstudiante.get(fixture.estudiantes().get(2)).estado());
        assertFalse(porEstudiante.get(fixture.estudiantes().get(2)).presente());
    }

    @Test
    void cmd_par_002_roundtrip_ex_no_se_reconstruye_como_sjc_en_ninguna_lectura() {
        final UUID sesion = fixture.newSessionB();
        final UUID estudiante = fixture.estudiantes().get(0);

        final Outcome jpa = run(jpaRoutedPort, sesion, List.of(estudiante), List.of("EX"), fixture.docenteUsuarioId());
        assertNull(jpa.exceptionClass());

        // Verdad de DB (tabla base).
        assertEquals("EX", fixture.persistedState(sesion).get(estudiante).split("/", 2)[1]);
        assertTrue(fixture.persistedState(sesion).get(estudiante).startsWith("false/"));

        // Lectura via query JDBC.
        final List<AsistenciaRepositoryProjection> viaJdbcQuery = jdbcBaseline().consultarAsistenciasPorGrupo(
                new ConsultarAsistenciasPorGrupoRepositoryDTO(fixture.grupoId(), sesion));
        final AsistenciaRepositoryProjection filaJdbc = soloDe(viaJdbcQuery, estudiante);
        assertEquals("EX", filaJdbc.getEstado());
        assertFalse(filaJdbc.isPresente());
        assertNotEquals("SJC", filaJdbc.getEstado());

        // Lectura via query JPA (CMD-PAR-015: readback JDBC y JPA).
        final List<AsistenciaRepositoryProjection> viaJpaQuery = new AsistenciaJpaQueryPersistence(entityManagerFactory)
                .consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(fixture.grupoId(), sesion));
        final AsistenciaRepositoryProjection filaJpa = soloDe(viaJpaQuery, estudiante);
        assertEquals("EX", filaJpa.getEstado());
        assertFalse(filaJpa.isPresente());
        assertNotEquals("SJC", filaJpa.getEstado());
    }

    private static AsistenciaRepositoryProjection soloDe(final List<AsistenciaRepositoryProjection> filas, final UUID estudiante) {
        return filas.stream().filter(f -> f.getEstudiante().equals(estudiante)).findFirst()
                .orElseThrow(() -> new AssertionError("No se encontro fila para el estudiante " + estudiante));
    }

    // ------------------------------------------------------------------ CMD-PAR-003

    @Test
    void cmd_par_003_docente_ajeno_es_forbidden_sin_escrituras_en_ambos_providers() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();
        final List<UUID> estudiante = List.of(fixture.estudiantes().get(0));
        final List<String> estado = List.of("AN");

        final Outcome jdbc = run(jdbcBaseline(), sesionJdbc, estudiante, estado, fixture.otroDocenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, sesionJpa, estudiante, estado, fixture.otroDocenteUsuarioId());

        assertParity("CMD-PAR-003", jdbc, jpa);
        assertEquals("ForbiddenException", jpa.exceptionClass());
        assertEquals("FORBIDDEN", jpa.stableApplicationCode());
        assertEquals(0, jpa.detalleCount());
        assertEquals(0, jpa.asistenciaHeaderCount());
        assertTrue(jpa.persistedRows().isEmpty());
    }

    // ------------------------------------------------------------------ CMD-PAR-004

    @Test
    void cmd_par_004_estado_invalido_abc_es_rechazado_sin_crear_razonCausa_dinamica() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();
        final int razonCausaAbcAntes = fixture.countRazonCausa("ABC");
        final int razonCausaTotalAntes = fixture.countRazonCausaTotal();

        final Outcome jdbc = run(jdbcBaseline(), sesionJdbc, List.of(fixture.estudiantes().get(0)), List.of("ABC"), fixture.docenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, sesionJpa, List.of(fixture.estudiantes().get(0)), List.of("ABC"), fixture.docenteUsuarioId());

        assertParity("CMD-PAR-004", jdbc, jpa);
        assertEquals("ValidationException", jpa.exceptionClass());
        assertEquals("VALIDATION_ERROR", jpa.stableApplicationCode());
        assertEquals(0, jpa.detalleCount());
        assertEquals(razonCausaAbcAntes, fixture.countRazonCausa("ABC"), "No debe crearse ABC en dbo.RazonCausa.");
        assertEquals(razonCausaTotalAntes, fixture.countRazonCausaTotal(), "El catalogo RazonCausa no debe cambiar de cardinalidad.");
    }

    // ------------------------------------------------------------------ CMD-PAR-005

    @Test
    void cmd_par_005_rollback_total_lote_mixto_an_abc_ex_cero_filas() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();
        final List<UUID> tresEstudiantes = fixture.estudiantes();
        final List<String> estados = List.of("AN", "ABC", "EX");

        final Outcome jdbc = run(jdbcBaseline(), sesionJdbc, tresEstudiantes, estados, fixture.docenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, sesionJpa, tresEstudiantes, estados, fixture.docenteUsuarioId());

        assertParity("CMD-PAR-005", jdbc, jpa);
        assertEquals("ValidationException", jpa.exceptionClass());
        assertEquals(0, jpa.detalleCount(), "Ni AN ni EX deben sobrevivir en un lote mixto invalido.");
        assertEquals(0, jpa.asistenciaHeaderCount());
        assertTrue(jpa.persistedRows().isEmpty());
    }

    // ------------------------------------------------------------------ CMD-PAR-006 (rechazos contractuales)

    @Test
    void cmd_par_006_lista_vacia_es_rechazo_de_validacion() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();

        final Outcome jdbc = run(jdbcBaseline(), sesionJdbc, List.of(), List.of(), fixture.docenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, sesionJpa, List.of(), List.of(), fixture.docenteUsuarioId());

        assertParity("CMD-PAR-006-lista-vacia", jdbc, jpa);
        assertNotNull(jpa.exceptionClass(), "Una lista vacia debe ser rechazada, no un no-op silencioso.");
        assertEquals(0, jpa.detalleCount());
    }

    @Test
    void cmd_par_006_estudiante_duplicado_es_rechazo_de_validacion() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();
        final UUID duplicado = fixture.estudiantes().get(0);

        final Outcome jdbc = run(jdbcBaseline(), sesionJdbc, List.of(duplicado, duplicado), List.of("AN", "SJC"), fixture.docenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, sesionJpa, List.of(duplicado, duplicado), List.of("AN", "SJC"), fixture.docenteUsuarioId());

        assertParity("CMD-PAR-006-duplicado", jdbc, jpa);
        assertNotNull(jpa.exceptionClass(), "Un estudiante duplicado en el mismo lote debe ser rechazado.");
        assertEquals(0, jpa.detalleCount());
    }

    @Test
    void cmd_par_006_estudiante_fuera_del_grupo_es_forbidden() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();
        final UUID ajeno = UUID.randomUUID();

        final Outcome jdbc = run(jdbcBaseline(), sesionJdbc, List.of(ajeno), List.of("AN"), fixture.docenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, sesionJpa, List.of(ajeno), List.of("AN"), fixture.docenteUsuarioId());

        assertParity("CMD-PAR-006-fuera-del-grupo", jdbc, jpa);
        assertEquals("ForbiddenException", jpa.exceptionClass());
        assertEquals("FORBIDDEN", jpa.stableApplicationCode());
        assertEquals(0, jpa.detalleCount());
    }

    @Test
    void cmd_par_006_sesion_inexistente_es_not_found() {
        final UUID sesionInexistenteJdbc = UUID.randomUUID();
        final UUID sesionInexistenteJpa = UUID.randomUUID();

        final Outcome jdbc = run(jdbcBaseline(), sesionInexistenteJdbc, List.of(fixture.estudiantes().get(0)), List.of("AN"), fixture.docenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, sesionInexistenteJpa, List.of(fixture.estudiantes().get(0)), List.of("AN"), fixture.docenteUsuarioId());

        assertParity("CMD-PAR-006-sesion-inexistente", jdbc, jpa);
        assertEquals("ResourceNotFoundException", jpa.exceptionClass());
        assertEquals("RESOURCE_NOT_FOUND", jpa.stableApplicationCode());
        assertEquals(0, jpa.detalleCount());
    }

    @Test
    void cmd_par_006_matricula_inactiva_es_forbidden() {
        final UUID inactivoJdbc = fixture.crearEstudianteConEstadoInactivo();
        final UUID inactivoJpa = fixture.crearEstudianteConEstadoInactivo();
        if (inactivoJdbc == null || inactivoJpa == null) {
            fail("CMD-PAR-006 matricula inactiva: NOT_OBSERVABLE — el catalogo EstadoEstudianteGrupo "
                    + "solo tiene el codigo 'A' en esta DB desplegada; no es posible construir una matricula "
                    + "inactiva determinista sin violar el contrato congelado.");
            return;
        }
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();

        final Outcome jdbc = run(jdbcBaseline(), sesionJdbc, List.of(inactivoJdbc), List.of("AN"), fixture.docenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, sesionJpa, List.of(inactivoJpa), List.of("AN"), fixture.docenteUsuarioId());

        assertParity("CMD-PAR-006-matricula-inactiva", jdbc, jpa);
        assertEquals("ForbiddenException", jpa.exceptionClass());
        assertEquals("FORBIDDEN", jpa.stableApplicationCode());
        assertEquals(0, jpa.detalleCount());
    }

    // ------------------------------------------------------------------ CMD-PAR-007

    @Test
    void cmd_par_007_usuario_id_vs_docente_id_misma_semantica_en_ambos_providers() {
        assertNotEquals(fixture.docenteId(), fixture.docenteUsuarioId(),
                "Docente.id y Usuario.id del fixture no deben coincidir; de lo contrario esta IT no distinguiria un bypass real.");

        final UUID sesionJdbcRechazo = fixture.newSessionA();
        final UUID sesionJpaRechazo = fixture.newSessionB();
        final Outcome jdbcRechazo = run(jdbcBaseline(), sesionJdbcRechazo, List.of(fixture.estudiantes().get(0)), List.of("AN"), fixture.docenteId());
        final Outcome jpaRechazo = run(jpaRoutedPort, sesionJpaRechazo, List.of(fixture.estudiantes().get(0)), List.of("AN"), fixture.docenteId());
        assertParity("CMD-PAR-007-docenteId", jdbcRechazo, jpaRechazo);
        assertNotNull(jpaRechazo.exceptionClass(), "Docente.id como ejecutor no debe persistir nada.");
        assertEquals(0, jpaRechazo.detalleCount());

        final UUID sesionJdbcExito = fixture.newSessionA();
        final UUID sesionJpaExito = fixture.newSessionB();
        final Outcome jdbcExito = run(jdbcBaseline(), sesionJdbcExito, List.of(fixture.estudiantes().get(0)), List.of("AN"), fixture.docenteUsuarioId());
        final Outcome jpaExito = run(jpaRoutedPort, sesionJpaExito, List.of(fixture.estudiantes().get(0)), List.of("AN"), fixture.docenteUsuarioId());
        assertParity("CMD-PAR-007-usuarioId", jdbcExito, jpaExito);
        assertNull(jpaExito.exceptionClass(), "Usuario.id del titular si debe registrar el lote.");
        assertEquals(1, jpaExito.detalleCount());
    }

    // ------------------------------------------------------------------ CMD-PAR-008

    @Test
    void cmd_par_008_usuarioEjecutor_null_preserva_la_clasificacion_del_baseline() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();

        final Outcome jdbc = run(jdbcBaseline(), sesionJdbc, fixture.estudiantes(), List.of("AN", "SJC", "EX"), null);
        final Outcome jpa = run(jpaRoutedPort, sesionJpa, fixture.estudiantes(), List.of("AN", "SJC", "EX"), null);

        assertParity("CMD-PAR-008-usuarioEjecutor-null", jdbc, jpa);
        assertNotNull(jdbc.exceptionClass(), "El baseline debe rechazar ejecutor null (oraculo).");
        assertTrue(esExcepcionControlada(jpa), "El candidato no debe filtrar un error no controlado (ni Application ni Technical).");
        assertEquals(0, jpa.detalleCount());
    }

    /**
     * idSesion=null: tanto JDBC como JPA fallan con la MISMA {@code DatabaseOperationException}
     * (technical, no de negocio) — un UUID null no tiene tipo SQL inferible sin conversion previa a
     * nivel de driver, y ambos caminos delegan esa conversion identicamente. El baseline JDBC es el
     * oraculo (LB-002.2D &sect;18): la clasificacion tecnica exacta, no una clase de negocio especifica,
     * es lo que debe preservarse igual en ambos providers.
     */
    @Test
    void cmd_par_008_idSesion_null_preserva_la_clasificacion_del_baseline() {
        final Outcome jdbc = run(jdbcBaseline(), null, List.of(fixture.estudiantes().get(0)), List.of("AN"), fixture.docenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, null, List.of(fixture.estudiantes().get(0)), List.of("AN"), fixture.docenteUsuarioId());

        assertEquals(jdbc.exceptionClass(), jpa.exceptionClass(), "PARITY_MISMATCH en CMD-PAR-008-idSesion-null (clase)");
        assertEquals(jdbc.stableApplicationCode(), jpa.stableApplicationCode(), "PARITY_MISMATCH en CMD-PAR-008-idSesion-null (codigo)");
        assertNotNull(jdbc.exceptionClass(), "El baseline debe rechazar sesion null (oraculo).");
        assertTrue(esExcepcionControlada(jpa), "El candidato no debe filtrar un error no controlado (ni Application ni Technical).");
    }

    /** Controlada = clasificada por el traductor comun (ApplicationException de negocio o TechnicalException de infraestructura); nunca una excepcion cruda del driver/Hibernate/JDBC sin envolver. */
    private static boolean esExcepcionControlada(final Outcome outcome) {
        return outcome.exceptionClass() != null && outcome.stableApplicationCode() != null;
    }

    // ------------------------------------------------------------------ CMD-PAR-009 (idempotencia)

    @Test
    void cmd_par_009_idempotencia_misma_sesion_mismo_lote_dos_veces() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();
        final List<String> estados = List.of("AN", "SJC", "EX");

        run(jdbcBaseline(), sesionJdbc, fixture.estudiantes(), estados, fixture.docenteUsuarioId());
        final Outcome jdbcSegunda = run(jdbcBaseline(), sesionJdbc, fixture.estudiantes(), estados, fixture.docenteUsuarioId());

        run(jpaRoutedPort, sesionJpa, fixture.estudiantes(), estados, fixture.docenteUsuarioId());
        final Outcome jpaSegunda = run(jpaRoutedPort, sesionJpa, fixture.estudiantes(), estados, fixture.docenteUsuarioId());

        assertParity("CMD-PAR-009", jdbcSegunda, jpaSegunda);
        assertNull(jpaSegunda.exceptionClass());
        assertEquals(3, jpaSegunda.asistenciaHeaderCount(), "La segunda llamada NO debe duplicar cabeceras.");
        assertEquals(3, jpaSegunda.detalleCount(), "La segunda llamada NO debe duplicar detalles.");
    }

    // ------------------------------------------------------------------ CMD-PAR-011 (JSON > 4000)

    @Test
    void cmd_par_011_json_mayor_a_4000_caracteres_no_se_trunca_y_falla_igual() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();
        final List<UUID> estudiantesJdbc = new ArrayList<>();
        final List<UUID> estudiantesJpa = new ArrayList<>();
        final List<String> estados = new ArrayList<>();
        estudiantesJdbc.add(fixture.estudiantes().get(0));
        estudiantesJpa.add(fixture.estudiantes().get(0));
        estados.add("AN");
        for (int i = 0; i < 120; i++) {
            estudiantesJdbc.add(UUID.randomUUID());
            estudiantesJpa.add(UUID.randomUUID());
            estados.add("AN");
        }
        final int jsonLength = estimarLongitudJson(estudiantesJdbc, estados);
        assertTrue(jsonLength > 4000, "El payload debe superar 4000 caracteres (longitud=" + jsonLength + ").");

        final Outcome jdbc = run(jdbcBaseline(), sesionJdbc, estudiantesJdbc, estados, fixture.docenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, sesionJpa, estudiantesJpa, estados, fixture.docenteUsuarioId());

        assertParity("CMD-PAR-011", jdbc, jpa);
        assertNotEquals("ATT_001", jpa.stableApplicationCode(), "ATT_001 evidenciaria JSON truncado.");
        assertNotNull(jdbc.exceptionClass(), "El baseline debe rechazar estudiantes no matriculados.");
        assertEquals(0, jpa.detalleCount());
    }

    private static int estimarLongitudJson(final List<UUID> estudiantes, final List<String> estados) {
        return estudiantes.size() * ("{\"idEstudiante\":\"" + UUID.randomUUID() + "\",\"estado\":\"AN\"},").length();
    }

    // ------------------------------------------------------------------ CMD-PAR-013 (firma/orden)

    @Test
    void cmd_par_013_orden_y_tipo_de_parametros_coincide_con_sys_parameters() {
        final List<String> params = jdbcTemplate.query("""
                SELECT p.parameter_id AS id, p.name AS nombre, t.name AS tipo
                FROM sys.parameters p INNER JOIN sys.types t ON t.user_type_id = p.user_type_id
                WHERE p.object_id = OBJECT_ID(?) ORDER BY p.parameter_id""",
                (rs, n) -> rs.getInt("id") + ":" + rs.getString("nombre") + ":" + rs.getString("tipo"), SP);

        assertEquals(4, params.size(), "El SP debe tener exactamente 4 parametros.");
        assertEquals("1:@idSesion:uniqueidentifier", params.get(0));
        assertEquals("2:@asistenciaJSON:nvarchar", params.get(1));
        assertEquals("3:@idCorrelacion:uniqueidentifier", params.get(2));
        assertEquals("4:@idUsuarioEjecutor:uniqueidentifier", params.get(3));
    }

    // ------------------------------------------------------------------ CMD-PAR-014 (fuga de conexiones)

    @Test
    void cmd_par_014_sin_fuga_de_conexiones_tras_50_rechazos_y_exitos_por_jpa() {
        final UUID sesion = fixture.newSessionB();
        for (int i = 0; i < 50; i++) {
            final Outcome rejected = run(jpaRoutedPort, sesion, List.of(fixture.estudiantes().get(0)), List.of("AN"), fixture.otroDocenteUsuarioId());
            assertEquals("ForbiddenException", rejected.exceptionClass());
        }
        assertConnectionPoolClean("tras 50 rechazos JPA");

        final Outcome ok = run(jpaRoutedPort, sesion, fixture.estudiantes(), List.of("AN", "SJC", "EX"), fixture.docenteUsuarioId());
        assertNull(ok.exceptionClass());
        assertEquals(3, ok.detalleCount());
        assertConnectionPoolClean("tras 1 exito adicional JPA");
    }

    private void assertConnectionPoolClean(final String momento) {
        if (dataSource instanceof HikariDataSource hikari && hikari.getHikariPoolMXBean() != null) {
            assertEquals(0, hikari.getHikariPoolMXBean().getActiveConnections(), "activeConnections debe ser 0 " + momento);
            assertEquals(0, hikari.getHikariPoolMXBean().getThreadsAwaitingConnection(), "threadsAwaitingConnection debe ser 0 " + momento);
        }
    }

    // ------------------------------------------------------------------ CMD-PAR-016 (correlacion / auditoria)

    /**
     * El eco de correlacion lo exige {@code CanonicalStoredProcedureExecutor}/{@code
     * CanonicalProcedureResultValidator} de forma IDENTICA para JDBC y JPA (ambos lanzan
     * {@code DatabaseOperationException(ERR_DB_CANONICAL_CONTRACT)} si el SP devolviera una
     * correlacion distinta a la enviada). Toda ejecucion exitosa de este arnes que NO produce ese
     * codigo ya es evidencia de que la correlacion se propago correctamente en ambos caminos.
     *
     * <p>AUDIT: NOT_OBSERVABLE — este arnes llama al {@code AsistenciaRepositoryPort} directamente
     * (sin pasar por HTTP/controller), y la escritura en {@code dbo.AuditoriaEvento} la realiza el
     * interceptor de auditoria a nivel de controller, no el repositorio. No existe una forma
     * contractual segura de observar auditoria desde este nivel sin modificar Application/HTTP
     * (fuera de alcance de LB-002.2D).</p>
     */
    @Test
    void cmd_par_016_correlacion_se_propaga_y_se_exige_igual_en_ambos_providers() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();

        final Outcome jdbc = run(jdbcBaseline(), sesionJdbc, fixture.estudiantes(), List.of("AN", "SJC", "EX"), fixture.docenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, sesionJpa, fixture.estudiantes(), List.of("AN", "SJC", "EX"), fixture.docenteUsuarioId());

        assertNotEquals("ERR_DB_CANONICAL_CONTRACT", jdbc.stableApplicationCode());
        assertNotEquals("ERR_DB_CANONICAL_CONTRACT", jpa.stableApplicationCode());
        assertParity("CMD-PAR-016", jdbc, jpa);
    }

    // ------------------------------------------------------------------ CMD-PAR-017 (lote parcial)

    @Test
    void cmd_par_017_lote_parcial_el_estudiante_omitido_no_tiene_fila() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();
        final List<UUID> dosDeTres = fixture.estudiantes().subList(0, 2);
        final UUID omitido = fixture.estudiantes().get(2);

        final Outcome jdbc = run(jdbcBaseline(), sesionJdbc, dosDeTres, List.of("AN", "SJC"), fixture.docenteUsuarioId());
        final Outcome jpa = run(jpaRoutedPort, sesionJpa, dosDeTres, List.of("AN", "SJC"), fixture.docenteUsuarioId());

        assertParity("CMD-PAR-017", jdbc, jpa);
        assertNull(jpa.exceptionClass());
        assertEquals(2, jpa.detalleCount(), "Solo deben persistir exactamente los 2 estudiantes enviados.");
        assertTrue(jpa.persistedRows().stream().noneMatch(r -> r.estudiante().equals(omitido)),
                "El estudiante omitido NO debe tener fila (ausencia != AN).");
    }

    // ------------------------------------------------------------------ CMD-PAR-018 (upsert)

    @Test
    void cmd_par_018_upsert_an_luego_ex_mismo_estudiante_una_sola_fila_final_ex() {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();
        final UUID estudiante = fixture.estudiantes().get(0);

        run(jdbcBaseline(), sesionJdbc, List.of(estudiante), List.of("AN"), fixture.docenteUsuarioId());
        final Outcome jdbcFinal = run(jdbcBaseline(), sesionJdbc, List.of(estudiante), List.of("EX"), fixture.docenteUsuarioId());

        run(jpaRoutedPort, sesionJpa, List.of(estudiante), List.of("AN"), fixture.docenteUsuarioId());
        final Outcome jpaFinal = run(jpaRoutedPort, sesionJpa, List.of(estudiante), List.of("EX"), fixture.docenteUsuarioId());

        assertParity("CMD-PAR-018", jdbcFinal, jpaFinal);
        assertNull(jpaFinal.exceptionClass());
        assertEquals(1, jpaFinal.detalleCount(), "Debe quedar una sola fila (upsert), sin duplicados.");
        assertEquals("EX", jpaFinal.persistedRows().get(0).estado());
        assertFalse(jpaFinal.persistedRows().get(0).presente());
    }

    // ------------------------------------------------------------------ mecanismo comun

    private Outcome run(
            final AsistenciaRepositoryPort port,
            final UUID sesion,
            final List<UUID> estudiantesLote,
            final List<String> estados,
            final UUID ejecutor
    ) {
        final UUID correlacionPrevia = CorrelationIdContext.get();
        CorrelationIdContext.set(UUID.randomUUID());
        String exceptionClass = null;
        String code = null;
        try {
            final List<RegistroAsistenciaSesionRepositoryDTO> registros = new ArrayList<>();
            for (int i = 0; i < estudiantesLote.size(); i++) {
                registros.add(new RegistroAsistenciaSesionRepositoryDTO(estudiantesLote.get(i), estados.get(i)));
            }
            port.registrarAsistenciasSesion(new RegistrarAsistenciasSesionRepositoryDTO(sesion, registros, ejecutor));
        } catch (final RuntimeException failure) {
            exceptionClass = failure.getClass().getSimpleName();
            if (failure instanceof ApplicationException application) {
                code = application.getCode();
            } else if (failure instanceof TechnicalException technical) {
                code = technical.getCode();
            } else {
                System.out.printf("[PARITY-OBS] raw_uncontrolled_exception class=%s message=%s%n",
                        failure.getClass().getName(), failure.getMessage());
            }
        } finally {
            CorrelationIdContext.set(correlacionPrevia);
        }
        return outcomeOf(sesion, exceptionClass, code);
    }

    private Outcome outcomeOf(final UUID sesion, final String exceptionClass, final String code) {
        if (sesion == null) {
            return new Outcome(exceptionClass, code, List.of(), 0, 0, fixture.countRazonCausaTotal());
        }
        final List<PersistedRow> rows = new ArrayList<>();
        fixture.persistedState(sesion).forEach((estudiante, raw) -> rows.add(parseRow(estudiante, raw)));
        rows.sort(Comparator.comparing(PersistedRow::estudiante));
        return new Outcome(exceptionClass, code, List.copyOf(rows),
                fixture.countAsistencia(sesion), fixture.countDetalle(sesion), fixture.countRazonCausaTotal());
    }

    private static PersistedRow parseRow(final UUID estudiante, final String raw) {
        final int slash = raw.indexOf('/');
        final boolean presente = Boolean.parseBoolean(raw.substring(0, slash));
        final String estado = raw.substring(slash + 1);
        return new PersistedRow(estudiante, estado, presente);
    }

    /** (A) Outcome(JDBC) == Outcome(JPA), campo a campo. Nunca usada como unica evidencia (ver (B) por escenario). */
    private static int assertParity(final String scenario, final Outcome jdbc, final Outcome jpa) {
        int mismatches = 0;
        mismatches += Objects.equals(jdbc.exceptionClass(), jpa.exceptionClass()) ? 0 : 1;
        mismatches += Objects.equals(jdbc.stableApplicationCode(), jpa.stableApplicationCode()) ? 0 : 1;
        mismatches += jdbc.persistedRows().equals(jpa.persistedRows()) ? 0 : 1;
        mismatches += jdbc.asistenciaHeaderCount() == jpa.asistenciaHeaderCount() ? 0 : 1;
        mismatches += jdbc.detalleCount() == jpa.detalleCount() ? 0 : 1;
        System.out.printf(
                "PARITY|scenario=%s|JDBC_EXC=%s|JDBC_CODE=%s|JDBC_ROWS=%d|JPA_EXC=%s|JPA_CODE=%s|JPA_ROWS=%d|MISMATCH=%d%n",
                scenario, jdbc.exceptionClass(), jdbc.stableApplicationCode(), jdbc.persistedRows().size(),
                jpa.exceptionClass(), jpa.stableApplicationCode(), jpa.persistedRows().size(), mismatches);
        assertEquals(0, mismatches, "PARITY_MISMATCH en escenario " + scenario);
        return mismatches;
    }
}
