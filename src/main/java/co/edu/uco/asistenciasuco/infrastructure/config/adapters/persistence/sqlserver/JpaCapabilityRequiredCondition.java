package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver.SqlServerCoreRepositoryAdapterConfiguration.AsistenciaCommandProvider;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver.SqlServerCoreRepositoryAdapterConfiguration.AsistenciaQueryProvider;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Activa la capability JPA compartida (el unico {@code EntityManagerFactory}) si el selector de query O el
 * de command resuelven a {@code jpa} (LB-002.2 D3). Usa EXACTAMENTE la misma regla de parseo que los
 * selectors del Composition Root, de modo que nunca ocurre "el selector dice JPA y el EMF no existe".
 * Un valor no soportado en CUALQUIERA de los dos selectors falla el arranque (fail-closed), aunque el otro
 * ya resuelva a jpa.
 */
public final class JpaCapabilityRequiredCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        final Environment environment = context.getEnvironment();
        final boolean queryRequiresJpa = AsistenciaQueryProvider.JPA == AsistenciaQueryProvider.from(
                environment.getProperty(SqlServerCoreRepositoryAdapterConfiguration.ASISTENCIA_QUERY_PROVIDER_PROPERTY));
        final boolean commandRequiresJpa = AsistenciaCommandProvider.JPA == AsistenciaCommandProvider.from(
                environment.getProperty(SqlServerCoreRepositoryAdapterConfiguration.ASISTENCIA_COMMAND_PROVIDER_PROPERTY));
        return queryRequiresJpa || commandRequiresJpa;
    }
}
