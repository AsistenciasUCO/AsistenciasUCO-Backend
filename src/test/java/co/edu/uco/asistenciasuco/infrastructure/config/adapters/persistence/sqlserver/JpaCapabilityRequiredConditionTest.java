package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * LB-002.2C — CMD-CFG-004/005/009: la condicion del unico {@code EntityManagerFactory} usa la MISMA regla de
 * parseo que los selectors, activa la capability si query=jpa OR command=jpa y falla cerrado ante un valor
 * invalido en cualquiera de los dos.
 */
class JpaCapabilityRequiredConditionTest {

    private static final String QUERY = "app.adapters.persistence.asistencia-query-provider";
    private static final String COMMAND = "app.adapters.persistence.asistencia-command-provider";

    private final JpaCapabilityRequiredCondition condition = new JpaCapabilityRequiredCondition();

    private boolean matches(final String query, final String command) {
        final MockEnvironment environment = new MockEnvironment();
        if (query != null) {
            environment.setProperty(QUERY, query);
        }
        if (command != null) {
            environment.setProperty(COMMAND, command);
        }
        final ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        return condition.matches(context, null);
    }

    @ParameterizedTest(name = "query={0}, command={1} -> {2}")
    @CsvSource(nullValues = "NULL", value = {
            "NULL, NULL, false",
            "jdbc, jdbc, false",
            "jdbc, NULL, false",
            "NULL, jdbc, false",
            "jpa, jdbc, true",
            "jpa, NULL, true",
            "jdbc, jpa, true",
            "NULL, jpa, true",
            "jpa, jpa, true"
    })
    void matriz_2x2_activa_la_capability_si_query_o_command_es_jpa(
            final String query, final String command, final boolean esperado) {
        assertEquals(esperado, matches(query, command));
    }

    @ParameterizedTest(name = "query=''{0}'', command=''{1}''")
    @CsvSource(nullValues = "NULL", value = {
            "'  JPA ', jdbc, true",
            "Jpa, NULL, true",
            "jdbc, '  JPA ', true",
            "NULL, JPA, true",
            "'jdbc ', ' JDBC', false"
    })
    void usa_la_misma_regla_de_parseo_que_los_selectors_trim_y_sin_distinguir_mayusculas(
            final String query, final String command, final boolean esperado) {
        assertEquals(esperado, matches(query, command));
    }

    @ParameterizedTest(name = "query={0}, command={1}")
    @CsvSource(nullValues = "NULL", value = {
            "hibernate, NULL",
            "NULL, hibernate",
            "jpa, hibernate",
            "hibernate, jpa",
            "jdbc, valor-invalido",
            "'', NULL",
            "NULL, ''"
    })
    void valor_invalido_en_cualquiera_de_los_dos_selectors_falla_cerrado_incluso_si_el_otro_es_jpa(
            final String query, final String command) {
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> matches(query, command));

        assertTrue(exception.getMessage().contains("asistencia-query-provider")
                || exception.getMessage().contains("asistencia-command-provider"));
    }

    @Test
    void el_mensaje_de_fallo_nombra_la_propiedad_invalida_exacta() {
        final IllegalStateException query = assertThrows(IllegalStateException.class, () -> matches("x", null));
        final IllegalStateException command = assertThrows(IllegalStateException.class, () -> matches(null, "x"));

        assertTrue(query.getMessage().contains(QUERY));
        assertFalse(query.getMessage().contains(COMMAND));
        assertTrue(command.getMessage().contains(COMMAND));
        assertFalse(command.getMessage().contains(QUERY));
    }

    // ---- aplicada a la @Configuration real, contando definiciones sin arrancar Hibernate

    @ParameterizedTest(name = "query={0}, command={1} -> emf definidos={2}")
    @CsvSource({
            "jdbc, jdbc, 0",
            "jpa, jdbc, 1",
            "jdbc, jpa, 1",
            "jpa, jpa, 1"
    })
    void la_configuracion_real_define_un_unico_emf_solo_si_algun_selector_es_jpa(
            final String query, final String command, final int definiciones) {
        assertEquals(definiciones,
                EntityManagerFactoryDefinitionProbe.count(QUERY + "=" + query, COMMAND + "=" + command));
    }

    @Test
    void con_ambos_selectors_en_jdbc_o_ausentes_no_se_construye_entity_manager_factory() {
        new ApplicationContextRunner()
                .withPropertyValues(QUERY + "=jdbc", COMMAND + "=jdbc")
                .withUserConfiguration(SqlServerJpaAsistenciaQueryAdapterConfiguration.class)
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .run(context -> assertTrue(context.getBeansOfType(EntityManagerFactory.class).isEmpty()));
        new ApplicationContextRunner()
                .withUserConfiguration(SqlServerJpaAsistenciaQueryAdapterConfiguration.class)
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .run(context -> assertFalse(context.containsBean("entityManagerFactory")));
    }

    @Test
    void con_valor_invalido_en_command_la_configuracion_jpa_tampoco_arranca() {
        new ApplicationContextRunner()
                .withPropertyValues(COMMAND + "=hibernate")
                .withUserConfiguration(SqlServerJpaAsistenciaQueryAdapterConfiguration.class)
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void entorno_estandar_sin_propiedades_no_activa_la_capability() {
        final ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(new StandardEnvironment());

        assertFalse(condition.matches(context, null));
    }
}
