package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalJdbcBaselineExecutor;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LB-002.1 (JPA-Q-001..007, JPA-Q-012): paridad REAL JDBC baseline vs JPA candidato para
 * {@code AsistenciaRepositoryPort.consultarAsistenciasPorGrupo} sobre SQL Server real
 * ({@code gestionasistenciadb}, freeze DB desplegado), MISMA DB, MISMO estado, MISMO grupo/sesion.
 *
 * <p>Read-only: el fixture se DESCUBRE con consultas SELECT sobre las mismas vistas piloto; no se
 * inserta ni se modifica nada. El orden se normaliza SOLO en este test para comparar (la query de
 * produccion no tiene ORDER BY contractual). La comparacion es campo a campo, no por
 * {@code toString}.</p>
 */
@Import(JdbcBaselineTestConfiguration.class)
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class AsistenciaQueryJpaParityIT {

    private static final Comparator<AsistenciaRepositoryProjection> STABLE_ORDER = Comparator
            .comparing(AsistenciaRepositoryProjection::getAsistencia)
            .thenComparing(AsistenciaRepositoryProjection::getEstudiante)
            .thenComparing(AsistenciaRepositoryProjection::getGrupo)
            .thenComparing(AsistenciaRepositoryProjection::getSesion, Comparator.nullsFirst(Comparator.naturalOrder()));

    @Autowired
    private AsistenciaRepositoryPort jpaRoutedPort;

    @Autowired
    private NamedParameterJdbcOperations namedJdbc;

    @Autowired
    private CanonicalJdbcBaselineExecutor procedureExecutor;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AsistenciaRepositoryPort jdbcBaseline() {
        return new AsistenciaJdbcBaselineOracle(namedJdbc, procedureExecutor);
    }

    @Test
    void el_puerto_resuelto_con_provider_jpa_no_es_el_adapter_jdbc() {
        assertFalse(jpaRoutedPort instanceof AsistenciaJdbcBaselineOracle);
        assertNotNull(entityManagerFactory);
    }

    @Test
    void escenario_A_grupo_y_sesion_conocidos_paridad_campo_a_campo() {
        final Fixture fixture = Fixture.discover(jdbcTemplate);

        final int mismatches = compare("A", fixture.grupo(), fixture.sesion());

        assertEquals(0, mismatches);
    }

    @Test
    void escenario_B_mismo_grupo_con_sesion_null_paridad_campo_a_campo() {
        final Fixture fixture = Fixture.discover(jdbcTemplate);

        final int mismatches = compare("B", fixture.grupo(), null);

        assertEquals(0, mismatches);
    }

    @Test
    void escenario_C_cero_filas_equivalente_en_ambos() {
        // El freeze DB no tiene un grupo valido sin asistencias (hallazgo del RED); escenario
        // deterministico equivalente: grupo valido + sesion inexistente, y grupo inexistente.
        final Fixture fixture = Fixture.discover(jdbcTemplate);
        final UUID sesionInexistente = UUID.fromString("00000000-0000-0000-0000-00000000c0de");
        final UUID grupoInexistente = UUID.fromString("00000000-0000-0000-0000-00000000c0df");

        final int mismatchesGrupoValido = compare("C1", fixture.grupo(), sesionInexistente);

        assertEquals(0, mismatchesGrupoValido);
        assertTrue(jpaRoutedPort.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(
                fixture.grupo(), sesionInexistente, titular(fixture.grupo()))).isEmpty());
        // CULM-001 A.2-2 (A2-C2): un grupo inexistente o fuera de ambito ya no es una lista vacia sino 403 (SEC_002).
        org.junit.jupiter.api.Assertions.assertThrows(
                co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException.class,
                () -> jpaRoutedPort.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(
                        grupoInexistente, null, titular(fixture.grupo()))));
    }

    @Test
    void escenario_D_multiples_filas_y_semantica_de_campos() {
        final Fixture fixture = Fixture.discover(jdbcTemplate);

        final int mismatches = compare("D", fixture.grupoMultifila(), null);
        final List<AsistenciaRepositoryProjection> rows = jpaRoutedPort.consultarAsistenciasPorGrupo(
                new ConsultarAsistenciasPorGrupoRepositoryDTO(fixture.grupoMultifila(), null, titular(fixture.grupoMultifila())));

        assertEquals(0, mismatches);
        assertTrue(rows.size() > 1, "El fixture multi-fila debe tener N>1 filas.");
        for (final AsistenciaRepositoryProjection row : rows) {
            assertNotNull(row.getAsistencia());
            assertNotNull(row.getEstudiante());
            assertEquals(fixture.grupoMultifila(), row.getGrupo());
            assertNotNull(row.getSesion());
            assertEquals("", row.getObservacion());
        }
    }

    @Test
    void filtro_de_sesion_solo_retorna_esa_sesion() {
        final Fixture fixture = Fixture.discover(jdbcTemplate);

        final List<AsistenciaRepositoryProjection> rows = jpaRoutedPort.consultarAsistenciasPorGrupo(
                new ConsultarAsistenciasPorGrupoRepositoryDTO(fixture.grupo(), fixture.sesion(), titular(fixture.grupo())));

        assertFalse(rows.isEmpty());
        rows.forEach(row -> assertEquals(fixture.sesion(), row.getSesion()));
    }

    @Test
    void query_unica_sin_n_mas_1_delta_de_prepared_statements_es_uno() {
        final Fixture fixture = Fixture.discover(jdbcTemplate);
        final ConsultarAsistenciasPorGrupoRepositoryDTO dto =
                new ConsultarAsistenciasPorGrupoRepositoryDTO(fixture.grupoMultifila(), null, titular(fixture.grupoMultifila()));
        final Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        try {
            jpaRoutedPort.consultarAsistenciasPorGrupo(dto); // warm-up excluido del conteo
            statistics.clear();

            final List<AsistenciaRepositoryProjection> rows = jpaRoutedPort.consultarAsistenciasPorGrupo(dto);

            assertTrue(rows.size() > 1);
            assertEquals(1L, statistics.getPrepareStatementCount(), "Una sola sentencia SQL de negocio (sin N+1).");
            assertEquals(1L, statistics.getQueryExecutionCount());
            assertEquals(0L, statistics.getEntityLoadCount(), "La query es una proyeccion; no debe cargar entidades.");
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    // CULM-001 A.2-2: la consulta por SP exige la correlacion de la peticion.
    @org.junit.jupiter.api.BeforeEach
    void correlacion() {
        co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.set(UUID.randomUUID());
    }

    private int compare(final String scenario, final UUID grupo, final UUID sesion) {
        final ConsultarAsistenciasPorGrupoRepositoryDTO dto = new ConsultarAsistenciasPorGrupoRepositoryDTO(grupo, sesion, titular(grupo));
        final List<AsistenciaRepositoryProjection> jdbc = jdbcBaseline().consultarAsistenciasPorGrupo(dto)
                .stream().sorted(STABLE_ORDER).toList();
        final List<AsistenciaRepositoryProjection> jpa = jpaRoutedPort.consultarAsistenciasPorGrupo(dto)
                .stream().sorted(STABLE_ORDER).toList();

        int mismatches = 0;
        assertEquals(jdbc.size(), jpa.size(), "ROW_COUNT debe coincidir en escenario " + scenario);
        for (int i = 0; i < jdbc.size(); i++) {
            final AsistenciaRepositoryProjection a = jdbc.get(i);
            final AsistenciaRepositoryProjection b = jpa.get(i);
            mismatches += a.getAsistencia().equals(b.getAsistencia()) ? 0 : 1;
            mismatches += a.getEstudiante().equals(b.getEstudiante()) ? 0 : 1;
            mismatches += a.getGrupo().equals(b.getGrupo()) ? 0 : 1;
            mismatches += java.util.Objects.equals(a.getSesion(), b.getSesion()) ? 0 : 1;
            mismatches += a.isPresente() == b.isPresente() ? 0 : 1;
            mismatches += java.util.Objects.equals(a.getEstado(), b.getEstado()) ? 0 : 1;
            mismatches += java.util.Objects.equals(a.getObservacion(), b.getObservacion()) ? 0 : 1;
        }
        final long presentes = jdbc.stream().filter(AsistenciaRepositoryProjection::isPresente).count();
        final Map<String, Long> estados = jdbc.stream().collect(java.util.stream.Collectors.groupingBy(
                r -> String.valueOf(r.getEstado()), java.util.stream.Collectors.counting()));
        System.out.printf("PARITY|scenario=%s|JDBC_ROW_COUNT=%d|JPA_ROW_COUNT=%d|FIELD_MISMATCH_COUNT=%d|presente_true=%d|estados=%s%n",
                scenario, jdbc.size(), jpa.size(), mismatches, presentes, estados);
        return mismatches;
    }

    /** Fixture descubierto en modo SELECT (read-only) sobre las vistas piloto. */
    private record Fixture(UUID grupo, UUID sesion, UUID grupoMultifila) {

        static Fixture discover(final JdbcTemplate jdbc) {
            final List<Map<String, Object>> pares = jdbc.queryForList("""
                    SELECT eg.idGrupo AS grupo, a.idSesion AS sesion, COUNT(*) AS filas
                    FROM dbo.uv_detalle_asistencia da
                    INNER JOIN dbo.uv_asistencia a ON a.id = da.idAsistencia
                    INNER JOIN dbo.uv_estudiante_grupo eg ON eg.id = a.idEstudianteGrupo
                    GROUP BY eg.idGrupo, a.idSesion
                    ORDER BY COUNT(*) DESC, eg.idGrupo, a.idSesion
                    """);
            assertFalse(pares.isEmpty(),
                    "FIXTURE_MISSING: el freeze DB desplegado no tiene filas de asistencia en las vistas piloto.");
            final Map<String, Object> mejor = pares.getFirst();
            final List<Map<String, Object>> grupos = jdbc.queryForList("""
                    SELECT eg.idGrupo AS grupo, COUNT(*) AS filas
                    FROM dbo.uv_detalle_asistencia da
                    INNER JOIN dbo.uv_asistencia a ON a.id = da.idAsistencia
                    INNER JOIN dbo.uv_estudiante_grupo eg ON eg.id = a.idEstudianteGrupo
                    GROUP BY eg.idGrupo
                    ORDER BY COUNT(*) DESC, eg.idGrupo
                    """);
            return new Fixture(
                    UUID.fromString(String.valueOf(mejor.get("grupo"))),
                    UUID.fromString(String.valueOf(mejor.get("sesion"))),
                    UUID.fromString(String.valueOf(grupos.getFirst().get("grupo"))));
        }
    }

    /** CULM-001 A.2-2: la consulta por grupo exige el ejecutor; el docente titular ve todas las filas del grupo. */
    private UUID titular(final UUID grupo) {
        final java.util.List<UUID> ids = jdbcTemplate.query(
                "SELECT d.usuario FROM dbo.Grupo g INNER JOIN dbo.Docente d ON d.id = g.docente WHERE g.id = ?",
                (rs, row) -> UUID.fromString(rs.getString(1)), grupo);
        return ids.isEmpty() ? UUID.fromString("00000000-0000-0000-0000-0000000000a1") : ids.getFirst();
    }
}
