package co.edu.uco.asistenciasuco.infrastructure.config.adapters.persistence.sqlserver;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * LB-002.2B (soporte de test NUEVO): cuenta las DEFINICIONES de {@code EntityManagerFactory} que produce la
 * configuracion JPA existente ({@link SqlServerJpaAsistenciaQueryAdapterConfiguration}) para un conjunto de
 * propiedades. Solo procesa la {@code @Configuration} y sus {@code @Conditional} sobre un registro de
 * definiciones: NO hay {@code refresh()}, no se instancia ningun bean, no se arranca Hibernate y no se
 * necesita un DataSource.
 *
 * <p>Una definicion cuenta si su nombre es {@code entityManagerFactory} o si su metodo de fabrica declara
 * un tipo de EMF (Spring/Jakarta). La tecnica se valida con el estado actual (query jpa = 1, jdbc = 0) en
 * {@code AsistenciaCommandProviderCharacterizationTest}.</p>
 */
final class EntityManagerFactoryDefinitionProbe {

    private static final String EMF_BEAN_NAME = "entityManagerFactory";
    private static final Set<String> EMF_TYPES = Set.of(
            "org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean",
            "org.springframework.orm.jpa.LocalEntityManagerFactoryBean",
            "jakarta.persistence.EntityManagerFactory");

    private EntityManagerFactoryDefinitionProbe() {
    }

    /** @param properties pares {@code nombre=valor} de la Environment usada para evaluar las condiciones. */
    static int count(final String... properties) {
        final Map<String, Object> source = new HashMap<>();
        for (final String property : properties) {
            final int separator = property.indexOf('=');
            source.put(property.substring(0, separator), property.substring(separator + 1));
        }
        final StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("emf-probe", source));

        final DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        new AnnotatedBeanDefinitionReader(registry, environment)
                .register(SqlServerJpaAsistenciaQueryAdapterConfiguration.class);
        final ConfigurationClassPostProcessor processor = new ConfigurationClassPostProcessor();
        processor.setEnvironment(environment);
        processor.setResourceLoader(new DefaultResourceLoader());
        processor.setBeanClassLoader(EntityManagerFactoryDefinitionProbe.class.getClassLoader());
        processor.postProcessBeanDefinitionRegistry(registry);

        int count = 0;
        for (final String name : registry.getBeanDefinitionNames()) {
            final BeanDefinition definition = registry.getBeanDefinition(name);
            if (EMF_BEAN_NAME.equals(name) || returnsEntityManagerFactory(definition)) {
                count++;
            }
        }
        return count;
    }

    private static boolean returnsEntityManagerFactory(final BeanDefinition definition) {
        return definition instanceof AnnotatedBeanDefinition annotated
                && annotated.getFactoryMethodMetadata() != null
                && EMF_TYPES.contains(annotated.getFactoryMethodMetadata().getReturnTypeName());
    }
}
