package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaCommandPersistence;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaQueryPersistence;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.DocenteRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.EstudianteRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.GrupoRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.SesionRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.TipoIdentificacionRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.UsuarioRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaRepositoryHybridSqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.AsistenciaJpaCommandPersistence;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.AsistenciaJpaQueryPersistence;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Locale;

/**
 * Composition Root: selecciona SQL Server como tecnología de persistencia para los
 * repositorios "core" (Grupo, Usuario, TipoIdentificacion, Estudiante, Docente, Sesion, Asistencia).
 *
 * <p>Las Feature Configs (p.ej. {@code GrupoWiringConfiguration}) solo dependen de los
 * {@code *RepositoryPort} declarados aquí; no conocen SQL Server.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.persistence",
        name = "provider",
        havingValue = "sqlserver",
        matchIfMissing = true
)
public class SqlServerCoreRepositoryAdapterConfiguration {

    static final String ASISTENCIA_QUERY_PROVIDER_PROPERTY = "app.adapters.persistence.asistencia-query-provider";
    static final String ASISTENCIA_COMMAND_PROVIDER_PROPERTY = "app.adapters.persistence.asistencia-command-provider";

    @Bean
    public GrupoRepositoryPort grupoRepositoryPort(
            final CanonicalStoredProcedureExecutor canonicalStoredProcedureExecutor,
            final NamedParameterJdbcOperations namedParameterJdbcOperations,
            final TransactionOperations transactionOperations
    ) {
        return new GrupoRepositorySqlServerAdapter(
                canonicalStoredProcedureExecutor,
                namedParameterJdbcOperations,
                transactionOperations
        );
    }

    @Bean
    public UsuarioRepositoryPort usuarioRepositoryPort(
            final CanonicalStoredProcedureExecutor canonicalStoredProcedureExecutor,
            final NamedParameterJdbcOperations namedParameterJdbcOperations
    ) {
        return new UsuarioRepositorySqlServerAdapter(canonicalStoredProcedureExecutor, namedParameterJdbcOperations);
    }

    @Bean
    public TipoIdentificacionRepositoryPort tipoIdentificacionRepositoryPort(final JdbcTemplate jdbcTemplate) {
        return new TipoIdentificacionRepositorySqlServerAdapter(jdbcTemplate);
    }

    @Bean
    public EstudianteRepositoryPort estudianteRepositoryPort(final JdbcTemplate jdbcTemplate) {
        return new EstudianteRepositorySqlServerAdapter(jdbcTemplate);
    }

    @Bean
    public DocenteRepositoryPort docenteRepositoryPort(final JdbcTemplate jdbcTemplate) {
        return new DocenteRepositorySqlServerAdapter(jdbcTemplate);
    }

    @Bean
    public SesionRepositoryPort sesionRepositoryPort(
            final CanonicalStoredProcedureExecutor procedureExecutor,
            final NamedParameterJdbcOperations namedParameterJdbcOperations
    ) {
        return new SesionRepositorySqlServerAdapter(procedureExecutor, namedParameterJdbcOperations);
    }

    /**
     * LB-002.1/LB-002.2: unico {@link AsistenciaRepositoryPort}, con dos selectors INDEPENDIENTES
     * (jdbc por defecto | jpa, fail-closed ante cualquier otro valor):
     * <ul>
     *   <li>{@code asistencia-query-provider}: tecnologia SOLO de {@code consultarAsistenciasPorGrupo};</li>
     *   <li>{@code asistencia-command-provider}: durante LB-002.2 gobierna EXCLUSIVAMENTE
     *       {@code registrarAsistenciasSesion}; el resto de commands es siempre JDBC.</li>
     * </ul>
     * Con {@code (jdbc, jdbc)} se retorna el adapter JDBC sin envoltorio. Sin dual-write ni shadow write. El
     * {@code EntityManagerFactory} solo se resuelve si algun selector es {@code jpa}.
     */
    @Bean
    public AsistenciaRepositoryPort asistenciaRepositoryPort(
            final NamedParameterJdbcOperations namedParameterJdbcOperations,
            final CanonicalStoredProcedureExecutor procedureExecutor,
            final ObjectProvider<EntityManagerFactory> entityManagerFactory,
            final Environment environment
    ) {
        final AsistenciaRepositoryPort jdbcAdapter =
                new AsistenciaRepositorySqlServerAdapter(namedParameterJdbcOperations, procedureExecutor);
        final AsistenciaQueryProvider queryProvider =
                AsistenciaQueryProvider.from(environment.getProperty(ASISTENCIA_QUERY_PROVIDER_PROPERTY));
        final AsistenciaCommandProvider commandProvider =
                AsistenciaCommandProvider.from(environment.getProperty(ASISTENCIA_COMMAND_PROVIDER_PROPERTY));
        if (queryProvider == AsistenciaQueryProvider.JDBC && commandProvider == AsistenciaCommandProvider.JDBC) {
            return jdbcAdapter;
        }
        final AsistenciaQueryPersistence query = queryProvider == AsistenciaQueryProvider.JPA
                ? new AsistenciaJpaQueryPersistence(entityManagerFactory.getObject())
                : jdbcAdapter::consultarAsistenciasPorGrupo;
        final AsistenciaCommandPersistence registrarAsistenciasSesion =
                commandProvider == AsistenciaCommandProvider.JPA
                        ? new AsistenciaJpaCommandPersistence(entityManagerFactory.getObject())
                        : jdbcAdapter::registrarAsistenciasSesion;
        return new AsistenciaRepositoryHybridSqlServerAdapter(jdbcAdapter, query, registrarAsistenciasSesion);
    }

    enum AsistenciaQueryProvider {
        JDBC, JPA;

        static AsistenciaQueryProvider from(final String value) {
            return AsistenciaProviderValue.selectsJpa(ASISTENCIA_QUERY_PROVIDER_PROPERTY, value) ? JPA : JDBC;
        }
    }

    enum AsistenciaCommandProvider {
        JDBC, JPA;

        static AsistenciaCommandProvider from(final String value) {
            return AsistenciaProviderValue.selectsJpa(ASISTENCIA_COMMAND_PROVIDER_PROPERTY, value) ? JPA : JDBC;
        }
    }

    /** Unica regla de parseo de los selectors jdbc|jpa: null -> jdbc; trim + minusculas; cualquier otro falla. */
    private static final class AsistenciaProviderValue {

        private AsistenciaProviderValue() {
        }

        static boolean selectsJpa(final String property, final String value) {
            if (value == null) {
                return false;
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "jdbc" -> false;
                case "jpa" -> true;
                default -> throw new IllegalStateException(
                        property + " no soporta el valor '" + value + "'. Valores: jdbc, jpa.");
            };
        }
    }
}
