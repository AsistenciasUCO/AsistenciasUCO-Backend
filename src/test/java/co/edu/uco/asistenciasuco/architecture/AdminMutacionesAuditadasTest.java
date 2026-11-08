package co.edu.uco.asistenciasuco.architecture;

import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CULM-001 D.6 (DEC-022 §4, CONTRACT v2 §7.2): toda mutacion ({@code POST/PUT/PATCH/DELETE}) bajo {@code /api/v1/admin/**}
 * deja un evento de auditoria, es decir, su handler lleva {@link AuditableOperation}. Recorre el
 * {@link RequestMappingHandlerMapping} real de los controladores de produccion. Las unicas excepciones son operaciones sin
 * escritura (vista previa de plantillas y validacion de cargas) y estan listadas aqui, no en el codigo de produccion.
 */
class AdminMutacionesAuditadasTest {

    private static final String PREFIJO_ADMIN = "/api/v1/admin/";
    private static final Set<RequestMethod> MUTACIONES = Set.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);
    /** Sin escritura: renderizan o validan y no cambian estado (CONTRACT v2 §7.2). */
    private static final Set<String> EXCEPCIONES = Set.of(
            "POST /api/v1/admin/notificaciones/plantillas/{tipo}/vista-previa",
            "POST /api/v1/admin/cargas/validacion");

    @Test
    void toda_mutacion_de_admin_esta_auditada_salvo_las_excepciones_listadas() {
        final Map<String, HandlerMethod> mutaciones = mutacionesAdmin();

        assertFalse(mutaciones.isEmpty(), "No se descubrio ninguna mutacion de /api/v1/admin/**: el inventario esta roto.");
        final List<String> sinAuditoria = mutaciones.entrySet().stream()
                .filter(e -> !EXCEPCIONES.contains(e.getKey()))
                .filter(e -> e.getValue().getMethodAnnotation(AuditableOperation.class) == null)
                .map(Map.Entry::getKey)
                .toList();
        assertEquals(List.of(), sinAuditoria, "Mutaciones de /api/v1/admin/** sin @AuditableOperation");
    }

    @Test
    void las_mutaciones_conocidas_estan_en_el_inventario_y_las_excepciones_siguen_existiendo_o_son_futuras() {
        final Set<String> claves = mutacionesAdmin().keySet();

        for (final String esperada : List.of(
                "PUT /api/v1/admin/parametros/{id}",
                "POST /api/v1/admin/decanos",
                "PATCH /api/v1/admin/decanos/{id}/toggle",
                "PUT /api/v1/admin/decanos/{id}/identidad",
                "POST /api/v1/admin/cierre-masivo",
                "POST /api/v1/admin/facultades",
                "PUT /api/v1/admin/facultades/{id}",
                "PATCH /api/v1/admin/facultades/{id}/estado",
                "POST /api/v1/admin/periodos-academicos",
                "PUT /api/v1/admin/periodos-academicos/{id}",
                "DELETE /api/v1/admin/periodos-academicos/{id}",
                "POST /api/v1/admin/notificaciones/plantillas/{tipo}/versiones",
                "PATCH /api/v1/admin/notificaciones/plantillas/{tipo}/versiones/{version}/activar",
                "POST /api/v1/admin/notificaciones/cola/{id}/reintentar",
                "POST /api/v1/admin/notificaciones/prueba",
                "POST /api/v1/admin/festivos/{anio}/recarga")) {
            assertTrue(claves.contains(esperada), "Falta en el inventario: " + esperada);
        }
        // Las excepciones deben ser mutaciones reales (o de cargas, que llegan con D.5); no se acepta una excepcion huerfana de otra ruta
        for (final String excepcion : EXCEPCIONES) {
            assertTrue(claves.contains(excepcion) || excepcion.contains("/cargas/"),
                    "Excepcion huerfana: " + excepcion);
        }
    }

    private static Map<String, HandlerMethod> mutacionesAdmin() {
        try (GenericWebApplicationContext context = new GenericWebApplicationContext(new MockServletContext())) {
            for (final Class<?> controller : controladoresDeProduccion()) {
                final RootBeanDefinition definition = new RootBeanDefinition(controller);
                definition.setLazyInit(true);
                context.registerBeanDefinition(controller.getName(), definition);
            }
            context.registerBeanDefinition("requestMappingHandlerMapping", new RootBeanDefinition(RequestMappingHandlerMapping.class));
            context.refresh();
            final Map<String, HandlerMethod> resultado = new java.util.TreeMap<>();
            for (final Map.Entry<RequestMappingInfo, HandlerMethod> entry
                    : context.getBean(RequestMappingHandlerMapping.class).getHandlerMethods().entrySet()) {
                final Set<RequestMethod> metodos = entry.getKey().getMethodsCondition().getMethods();
                final Set<RequestMethod> efectivos = metodos.isEmpty() ? MUTACIONES : metodos;
                for (final String ruta : entry.getKey().getPathPatternsCondition().getPatternValues()) {
                    if (!ruta.startsWith(PREFIJO_ADMIN)) {
                        continue;
                    }
                    for (final RequestMethod metodo : new TreeSet<>(efectivos)) {
                        if (MUTACIONES.contains(metodo)) {
                            resultado.put(metodo.name() + " " + ruta, entry.getValue());
                        }
                    }
                }
            }
            return resultado;
        }
    }

    private static List<Class<?>> controladoresDeProduccion() {
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        final List<Class<?>> controladores = new ArrayList<>();
        for (final BeanDefinition candidato : scanner.findCandidateComponents("co.edu.uco.asistenciasuco")) {
            try {
                final Class<?> tipo = Class.forName(candidato.getBeanClassName());
                if (!tipo.getProtectionDomain().getCodeSource().getLocation().getPath().contains("test-classes")) {
                    controladores.add(tipo);
                }
            } catch (final ClassNotFoundException exception) {
                throw new IllegalStateException("No se pudo cargar el controlador " + candidato.getBeanClassName(), exception);
            }
        }
        controladores.sort(Comparator.comparing(Class::getName));
        return controladores;
    }
}
