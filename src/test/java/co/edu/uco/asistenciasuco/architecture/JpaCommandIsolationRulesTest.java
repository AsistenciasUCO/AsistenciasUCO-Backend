package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * LB-002.2C — CMD-ARCH-001..003: el candidato JPA del command {@code registrarAsistenciasSesion} vive
 * unicamente en {@code ..sqlserver.jpa..}, sin transaccion JPA/Spring, sin Spring Data y sin entidades
 * nuevas. Application/Domain y la abstraccion interna del command permanecen libres de JPA. Estas reglas
 * COMPLEMENTAN a {@link JpaIsolationRulesTest}; no la relajan.
 */
class JpaCommandIsolationRulesTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final String JPA_PACKAGES_PATTERN = "jakarta.persistence..";
    private static final String[] JPA_PACKAGES = {
            "jakarta.persistence..",
            "org.springframework.data..",
            "org.hibernate.."
    };

    @Test
    void application_y_domain_no_dependen_de_jpa_hibernate_ni_spring_data_ni_conocen_el_candidato() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(JPA_PACKAGES)
                .orShould().dependOnClassesThat().haveSimpleName("AsistenciaJpaCommandPersistence")
                .orShould().dependOnClassesThat().haveSimpleName("AsistenciaCommandPersistence")
                .check(importedClasses());
    }

    @Test
    void la_abstraccion_interna_del_command_y_el_hibrido_no_dependen_de_jpa() {
        noClasses()
                .that().haveSimpleName("AsistenciaCommandPersistence")
                .or().haveSimpleName("AsistenciaRepositoryHybridSqlServerAdapter")
                .should().dependOnClassesThat().resideInAnyPackage(JPA_PACKAGES)
                .check(importedClasses());
    }

    @Test
    void el_validador_canonico_y_el_soporte_de_procedimientos_no_dependen_de_jpa() {
        noClasses()
                .that().resideInAPackage("..sqlserver.support..")
                .should().dependOnClassesThat().resideInAnyPackage(JPA_PACKAGES)
                .check(importedClasses());
    }

    @Test
    void el_candidato_jpa_del_command_vive_en_sqlserver_jpa_e_implementa_la_abstraccion_interna() {
        classes()
                .that().haveSimpleName("AsistenciaJpaCommandPersistence")
                .should().resideInAPackage("..infrastructure.adapter.secondary.persistence.sqlserver.jpa..")
                .andShould().implement(
                        co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaCommandPersistence.class)
                .check(importedClasses());
    }

    @Test
    void solo_sqlserver_jpa_y_el_composition_root_dependen_de_jakarta_persistence_en_infrastructure_secondary() {
        noClasses()
                .that().resideInAPackage("..infrastructure.adapter.secondary..")
                .and().resideOutsideOfPackage("..sqlserver.jpa..")
                .should().dependOnClassesThat().resideInAPackage(JPA_PACKAGES_PATTERN)
                .check(importedClasses());
    }

    @Test
    void el_candidato_no_usa_transacciones_jpa_ni_spring() {
        noClasses()
                .that().resideInAPackage("..infrastructure.adapter.secondary.persistence.sqlserver.jpa..")
                .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.transaction.annotation.Transactional")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("jakarta.transaction.Transactional")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("jakarta.persistence.EntityTransaction")
                .orShould().dependOnClassesThat().resideInAPackage("org.springframework.transaction..")
                .check(importedClasses());
    }

    @Test
    void ninguna_clase_de_produccion_define_ni_usa_jpa_transaction_manager() {
        noClasses()
                .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.orm.jpa.JpaTransactionManager")
                .check(importedClasses());
    }

    @Test
    void no_se_usa_spring_data_ni_jdbc_disfrazado_en_el_candidato() {
        noClasses()
                .that().resideInAPackage("..infrastructure.adapter.secondary.persistence.sqlserver.jpa..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework.data..")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.hibernate.Session")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.hibernate.jdbc.Work")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("java.sql.Connection")
                .check(importedClasses());
    }

    @Test
    void el_command_no_introduce_entidades_nuevas_solo_existen_las_entidades_de_vista_de_la_query() {
        classes()
                .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().haveNameMatching(".*\\.Uv[A-Za-z]*Entity")
                .allowEmptyShould(true)
                .check(importedClasses());
        noClasses()
                .that().haveSimpleNameContaining("Command")
                .should().beAnnotatedWith(jakarta.persistence.Entity.class)
                .allowEmptyShould(true)
                .check(importedClasses());
    }

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }
}
