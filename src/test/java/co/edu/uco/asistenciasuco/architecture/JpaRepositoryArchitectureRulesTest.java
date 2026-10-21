package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JPA-06A RED: Port -> @Repository XxxJpaRepository -> EntityManager. */
class JpaRepositoryArchitectureRulesTest {

    private static final String BASE = "co.edu.uco.asistenciasuco";
    private static final String REPOSITORY_PACKAGE =
            BASE + ".infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository";

    private static final Map<String, List<String>> REPOSITORIES_AND_PORTS = Map.ofEntries(
            Map.entry("AsistenciaJpaRepository", List.of("repository.AsistenciaRepositoryPort")),
            Map.entry("GrupoJpaRepository", List.of("repository.GrupoRepositoryPort")),
            Map.entry("SesionJpaRepository", List.of("repository.SesionRepositoryPort")),
            Map.entry("UsuarioJpaRepository", List.of("repository.UsuarioRepositoryPort")),
            Map.entry("DocenteJpaRepository", List.of("repository.DocenteRepositoryPort")),
            Map.entry("EstudianteJpaRepository", List.of("repository.EstudianteRepositoryPort")),
            Map.entry("TipoIdentificacionJpaRepository", List.of("repository.TipoIdentificacionRepositoryPort")),
            Map.entry("AreaJpaRepository", List.of("academic.AreaQueryPort")),
            Map.entry("AsignaturaJpaRepository", List.of("academic.AsignaturaCommandPort", "academic.AsignaturaQueryPort")),
            Map.entry("AsignaturaDocenteJpaRepository", List.of("academic.AsignaturaDocenteQueryPort")),
            Map.entry("CierrePeriodoJpaRepository", List.of("academic.CierrePeriodoCommandPort")),
            Map.entry("CoordinadorJpaRepository", List.of("academic.CoordinadorCommandPort", "academic.CoordinadorQueryPort")),
            Map.entry("DecanoJpaRepository", List.of("academic.DecanoCommandPort", "academic.DecanoQueryPort")),
            Map.entry("EstudianteProgramaJpaRepository", List.of("academic.EstudianteProgramaQueryPort")),
            Map.entry("FacultadJpaRepository", List.of("academic.FacultadQueryPort")),
            Map.entry("HorarioDocenteJpaRepository", List.of("academic.HorarioDocenteQueryPort")),
            Map.entry("HorarioEstudianteJpaRepository", List.of("academic.HorarioEstudianteQueryPort")),
            Map.entry("InstitucionJpaRepository", List.of("academic.InstitucionQueryPort")),
            Map.entry("MateriaEstudianteJpaRepository", List.of("academic.MateriaEstudianteQueryPort")),
            Map.entry("ParametroJpaRepository", List.of("academic.ParametroQueryPort")),
            Map.entry("PeriodoAcademicoJpaRepository", List.of("academic.PeriodoAcademicoQueryPort")),
            Map.entry("PlanEstudioJpaRepository", List.of("academic.PlanEstudioCommandPort", "academic.PlanEstudioQueryPort")),
            Map.entry("SesionMateriaEstudianteJpaRepository", List.of("academic.SesionMateriaEstudianteQueryPort")),
            Map.entry("AsistenciaSesionEstudianteJpaRepository", List.of("academic.AsistenciaSesionEstudianteQueryPort")),
            Map.entry("InstitutionalScopeJpaRepository", List.of("security.InstitutionalScopePort")),
            Map.entry("ReporteAsistenciaJpaRepository", List.of("report.ReporteAsistenciaQueryPort")),
            Map.entry("MessageCatalogJpaRepository", List.of("catalog.MessageCatalogPort")),
            Map.entry("ParameterCatalogJpaRepository", List.of("catalog.ParameterCatalogPort"))
    );

    private static final List<String> REDUNDANT_QUERY_ROWS = List.of(
            "AreaQueryRow", "AsignaturaDocenteQueryRow", "CoordinadorQueryRow", "DecanoQueryRow",
            "DocenteAsignacionQueryRow", "DocenteIdentidadQueryRow", "EstudianteContextoQueryRow",
            "FacultadQueryRow", "GrupoQueryRow", "HorarioDocenteQueryRow", "HorarioEstudianteQueryRow",
            "InstitucionQueryRow", "ParametroQueryRow", "PeriodoAcademicoQueryRow", "PlanEstudioQueryRow",
            "SesionQueryRow", "TipoIdentificacionQueryRow", "UsuarioQueryRow"
    );

    @Test
    void repositories_jpa_productivos_implementan_ports_directamente_y_llevan_repository() {
        REPOSITORIES_AND_PORTS.forEach((repositoryName, ports) -> {
            final Class<?> repository = load(REPOSITORY_PACKAGE + "." + repositoryName);
            assertTrue(repository.isAnnotationPresent(Repository.class), repositoryName + " debe llevar @Repository");
            ports.forEach(port -> assertTrue(load(BASE + ".application.secondaryports." + port).isAssignableFrom(repository),
                    repositoryName + " debe implementar " + port));
        });
    }

    @Test
    void repositories_jpa_no_dependen_de_jdbc_y_capas_internas_no_dependen_de_repositories() {
        final JavaClasses imported = importedClasses();
        noClasses().that().resideInAPackage("..sqlserver.jpa.repository..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.jdbc..", "java.sql..")
                .check(imported);
        noClasses().that().resideInAnyPackage("..application..", "..domain..")
                .should().dependOnClassesThat().resideInAPackage("..sqlserver.jpa.repository..")
                .check(imported);
    }

    @Test
    void wrappers_delegadores_y_query_rows_uno_a_uno_fueron_retirados() {
        final JavaClasses imported = importedClasses();
        final List<String> wrappers = imported.stream()
                .map(javaClass -> javaClass.getSimpleName())
                .filter(name -> name.endsWith("JpaCommandPersistence")
                        || name.endsWith("JpaQueryPersistence")
                        || name.endsWith("RepositorySqlServerAdapter")
                        || name.equals("AsistenciaRepositoryJpaSqlServerAdapter"))
                .sorted()
                .toList();
        assertTrue(wrappers.isEmpty(), "Persisten wrappers entre Port y @Repository: " + wrappers);

        final List<String> rows = imported.stream()
                .map(javaClass -> javaClass.getSimpleName())
                .filter(REDUNDANT_QUERY_ROWS::contains)
                .sorted()
                .toList();
        assertTrue(rows.isEmpty(), "Persisten QueryRow 1:1 redundantes: " + rows);
    }

    private static Class<?> load(final String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Falta el tipo JPA-06A: " + name, exception);
        }
    }

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }
}
