package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LB-002.2B — ACCEPTANCE RED (congelado; ver {@code RED_SNAPSHOT.md}).
 *
 * <p>Contrato futuro (DECISION D2/D3, aprobado en la revision 2.2A): la propiedad
 * {@code app.adapters.persistence.asistencia-command-provider} ({@code jdbc|jpa}, default {@code jdbc},
 * fail-closed) gobierna EXCLUSIVAMENTE {@code registrarAsistenciasSesion}, es independiente del selector
 * de query y, con {@code command=jpa}, existe exactamente un {@code EntityManagerFactory} aunque
 * {@code query=jdbc}.</p>
 *
 * <p>Estos tests usan SOLO costuras productivas YA EXISTENTES (propiedades, Composition Root, EMF
 * condicional, puerto compuesto, {@code EntityManagerFactory}/{@code NamedParameterJdbcOperations} fakes
 * como en {@code AsistenciaQueryProviderCompositionRootTest}). NO referencian clases futuras. Cada uno
 * FALLA hoy por COMPORTAMIENTO observable ausente (la propiedad de command aun no se interpreta), no por
 * una clase inexistente. Son routing/wiring con fakes: NO certifican JPA (eso lo hacen el feasibility y la
 * paridad en SQL Server real).</p>
 *
 * <p>Un implementador NO puede modificar expectativas, eliminar, {@code @Disabled}, cambiar asserts ni
 * propiedades para conseguir GREEN. Un cambio aqui es {@code TEST_CONTRACT_CONFLICT} + revision humana.</p>
 */
class AsistenciaCommandProviderAcceptanceRedTest {

    private static final String QUERY_PROPERTY = "app.adapters.persistence.asistencia-query-provider";
    private static final String COMMAND_PROPERTY = "app.adapters.persistence.asistencia-command-provider";
    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESION = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);

    @BeforeEach
    void establecerCorrelacion() {
        CorrelationIdContext.set(UUID.randomUUID());
    }

    @AfterEach
    void limpiarCorrelacion() {
        CorrelationIdContext.clear();
    }

    private ApplicationContextRunner runner(final String... properties) {
        return new ApplicationContextRunner()
                .withPropertyValues("app.adapters.persistence.provider=sqlserver")
                .withPropertyValues(properties)
                .withBean(NamedParameterJdbcOperations.class, () -> jdbc)
                .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
                .withBean(TransactionOperations.class, () -> mock(TransactionOperations.class))
                .withBean(EntityManagerFactory.class, () -> entityManagerFactory)
                .withUserConfiguration(
                        SqlServerProcedureSupportConfiguration.class,
                        SqlServerCoreRepositoryAdapterConfiguration.class
                );
    }

    private static RegistrarAsistenciasSesionRepositoryDTO lote() {
        return new RegistrarAsistenciasSesionRepositoryDTO(
                SESION,
                List.of(new RegistroAsistenciaSesionRepositoryDTO(UUID.randomUUID(), "AN")),
                UUID.randomUUID());
    }

    /** Ejecuta el command ignorando el resultado: solo importa QUE tecnologia recibio la llamada. */
    private static void invokeCommandIgnoringOutcome(final AsistenciaRepositoryPort port) {
        try {
            port.registrarAsistenciasSesion(lote());
        } catch (RuntimeException expectedWithFakes) {
            // Con fakes el resultado canonico no existe: el fallo es esperado y no forma parte del contrato probado.
        }
    }

    // ------------------------------------------------------------------ RED-A

    /** RED-A: command=jpa (query=jdbc) => registrarAsistenciasSesion usa JPA, no JDBC. */
    @Test
    void RED_A_command_jpa_enruta_registrarAsistenciasSesion_por_jpa_y_no_por_jdbc() {
        when(entityManagerFactory.createEntityManager()).thenThrow(new IllegalStateException("marker-jpa-used"));
        runner(QUERY_PROPERTY + "=jdbc", COMMAND_PROPERTY + "=jpa").run(context -> {
            assertEquals(1, context.getBeansOfType(AsistenciaRepositoryPort.class).size());

            invokeCommandIgnoringOutcome(context.getBean(AsistenciaRepositoryPort.class));

            verify(entityManagerFactory).createEntityManager();
            verify(jdbc, never()).query(contains("usp_registrar_asistencias_sesion"),
                    any(SqlParameterSource.class), any(RowMapper.class));
        });
    }

    // ------------------------------------------------------------------ RED-B

    /**
     * RED-B: query=jdbc + command=jpa => debe existir exactamente UN EntityManagerFactory. Se cuentan las
     * definiciones (sin arrancar Hibernate) de la configuracion JPA existente.
     */
    @Test
    void RED_B_con_query_jdbc_y_command_jpa_existe_exactamente_un_entity_manager_factory() {
        final int emfDefinitions = EntityManagerFactoryDefinitionProbe.count(
                QUERY_PROPERTY + "=jdbc", COMMAND_PROPERTY + "=jpa");

        assertEquals(1, emfDefinitions,
                "command=jpa exige el EntityManagerFactory aunque query=jdbc. Definiciones EMF: " + emfDefinitions);
    }

    // ------------------------------------------------------------------ RED-C

    /** RED-C: command con valor invalido => falla el arranque (fail-closed), nombrando la propiedad. */
    @ParameterizedTest(name = "command=''{0}''")
    @ValueSource(strings = {"valor-invalido", "hibernate", ""})
    void RED_C_command_invalido_falla_el_arranque_fail_closed(final String invalidValue) {
        runner(COMMAND_PROPERTY + "=" + invalidValue).run(context -> {
            final Throwable startupFailure = context.getStartupFailure();

            assertNotNull(startupFailure, "Un valor no soportado del command provider debe fallar el arranque.");
            assertTrue(allMessages(startupFailure).contains("asistencia-command-provider"),
                    "El mensaje del fallo debe nombrar la propiedad asistencia-command-provider.");
        });
    }

    // ------------------------------------------------------------------ RED-D

    /**
     * RED-D: independencia de selectors. query=jdbc + command=jpa => la query sigue en JDBC y el command
     * usa JPA, dentro de UN unico AsistenciaRepositoryPort.
     */
    @Test
    @SuppressWarnings("unchecked")
    void RED_D_selectors_independientes_query_jdbc_mas_command_jpa() {
        when(entityManagerFactory.createEntityManager()).thenThrow(new IllegalStateException("marker-jpa-used"));
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        runner(QUERY_PROPERTY + "=jdbc", COMMAND_PROPERTY + "=jpa").run(context -> {
            assertEquals(1, context.getBeansOfType(AsistenciaRepositoryPort.class).size());
            final AsistenciaRepositoryPort port = context.getBean(AsistenciaRepositoryPort.class);

            port.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION));
            verify(jdbc).query(contains("uv_detalle_asistencia"), any(SqlParameterSource.class), any(RowMapper.class));
            verify(entityManagerFactory, never()).createEntityManager();

            invokeCommandIgnoringOutcome(port);
            verify(entityManagerFactory).createEntityManager();
            verify(jdbc, never()).query(contains("usp_registrar_asistencias_sesion"),
                    any(SqlParameterSource.class), any(RowMapper.class));
        });
    }

    // ------------------------------------------------------------------ soporte

    private static String allMessages(final Throwable failure) {
        final StringBuilder messages = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(" | ");
        }
        return messages.toString();
    }
}
