package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.TechnicalException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * LB-002.2D (CMD-PAR-012): 4 hilos concurrentes ejecutan el MISMO command contra la MISMA
 * sesion/estudiantes, cada uno con su propio {@link CorrelationIdContext}. Se ejecuta una vez contra el
 * baseline JDBC (sesion propia) y una vez contra el candidato JPA REAL resuelto por el Composition Root
 * (sesion propia, sin bypass manual), y se compara el estado final convergente. Sincronizacion
 * deterministica con {@link CyclicBarrier}; sin {@code sleep} como mecanismo de espera.
 */
@Tag("integration")
@SpringBootTest(properties = {
        "app.adapters.persistence.asistencia-query-provider=jdbc",
        "app.adapters.persistence.asistencia-command-provider=jpa"
})
@MockitoBean(types = JwtDecoder.class)
class AsistenciaCommandConcurrencyIT {

    private static final int HILOS = 4;

    @Autowired
    private AsistenciaRepositoryPort jpaRoutedPort;

    @Autowired
    private NamedParameterJdbcOperations namedJdbc;

    @Autowired
    private CanonicalStoredProcedureExecutor procedureExecutor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SesionRepositoryPort sesionRepositoryPort;

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

    @Test
    void cmd_par_012_concurrencia_jdbc_sin_duplicados_ni_excepciones_crudas() throws InterruptedException {
        final UUID sesion = fixture.newSessionA();
        final ConcurrentRunResult resultado = ejecutarConcurrente(jdbcBaseline(), sesion);

        assertSinExcepcionesCrudas(resultado);
        assertEquals(3, fixture.countAsistencia(sesion), "Sin duplicados: 1 cabecera por estudiante.");
        assertEquals(3, fixture.countDetalle(sesion), "Sin duplicados: 1 detalle por estudiante.");
    }

    @Test
    void cmd_par_012_concurrencia_jpa_sin_duplicados_ni_excepciones_crudas() throws InterruptedException {
        final UUID sesion = fixture.newSessionB();
        final ConcurrentRunResult resultado = ejecutarConcurrente(jpaRoutedPort, sesion);

        assertSinExcepcionesCrudas(resultado);
        assertEquals(3, fixture.countAsistencia(sesion), "Sin duplicados: 1 cabecera por estudiante.");
        assertEquals(3, fixture.countDetalle(sesion), "Sin duplicados: 1 detalle por estudiante.");
    }

    @Test
    void cmd_par_012_estado_final_convergente_es_igual_entre_jdbc_y_jpa() throws InterruptedException {
        final UUID sesionJdbc = fixture.newSessionA();
        final UUID sesionJpa = fixture.newSessionB();

        assertSinExcepcionesCrudas(ejecutarConcurrente(jdbcBaseline(), sesionJdbc));
        assertSinExcepcionesCrudas(ejecutarConcurrente(jpaRoutedPort, sesionJpa));

        final Map<UUID, String> estadoJdbc = fixture.persistedState(sesionJdbc);
        final Map<UUID, String> estadoJpa = fixture.persistedState(sesionJpa);
        assertEquals(estadoJdbc.size(), estadoJpa.size(), "Mismo numero de filas convergentes en JDBC y JPA.");
        for (final UUID estudiante : fixture.estudiantes()) {
            assertEquals(estadoJdbc.get(estudiante), estadoJpa.get(estudiante),
                    "Estado final convergente distinto para " + estudiante);
        }
    }

    // ------------------------------------------------------------------ mecanismo

    private record ConcurrentRunResult(List<String> rawUncontrolledExceptions) {
    }

    private ConcurrentRunResult ejecutarConcurrente(final AsistenciaRepositoryPort port, final UUID sesion) throws InterruptedException {
        final CyclicBarrier barrera = new CyclicBarrier(HILOS);
        final ExecutorService executor = Executors.newFixedThreadPool(HILOS);
        final AtomicInteger uncontrolled = new AtomicInteger(0);
        final AtomicReference<List<String>> mensajes = new AtomicReference<>(new java.util.concurrent.CopyOnWriteArrayList<>());
        try {
            for (int i = 0; i < HILOS; i++) {
                executor.submit(() -> {
                    CorrelationIdContext.set(UUID.randomUUID());
                    try {
                        barrera.await(10, TimeUnit.SECONDS);
                        final List<RegistroAsistenciaSesionRepositoryDTO> registros = List.of(
                                new RegistroAsistenciaSesionRepositoryDTO(fixture.estudiantes().get(0), "AN"),
                                new RegistroAsistenciaSesionRepositoryDTO(fixture.estudiantes().get(1), "SJC"),
                                new RegistroAsistenciaSesionRepositoryDTO(fixture.estudiantes().get(2), "EX"));
                        port.registrarAsistenciasSesion(new RegistrarAsistenciasSesionRepositoryDTO(
                                sesion, registros, fixture.docenteUsuarioId()));
                    } catch (final ApplicationException | TechnicalException controlada) {
                        // Aceptable: contencion concurrente traducida de forma controlada (nunca cruda).
                        mensajes.get().add("controlada:" + controlada.getClass().getSimpleName());
                    } catch (final Exception cruda) {
                        uncontrolled.incrementAndGet();
                        mensajes.get().add("CRUDA:" + cruda.getClass().getName() + ":" + cruda.getMessage());
                    } finally {
                        CorrelationIdContext.clear();
                    }
                });
            }
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "Los 4 hilos deben completar en 30s.");
        }
        System.out.println("[CONCURRENCY-OBS] " + mensajes.get());
        if (uncontrolled.get() > 0) {
            return new ConcurrentRunResult(mensajes.get());
        }
        return new ConcurrentRunResult(List.of());
    }

    private static void assertSinExcepcionesCrudas(final ConcurrentRunResult resultado) {
        if (!resultado.rawUncontrolledExceptions().isEmpty()) {
            fail("Excepcion(es) cruda(s) sin traducir bajo concurrencia: " + resultado.rawUncontrolledExceptions());
        }
    }
}
