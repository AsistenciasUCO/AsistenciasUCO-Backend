package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * LB-002.2D (CMD-PAR-010): frontera transaccional REAL del command JPA candidato bajo pool de UNA sola
 * conexion. Certifica, contra el {@link AsistenciaRepositoryPort} REAL resuelto por el Composition Root
 * (command-provider=jpa, sin bypass manual), que tras cada llamada — exito o rechazo — no queda ninguna
 * transaccion abierta ({@code @@TRANCOUNT=0}), la conexion vuelve a autocommit, y el estado exitoso es
 * visible de inmediato desde una conexion INDEPENDIENTE. Se ejecuta en su propio contexto Spring (pool=1)
 * para no interferir con los ITs de paridad ni de concurrencia.
 */
@Tag("integration")
@SpringBootTest(properties = {
        "app.adapters.persistence.asistencia-query-provider=jdbc",
        "app.adapters.persistence.asistencia-command-provider=jpa",
        "spring.datasource.hikari.maximum-pool-size=1"
})
@MockitoBean(types = JwtDecoder.class)
class AsistenciaCommandTransactionBoundaryIT {

    @Autowired
    private AsistenciaRepositoryPort jpaRoutedPort;

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

    @Test
    void cmd_par_010_exito_jpa_confirma_sin_transaccion_exterior_y_es_visible_desde_otra_conexion() {
        final UUID sesion = fixture.newSessionB();

        invocar(sesion, fixture.estudiantes(), List.of("AN", "SJC", "EX"), fixture.docenteUsuarioId());

        assertConnectionCleanAfterCall("tras exito JPA");
        assertEquals(3, fixture.countDetalle(sesion));
        assertEquals(3, independentDetalleCount(sesion), "El estado exitoso debe ser visible desde OTRA conexion.");
    }

    @Test
    void cmd_par_010_rechazo_jpa_no_deja_transaccion_abierta_y_deja_cero_filas() {
        final UUID sesion = fixture.newSessionB();

        final RuntimeException failure = invocarEsperandoFallo(sesion, List.of(fixture.estudiantes().get(0)), List.of("AN"),
                fixture.otroDocenteUsuarioId());

        assertConnectionCleanAfterCall("tras rechazo JPA");
        assertEquals(0, fixture.countDetalle(sesion));
        assertEquals(0, independentDetalleCount(sesion));
        assertFalse(failure == null);
    }

    @Test
    void cmd_par_010_rechazos_y_exitos_alternados_con_pool_de_una_conexion_no_dejan_estado_residual() {
        final UUID sesion = fixture.newSessionB();
        for (int i = 0; i < 10; i++) {
            invocarEsperandoFallo(sesion, List.of(fixture.estudiantes().get(0)), List.of("AN"), fixture.otroDocenteUsuarioId());
            assertConnectionCleanAfterCall("tras rechazo #" + i);
        }
        invocar(sesion, fixture.estudiantes(), List.of("AN", "SJC", "EX"), fixture.docenteUsuarioId());
        assertConnectionCleanAfterCall("tras exito final");
        assertEquals(3, fixture.countDetalle(sesion));
    }

    // ------------------------------------------------------------------ mecanismo

    private void invocar(final UUID sesion, final List<UUID> estudiantes, final List<String> estados, final UUID ejecutor) {
        CorrelationIdContext.set(UUID.randomUUID());
        final List<RegistroAsistenciaSesionRepositoryDTO> registros = new ArrayList<>();
        for (int i = 0; i < estudiantes.size(); i++) {
            registros.add(new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(i), estados.get(i)));
        }
        jpaRoutedPort.registrarAsistenciasSesion(new RegistrarAsistenciasSesionRepositoryDTO(sesion, registros, ejecutor));
    }

    private RuntimeException invocarEsperandoFallo(
            final UUID sesion, final List<UUID> estudiantes, final List<String> estados, final UUID ejecutor) {
        try {
            invocar(sesion, estudiantes, estados, ejecutor);
        } catch (final RuntimeException failure) {
            return failure;
        }
        return fail("Se esperaba un rechazo y la llamada tuvo exito.");
    }

    /** Con pool=1: la unica conexion esta libre, en autocommit, sin transaccion abierta y sin fuga. */
    private void assertConnectionCleanAfterCall(final String momento) {
        final Boolean autocommit = jdbcTemplate.execute((Connection c) -> c.getAutoCommit());
        final Integer tranCount = jdbcTemplate.queryForObject("SELECT @@TRANCOUNT", Integer.class);
        assertEquals(Boolean.TRUE, autocommit, "autocommit debe ser true " + momento);
        assertEquals(0, tranCount, "@@TRANCOUNT debe ser 0 " + momento);
        if (dataSource instanceof HikariDataSource hikari && hikari.getHikariPoolMXBean() != null) {
            assertEquals(0, hikari.getHikariPoolMXBean().getActiveConnections(), "Sin conexiones activas (sin fuga) " + momento);
        }
    }

    private int independentDetalleCount(final UUID sesion) {
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
        } catch (final SQLException e) {
            return fail("Lectura desde conexion independiente fallo (posible transaccion abierta): " + e.getClass().getSimpleName());
        }
    }
}
