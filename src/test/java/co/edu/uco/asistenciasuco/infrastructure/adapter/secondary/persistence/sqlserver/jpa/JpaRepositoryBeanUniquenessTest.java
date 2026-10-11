package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Verifica, por component scanning real, que cada Port de persistencia tenga una unica implementacion JPA
 * activa (BM-06A-05). Escanea solo el paquete de repositories con EntityManager y TransactionOperations
 * simulados: no requiere SQL Server y no ejecuta SQL.
 */
class JpaRepositoryBeanUniquenessTest {

    private static final String REPOSITORY_PACKAGE =
            "co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository";
    private static final String PORT_PACKAGE = "co.edu.uco.asistenciasuco.application.secondaryports.";
    // 28 repositories de Port (MAINT-003F: + SesionProcedenciaJpaRepository) + AuditEventJpaRepository
    // (@Repository sin Port, TD-010 OPTION A).
    private static final int EXPECTED_REPOSITORIES = 29;

    @Test
    void cada_port_tiene_exactamente_un_repository_activo_con_provider_sqlserver() {
        new ApplicationContextRunner()
                .withUserConfiguration(RepositoryScanConfiguration.class)
                .withPropertyValues(
                        "app.adapters.message-catalog.provider=sqlserver",
                        "app.adapters.parameter-catalog.provider=sqlserver")
                .run(context -> {
                    assertNull(context.getStartupFailure(), "El contexto de repositories debe arrancar");
                    final Map<String, Integer> portCounts = countImplementationsPerPort(context);

                    assertEquals(EXPECTED_REPOSITORIES, countRepositoryBeans(context), "Repositories productivos activos");
                    portCounts.forEach((port, count) ->
                            assertEquals(1, count, "El port " + port + " debe tener exactamente una implementacion"));
                });
    }

    @Test
    void con_provider_de_catalogo_alternativo_no_queda_repository_jpa_compitiendo() {
        new ApplicationContextRunner()
                .withUserConfiguration(RepositoryScanConfiguration.class)
                .withPropertyValues(
                        "app.adapters.message-catalog.provider=AZURE",
                        "app.adapters.parameter-catalog.provider=AZURE_APPCONFIG")
                .run(context -> {
                    assertNull(context.getStartupFailure(), "El contexto de repositories debe arrancar");
                    final Map<String, Integer> portCounts = countImplementationsPerPort(context);

                    assertEquals(0, portCounts.getOrDefault("MessageCatalogPort", 0));
                    assertEquals(0, portCounts.getOrDefault("ParameterCatalogPort", 0));
                });
    }

    /**
     * {@code @Repository} activa la traduccion de excepciones de persistencia, que envuelve el bean en un
     * proxy. Spring Boot usa CGLIB por defecto ({@code proxy-target-class=true}), que no puede subclasear
     * una clase {@code final}: el arranque debe completar con repositories no finales.
     */
    @Test
    void repositories_jpa_son_proxiables_con_traduccion_de_excepciones_de_persistencia_y_cglib() {
        final PersistenceExceptionTranslationPostProcessor translation = new PersistenceExceptionTranslationPostProcessor();
        translation.setProxyTargetClass(true);

        new ApplicationContextRunner()
                .withUserConfiguration(RepositoryScanConfiguration.class)
                .withBean("persistenceExceptionTranslationPostProcessor",
                        PersistenceExceptionTranslationPostProcessor.class, () -> translation)
                .withPropertyValues(
                        "app.adapters.message-catalog.provider=sqlserver",
                        "app.adapters.parameter-catalog.provider=sqlserver")
                .run(context -> assertNull(context.getStartupFailure(),
                        "Los @Repository deben poder proxearse con CGLIB (sin clases final)"));
    }

    private static Map<String, Integer> countImplementationsPerPort(final ApplicationContext context) {
        final Map<String, Integer> counts = new HashMap<>();
        for (final String beanName : context.getBeanDefinitionNames()) {
            final Class<?> type = context.getType(beanName);
            if (type == null || !REPOSITORY_PACKAGE.equals(type.getPackageName())) {
                continue;
            }
            for (final Class<?> implemented : type.getInterfaces()) {
                if (implemented.getName().startsWith(PORT_PACKAGE)) {
                    counts.merge(implemented.getSimpleName(), 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private static int countRepositoryBeans(final ApplicationContext context) {
        int total = 0;
        for (final String beanName : context.getBeanDefinitionNames()) {
            final Class<?> type = context.getType(beanName);
            if (type != null && REPOSITORY_PACKAGE.equals(type.getPackageName())) {
                total++;
            }
        }
        return total;
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = REPOSITORY_PACKAGE)
    static class RepositoryScanConfiguration {

        @Bean
        EntityManager entityManager() {
            return mock(EntityManager.class);
        }

        @Bean
        JpaProcedureExecutor jpaProcedureExecutor(final EntityManager entityManager) {
            return new JpaProcedureExecutor(entityManager);
        }

        @Bean
        TransactionOperations transactionOperations() {
            return mock(TransactionOperations.class);
        }
    }
}
