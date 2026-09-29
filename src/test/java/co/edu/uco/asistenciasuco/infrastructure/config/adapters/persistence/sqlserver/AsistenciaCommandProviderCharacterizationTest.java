package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * LB-002.2B — CARACTERIZACION (debe PASAR hoy y despues del cambio; NO es RED).
 *
 * <p>Fija el comportamiento vigente que el futuro command provider JPA NO debe alterar:
 * default sin propiedad {@code asistencia-command-provider} => commands JDBC (RED-E), estado actual de
 * local/dev {@code query=jpa} + commands JDBC (RED-F) y la tecnica de medicion de EMF usada por RED-B
 * (para demostrar que dicha tecnica cuenta correctamente cuando SI hay EMF y cuando no lo hay).
 * Se conserva aparte del Acceptance RED. Usa solo costuras existentes.</p>
 */
class AsistenciaCommandProviderCharacterizationTest {

    private static final String QUERY_PROPERTY = "app.adapters.persistence.asistencia-query-provider";
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

    /** RED-E (caracterizacion): sin propiedad de command => JDBC; el EMF no se toca. */
    @Test
    void RED_E_sin_propiedad_de_command_el_command_sigue_en_jdbc() {
        runner().run(context -> {
            assertEquals(1, context.getBeansOfType(AsistenciaRepositoryPort.class).size());

            // Con fakes el executor canonico exige una fila: falla DESPUES de invocar JDBC.
            assertThrows(DatabaseOperationException.class,
                    () -> context.getBean(AsistenciaRepositoryPort.class).registrarAsistenciasSesion(lote()));

            verify(jdbc).query(contains("usp_registrar_asistencias_sesion"), any(SqlParameterSource.class),
                    any(RowMapper.class));
            verifyNoInteractions(entityManagerFactory);
        });
    }

    /** RED-F (caracterizacion): query=jpa sin command => query JPA + command JDBC (estado actual de local/dev). */
    @Test
    void RED_F_query_jpa_sin_propiedad_de_command_mantiene_query_jpa_y_command_jdbc() {
        when(entityManagerFactory.createEntityManager()).thenThrow(new IllegalStateException("marker-jpa-used"));
        runner(QUERY_PROPERTY + "=jpa").run(context -> {
            final AsistenciaRepositoryPort port = context.getBean(AsistenciaRepositoryPort.class);

            // Command: JDBC y sin tocar el EMF.
            assertThrows(DatabaseOperationException.class, () -> port.registrarAsistenciasSesion(lote()));
            verify(jdbc).query(contains("usp_registrar_asistencias_sesion"), any(SqlParameterSource.class),
                    any(RowMapper.class));
            verifyNoInteractions(entityManagerFactory);

            // Query: JPA (el marcador del EMF se traduce a DatabaseOperationException) y sin JDBC adicional.
            assertThrows(DatabaseOperationException.class,
                    () -> port.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION)));
            verify(entityManagerFactory).createEntityManager();
            verify(jdbc, never()).query(contains("uv_detalle_asistencia"), any(SqlParameterSource.class),
                    any(RowMapper.class));
        });
    }

    /**
     * Valida la tecnica de medicion de EMF de RED-B: (jdbc, sin command) => 0; query=jpa => exactamente 1.
     * Asi RED-B falla hoy porque la CONDICION ignora command=jpa, no por un defecto de la medicion.
     */
    @Test
    void la_tecnica_de_conteo_de_emf_distingue_ausencia_de_presencia() {
        assertEquals(0, EntityManagerFactoryDefinitionProbe.count(QUERY_PROPERTY + "=jdbc"));
        assertEquals(0, EntityManagerFactoryDefinitionProbe.count());
        assertEquals(1, EntityManagerFactoryDefinitionProbe.count(QUERY_PROPERTY + "=jpa"));
    }
}
