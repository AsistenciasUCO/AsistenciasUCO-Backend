package co.edu.uco.asistenciasuco.architecture;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CULM-001 A.1 (B-RED-01 y B-RED-02): la matricula solo se hace por QR/PIN (HU176B) o manualmente por el docente
 * titular (HU050). HU026, HU053, HU054 y HU055 quedan descartadas: no debe quedar ninguna pieza de produccion de las
 * solicitudes de ingreso. Los reclamos (SolicitudRevision*) son homonimos y se conservan.
 */
class SolicitudesIngresoRetiradasTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final Pattern NOMBRE_RETIRADO =
            Pattern.compile(".*(SolicitudIngreso|SolicitarIngreso|SolicitudesIngreso).*");
    private static final List<String> PAQUETES_RETIRADOS = List.of(
            ".consultarsolicitudesingreso", ".resolversolicitudingreso",
            ".retirarsolicitudingreso", ".solicitaringresogrupo");
    private static final List<String> PLANTILLAS_RETIRADAS = List.of(
            "SOLICITUD_INGRESO_REGISTRADA", "SOLICITUD_INGRESO_RESUELTA", "SOLICITUD_INGRESO_RETIRADA");

    private static JavaClasses produccion() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }

    @Test
    void no_quedan_clases_de_produccion_de_solicitudes_de_ingreso() {
        final JavaClasses clases = produccion();
        final List<String> sobrantes = clases.stream()
                .filter(clase -> NOMBRE_RETIRADO.matcher(clase.getSimpleName()).matches()
                        || PAQUETES_RETIRADOS.stream().anyMatch(paquete -> (clase.getPackageName() + ".").contains(paquete + ".")))
                .map(JavaClass::getName)
                .sorted()
                .toList();

        assertTrue(sobrantes.isEmpty(), () -> "Piezas de solicitudes de ingreso aun presentes: " + sobrantes);
    }

    @Test
    void los_reclamos_homonimos_se_conservan() {
        // Asercion positiva (R-11): evita un falso verde si el importador no encuentra clases.
        final JavaClasses clases = produccion();
        assertTrue(clases.contain(BASE_PACKAGE
                        + ".infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.SolicitudRevisionEstudianteJpaRepository"),
                "SolicitudRevisionEstudianteJpaRepository (reclamos) debe seguir existiendo");
    }

    @Test
    void notificacion_no_declara_tipos_de_solicitud_de_ingreso() {
        final List<String> campos = Arrays.stream(Notificacion.class.getDeclaredFields())
                .map(Field::getName)
                .filter(nombre -> nombre.startsWith("TIPO_SOLICITUD_INGRESO"))
                .toList();

        assertTrue(campos.isEmpty(), () -> "Tipos de notificacion retirados aun declarados: " + campos);
    }

    @Test
    void no_quedan_plantillas_de_correo_de_solicitudes_de_ingreso() {
        for (final String tipo : PLANTILLAS_RETIRADAS) {
            assertNull(getClass().getClassLoader().getResource("notification/templates/" + tipo + ".es.properties"),
                    () -> "Plantilla retirada aun presente: " + tipo);
        }
    }
}
