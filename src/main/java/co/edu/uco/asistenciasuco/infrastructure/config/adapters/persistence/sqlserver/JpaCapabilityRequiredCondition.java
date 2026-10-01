package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver.SqlServerCoreRepositoryAdapterConfiguration.AsistenciaCommandProvider;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver.SqlServerCoreRepositoryAdapterConfiguration.AsistenciaQueryProvider;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Locale;

/**
 * Activa la capability JPA compartida (el unico {@code EntityManagerFactory}) si el selector de query O el
 * de command de asistencia resuelven a {@code jpa} (LB-002.2 D3), O si el selector de grupo-command
 * resuelve a {@code jpa} (necesario para commands y queries JPA de grupo/sesion). Usa EXACTAMENTE la misma
 * regla de parseo que los selectors del Composition Root, de modo que nunca ocurre "el selector dice JPA y
 * el EMF no existe". Un valor no soportado en CUALQUIERA de los selectores falla el arranque (fail-closed),
 * aunque el otro ya resuelva a jpa.
 */
public final class JpaCapabilityRequiredCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        final Environment environment = context.getEnvironment();
        final boolean queryRequiresJpa = AsistenciaQueryProvider.JPA == AsistenciaQueryProvider.from(
                environment.getProperty(SqlServerCoreRepositoryAdapterConfiguration.ASISTENCIA_QUERY_PROVIDER_PROPERTY));
        final boolean commandRequiresJpa = AsistenciaCommandProvider.JPA == AsistenciaCommandProvider.from(
                environment.getProperty(SqlServerCoreRepositoryAdapterConfiguration.ASISTENCIA_COMMAND_PROVIDER_PROPERTY));
        final boolean grupoRequiresJpa = "jpa".equals(
                environment.getProperty(SqlServerCoreRepositoryAdapterConfiguration.GRUPO_COMMAND_PROVIDER_PROPERTY, "jdbc")
                        .trim().toLowerCase(Locale.ROOT));
        return queryRequiresJpa || commandRequiresJpa || grupoRequiresJpa;
    }
}

