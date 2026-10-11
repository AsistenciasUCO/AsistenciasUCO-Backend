package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.ActualizarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.ConsultarSesionesPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.GenerarSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2.SesionV2Controller;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2.SesionesV2DisabledInterceptor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.compatibility.SesionV2SchemaCompatibilityVerifier;
import co.edu.uco.asistenciasuco.infrastructure.config.sesion.SesionesV2ActivationConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guard UTC v2 / v1 (MAINT-003, sustituido en MAINT-003F).
 *
 * <p>MAINT-003 bloqueaba cualquier /api/v2/** mientras UTC-D06 estuviera sin decidir. Con D06 decidido
 * y el contrato DB UTC-D06-POST-FREEZE implementado (gestion-asistencia-db feat/utc-d06-post-freeze), ese
 * bloqueo se SUSTITUYE por un guard de activacion verificable: la superficie v2 es exactamente el
 * contrato de sesiones sin PUT, esta OFF por defecto (404, sin tocar puertos) y su activacion exige que
 * la DB exponga el contrato v2. El wire v1 sigue rechazando offsets: el codec nunca llega a v1.
 */
class SesionUtcActivationGuardTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    // AS-IS v1 code observed for offset input; UTC-D02 proposes VALIDATION_ERROR for v2 (decision pending).
    private static final String V1_INVALID_DATE_TIME_CODE = "ERR_FECHA_HORA_INVALIDA";
    private static final Set<String> V2_CONTRACT = Set.of(
            "POST /api/v2/sesiones",
            "PATCH /api/v2/sesiones/{sesionId}",
            "GET /api/v2/sesiones/{sesionId}",
            "GET /api/v2/sesiones/grupo/{grupoId}");
    private final CrearSesionInputPort create = mock(CrearSesionInputPort.class);
    private final ActualizarSesionInputPort update = mock(ActualizarSesionInputPort.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SesionController(
                    create,
                    mock(ConsultarSesionInputPort.class),
                    mock(ConsultarSesionesPorGrupoInputPort.class),
                    mock(CerrarSesionInputPort.class),
                    update,
                    mock(GenerarSesionesGrupoInputPort.class),
                    UUID::randomUUID))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty()))
            .build();

    @Test
    void apiV2ExposesExactlyTheSesionesUtcContractWithoutPut() throws ClassNotFoundException {
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        final Set<String> v2Mappings = new TreeSet<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
            final Class<?> controller = Class.forName(candidate.getBeanClassName());
            final List<String> classPaths = paths(findMergedAnnotation(controller, RequestMapping.class));
            for (Method method : controller.getDeclaredMethods()) {
                final RequestMapping mapping = findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                final List<String> methodPaths = paths(mapping).isEmpty() ? List.of("") : paths(mapping);
                for (String methodPath : methodPaths) {
                    for (String classPath : classPaths.isEmpty() ? List.of("") : classPaths) {
                        final String full = classPath + methodPath;
                        if (full.startsWith("/api/v2")) {
                            Arrays.stream(mapping.method()).map(RequestMethod::name)
                                    .forEach(verb -> v2Mappings.add(verb + " " + full));
                        }
                    }
                }
            }
        }
        assertEquals(new TreeSet<>(V2_CONTRACT), v2Mappings,
                "La superficie /api/v2 debe ser exactamente UTC-D01 (sin PUT por UTC-D09 y sin rutas vacias)");
    }

    @Test
    void apiV2IsOffByDefaultAndAnswersNotFoundWithoutReachingThePorts() throws Exception {
        assertEquals("${APP_SESIONES_V2_ENABLED:false}", applicationYmlProperty("app.sesiones.v2.enabled"),
                "app.sesiones.v2.enabled debe valer false por defecto");
        new ApplicationContextRunner()
                .withUserConfiguration(SesionesV2ActivationConfiguration.class)
                .run(context -> assertEquals(1, context.getBeansOfType(SesionesV2ActivationConfiguration.class).size()));
        new ApplicationContextRunner()
                .withPropertyValues("app.sesiones.v2.enabled=true")
                .withUserConfiguration(SesionesV2ActivationConfiguration.class)
                .run(context -> assertTrue(context.getBeansOfType(SesionesV2ActivationConfiguration.class).isEmpty()));

        final MockMvc disabled = MockMvcBuilders.standaloneSetup(new SesionV2Controller(create, update, UUID::randomUUID))
                .addMappedInterceptors(new String[] {"/api/v2/sesiones", "/api/v2/sesiones/**"}, new SesionesV2DisabledInterceptor())
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty()))
                .build();
        disabled.perform(post("/api/v2/sesiones").contentType("application/json").content("""
                        {"grupo":"%s","nombre":"Tema","fechaHoraInicio":"2026-07-15T14:00:00Z","fechaHoraFin":"2026-07-15T15:00:00Z"}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        verifyNoInteractions(create, update);
    }

    @Test
    void apiV2ActivationRequiresTheDbProvenanceContract() {
        new ApplicationContextRunner()
                .withBean(EntityManager.class, () -> mock(EntityManager.class))
                .withUserConfiguration(SesionV2SchemaCompatibilityVerifier.class)
                .run(context -> assertTrue(context.getBeansOfType(SesionV2SchemaCompatibilityVerifier.class).isEmpty(),
                        "Con v2 OFF no se consulta la DB"));

        final EntityManager sinContrato = entityManagerReturning(0);
        new ApplicationContextRunner()
                .withPropertyValues("app.sesiones.v2.enabled=true")
                .withBean(EntityManager.class, () -> sinContrato)
                .withUserConfiguration(SesionV2SchemaCompatibilityVerifier.class)
                .run(context -> {
                    final SesionV2SchemaCompatibilityVerifier verifier = context.getBean(SesionV2SchemaCompatibilityVerifier.class);
                    final IllegalStateException error = assertThrows(IllegalStateException.class, () -> verifier.run(null));
                    assertTrue(error.getMessage().contains("UTC-D06-POST-FREEZE"));
                });

        final EntityManager conContrato = entityManagerReturning(1);
        new ApplicationContextRunner()
                .withPropertyValues("app.sesiones.v2.enabled=true")
                .withBean(EntityManager.class, () -> conContrato)
                .withUserConfiguration(SesionV2SchemaCompatibilityVerifier.class)
                .run(context -> context.getBean(SesionV2SchemaCompatibilityVerifier.class).run(null));
    }

    @Test
    void v1CreateStillRejectsOffsetsInsteadOfConvertingThem() throws Exception {
        mvc.perform(post("/api/v1/sesiones").contentType("application/json")
                        .content("""
                                {"grupo":"%s","nombre":"Tema válido",
                                 "fechaHoraInicio":"2026-07-15T16:00:00+02:00","fechaHoraFin":"2026-07-15T17:00:00Z"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(V1_INVALID_DATE_TIME_CODE));
        verifyNoInteractions(create);
    }

    @Test
    void v1PatchStillRejectsOffsetsInsteadOfConvertingThem() throws Exception {
        mvc.perform(patch("/api/v1/sesiones/{id}", UUID.randomUUID()).contentType("application/json")
                        .content("""
                                {"nombre":"Tema válido",
                                 "fechaHoraInicio":"2026-07-15T14:00:00Z","fechaHoraFin":"2026-07-15T15:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(V1_INVALID_DATE_TIME_CODE));
        verifyNoInteractions(update);
    }

    private static List<String> paths(final RequestMapping mapping) {
        if (mapping == null) {
            return List.of();
        }
        final List<String> all = new ArrayList<>(Arrays.asList(mapping.path()));
        all.addAll(Arrays.asList(mapping.value()));
        return all.stream().distinct().toList();
    }

    private static EntityManager entityManagerReturning(final int contrato) {
        final EntityManager entityManager = mock(EntityManager.class);
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(contrato);
        return entityManager;
    }

    private static String applicationYmlProperty(final String name) throws IOException {
        final List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application.yml", new FileSystemResource("src/main/resources/application.yml"));
        return sources.stream()
                .map(source -> source.getProperty(name))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .findFirst()
                .orElse(null);
    }
}
