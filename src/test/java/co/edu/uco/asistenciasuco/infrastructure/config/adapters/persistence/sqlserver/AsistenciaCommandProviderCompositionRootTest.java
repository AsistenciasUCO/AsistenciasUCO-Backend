package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaRepositoryHybridSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * LB-002.2C — CMD-CFG-001/002/003/007/008/009/010/011/012 (sin SQL Server; CMD-CFG-006 queda para 2.2D): composicion del unico
 * {@link AsistenciaRepositoryPort} con dos selectors independientes. El routing se observa por el
 * comportamiento del puerto (que tecnologia recibe la llamada), con fakes: NO certifica JPA real.
 */
class AsistenciaCommandProviderCompositionRootTest {

    private static final String QUERY = "app.adapters.persistence.asistencia-query-provider";
    private static final String COMMAND = "app.adapters.persistence.asistencia-command-provider";
    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String SP_LOTE = "usp_registrar_asistencias_sesion";

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

    private ApplicationContextRunner runnerWithRealConfig(final String... properties) {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
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

    private static ConsultarAsistenciasPorGrupoRepositoryDTO consulta() {
        return new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION);
    }

    private void jpaDeberiaFallarConMarcador() {
        when(entityManagerFactory.createEntityManager()).thenThrow(new IllegalStateException("marker-jpa-used"));
    }

    @SuppressWarnings("unchecked")
    private void jdbcQueryDevuelveVacio() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
    }

    /** Invoca el command de lote ignorando el resultado: solo importa QUE tecnologia lo recibio. */
    private static void invocarLote(final AsistenciaRepositoryPort port) {
        assertThrows(DatabaseOperationException.class, () -> port.registrarAsistenciasSesion(lote()));
    }

    @SuppressWarnings("unchecked")
    private void assertLoteVaPorJdbc() {
        verify(jdbc).query(contains(SP_LOTE), any(SqlParameterSource.class), any(RowMapper.class));
        verifyNoInteractions(entityManagerFactory);
    }

    @SuppressWarnings("unchecked")
    private void assertLoteVaPorJpa() {
        verify(entityManagerFactory).createEntityManager();
        verify(jdbc, never()).query(contains(SP_LOTE), any(SqlParameterSource.class), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    private void assertQueryVaPorJdbc() {
        verify(jdbc).query(contains("uv_detalle_asistencia"), any(SqlParameterSource.class), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    private void assertQueryNoVaPorJdbc() {
        verify(jdbc, never()).query(contains("uv_detalle_asistencia"), any(SqlParameterSource.class), any(RowMapper.class));
    }

    // ------------------------------------------------------------------ CMD-CFG-001 / 002

    @Test
    void CMD_CFG_001_default_es_jdbc_el_bean_es_el_adapter_jdbc_sin_envoltorio_y_el_emf_no_se_toca() {
        jdbcQueryDevuelveVacio();
        runner().run(context -> {
            assertEquals(1, context.getBeansOfType(AsistenciaRepositoryPort.class).size());
            final AsistenciaRepositoryPort port = context.getBean(AsistenciaRepositoryPort.class);

            assertInstanceOf(AsistenciaRepositorySqlServerAdapter.class, port);
            invocarLote(port);
            port.consultarAsistenciasPorGrupo(consulta());

            assertLoteVaPorJdbc();
        });
    }

    @ParameterizedTest(name = "query={0}, command=jdbc")
    @CsvSource({"jdbc", "jpa"})
    void CMD_CFG_002_jdbc_explicito_es_igual_al_default_en_ambos_valores_de_query(final String query) {
        runner(QUERY + "=" + query, COMMAND + "=jdbc").run(context -> {
            invocarLote(context.getBean(AsistenciaRepositoryPort.class));

            assertLoteVaPorJdbc();
        });
    }

    // ------------------------------------------------------------------ CMD-CFG-003 / 010

    @Test
    void CMD_CFG_003_command_jpa_enruta_solo_el_command_la_query_sigue_en_jdbc_y_hay_un_unico_puerto() {
        jpaDeberiaFallarConMarcador();
        jdbcQueryDevuelveVacio();
        runner(QUERY + "=jdbc", COMMAND + "=jpa").run(context -> {
            assertEquals(1, context.getBeansOfType(AsistenciaRepositoryPort.class).size());
            final AsistenciaRepositoryPort port = context.getBean(AsistenciaRepositoryPort.class);
            assertInstanceOf(AsistenciaRepositoryHybridSqlServerAdapter.class, port);

            port.consultarAsistenciasPorGrupo(consulta());
            assertQueryVaPorJdbc();
            verifyNoInteractions(entityManagerFactory);

            invocarLote(port);
            assertLoteVaPorJpa();
        });
    }

    @ParameterizedTest(name = "query={0}, command={1} -> queryJpa={2}, comandoJpa={3}")
    @CsvSource({
            "jdbc, jdbc, false, false",
            "jpa,  jdbc, true,  false",
            "jdbc, jpa,  false, true",
            "jpa,  jpa,  true,  true"
    })
    void CMD_CFG_010_matriz_2x2_de_selectors_independientes(
            final String query, final String command, final boolean queryJpa, final boolean commandJpa) {
        jpaDeberiaFallarConMarcador();
        jdbcQueryDevuelveVacio();
        runner(QUERY + "=" + query, COMMAND + "=" + command).run(context -> {
            assertEquals(1, context.getBeansOfType(AsistenciaRepositoryPort.class).size());
            final AsistenciaRepositoryPort port = context.getBean(AsistenciaRepositoryPort.class);
            assertEquals(!queryJpa && !commandJpa, port instanceof AsistenciaRepositorySqlServerAdapter,
                    "Solo (jdbc, jdbc) devuelve el adapter JDBC sin envoltorio.");

            if (queryJpa) {
                assertThrows(DatabaseOperationException.class, () -> port.consultarAsistenciasPorGrupo(consulta()));
                assertQueryNoVaPorJdbc();
                verify(entityManagerFactory, times(1)).createEntityManager();
            } else {
                port.consultarAsistenciasPorGrupo(consulta());
                assertQueryVaPorJdbc();
                verifyNoInteractions(entityManagerFactory);
            }

            invocarLote(port);
            if (commandJpa) {
                verify(entityManagerFactory, times(queryJpa ? 2 : 1)).createEntityManager();
                verify(jdbc, never()).query(contains(SP_LOTE), any(SqlParameterSource.class), any(RowMapper.class));
            } else {
                verify(jdbc).query(contains(SP_LOTE), any(SqlParameterSource.class), any(RowMapper.class));
                verify(entityManagerFactory, times(queryJpa ? 1 : 0)).createEntityManager();
            }
        });
    }

    // ------------------------------------------------------------------ CMD-CFG-007

    @Test
    void CMD_CFG_007_command_jpa_solo_migra_registrar_asistencias_sesion_los_otros_cuatro_commands_no_tocan_el_emf() {
        jdbcQueryDevuelveVacio();
        runner(QUERY + "=jdbc", COMMAND + "=jpa").run(context -> {
            final AsistenciaRepositoryPort port = context.getBean(AsistenciaRepositoryPort.class);

            assertThrows(FeatureUnavailableException.class,
                    () -> port.registrarAsistencia(mock(RegistrarAsistenciaRepositoryDTO.class)));
            assertThrows(DatabaseOperationException.class,
                    () -> port.registrarAsistenciaAutonoma(mock(RegistrarAsistenciaAutonomaRepositoryDTO.class)));
            assertThrows(DatabaseOperationException.class,
                    () -> port.solicitarRevisionAsistencia(mock(SolicitarRevisionAsistenciaRepositoryDTO.class)));
            assertThrows(DatabaseOperationException.class,
                    () -> port.resolverSolicitudRevisionAsistencia(
                            mock(ResolverSolicitudRevisionAsistenciaRepositoryDTO.class)));

            verifyNoInteractions(entityManagerFactory);
            verify(jdbc).query(contains("usp_registrar_asistencia_estudiante_autonomo"),
                    any(SqlParameterSource.class), any(RowMapper.class));
            verify(jdbc).query(contains("usp_radicar_solicitud_revision_asistencia"),
                    any(SqlParameterSource.class), any(RowMapper.class));
            verify(jdbc).query(contains("usp_resolver_solicitud_revision_asistencia"),
                    any(SqlParameterSource.class), any(RowMapper.class));
        });
    }

    // ------------------------------------------------------------------ CMD-CFG-008 / 009

    @ParameterizedTest(name = "command=''{0}''")
    @CsvSource({"valor-invalido", "hibernate", "JPA2", "jpa;jdbc"})
    void CMD_CFG_008_command_invalido_falla_el_arranque_nombrando_la_propiedad(final String invalido) {
        runner(COMMAND + "=" + invalido).run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(mensajes(context.getStartupFailure()).contains(COMMAND));
            assertFalse(mensajes(context.getStartupFailure()).contains(QUERY));
        });
    }

    @Test
    void CMD_CFG_008_command_vacio_falla_el_arranque_nombrando_la_propiedad() {
        runner(COMMAND + "=").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(mensajes(context.getStartupFailure()).contains(COMMAND));
        });
    }

    @Test
    void CMD_CFG_008_query_invalido_con_command_jpa_falla_el_arranque_nombrando_la_propiedad_de_query() {
        runner(QUERY + "=hibernate", COMMAND + "=jpa").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(mensajes(context.getStartupFailure()).contains(QUERY));
        });
    }

    @Test
    void CMD_CFG_009_command_se_parsea_con_trim_y_sin_distinguir_mayusculas_igual_que_query() {
        jpaDeberiaFallarConMarcador();
        runner(QUERY + "=  Jdbc ", COMMAND + "=  JPA ").run(context -> {
            invocarLote(context.getBean(AsistenciaRepositoryPort.class));

            assertLoteVaPorJpa();
        });
    }

    // ------------------------------------------------------------------ CMD-CFG-011 / 012 (configuracion REAL)

    /** LB-002.2E: el perfil local activa tambien el command JPA (antes solo la query). */
    @Test
    void CMD_CFG_011_perfil_local_activa_el_command_jpa() {
        when(entityManagerFactory.createEntityManager()).thenThrow(new IllegalStateException("marker-jpa-used"));
        runnerWithRealConfig("spring.profiles.active=local").run(context -> {
            final AsistenciaRepositoryPort port = context.getBean(AsistenciaRepositoryPort.class);

            invocarLote(port);

            assertLoteVaPorJpa();
            assertInstanceOf(AsistenciaRepositoryHybridSqlServerAdapter.class, port,
                    "local => query y command JPA (hibrido).");
        });
    }

    /** LB-002.2E: dev no activa el command JPA (queda para una decision posterior); sin perfil tampoco. */
    @ParameterizedTest(name = "perfil={0}")
    @CsvSource(nullValues = "NULL", value = {"NULL", "dev"})
    void CMD_CFG_011_sin_perfil_o_en_dev_el_command_permanece_en_jdbc(final String perfil) {
        final String[] properties = perfil == null ? new String[0] : new String[]{"spring.profiles.active=" + perfil};
        when(entityManagerFactory.createEntityManager()).thenThrow(new IllegalStateException("marker-jpa-used"));
        runnerWithRealConfig(properties).run(context -> {
            final AsistenciaRepositoryPort port = context.getBean(AsistenciaRepositoryPort.class);

            invocarLote(port);

            assertLoteVaPorJdbc();
            assertEquals(perfil != null, !(port instanceof AsistenciaRepositorySqlServerAdapter),
                    "dev => query JPA (hibrido) con command JDBC; sin perfil => (jdbc, jdbc) sin envoltorio.");
        });
    }

    @Test
    void CMD_CFG_012_el_rollback_por_propiedad_gana_sobre_el_perfil_y_el_roll_forward_a_jpa_funciona() {
        jpaDeberiaFallarConMarcador();
        runnerWithRealConfig("spring.profiles.active=local", COMMAND + "=jdbc").run(context -> {
            invocarLote(context.getBean(AsistenciaRepositoryPort.class));

            assertLoteVaPorJdbc();
        });
    }

    @Test
    void CMD_CFG_012_roll_forward_explicito_a_jpa_funciona_incluso_sin_perfil() {
        jpaDeberiaFallarConMarcador();
        runnerWithRealConfig(COMMAND + "=jpa").run(context -> {
            invocarLote(context.getBean(AsistenciaRepositoryPort.class));

            assertLoteVaPorJpa();
        });
    }

    private static String mensajes(final Throwable failure) {
        final StringBuilder messages = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(" | ");
        }
        return messages.toString();
    }
}
