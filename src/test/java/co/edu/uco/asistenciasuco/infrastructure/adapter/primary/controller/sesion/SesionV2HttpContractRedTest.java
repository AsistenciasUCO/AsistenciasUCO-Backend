package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.ActualizarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto.ActualizarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.ConsultarSesionesPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.GenerarSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.config.jackson.JacksonInputConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RED for the UTC v2 write surface (MAINT-003B, UTC-D01..D05, D08, D09; D02 error contract MAINT-003C).
 *
 * <p>Derived from UTC_API_V2_PROPOSAL, not from an implementation: the controller is discovered by
 * its /api/v2/sesiones mapping and built from the existing input ports, so the test does not
 * dictate class names. GET v2 is intentionally absent: its behavior for HISTORICAL_TZ_UNDETERMINED
 * rows depends on UTC-D06, which is not decided (see UTC_D06_HISTORICAL_PROVENANCE_OPTIONS.md).
 * Expected today: every test fails with "no controller maps /api/v2/sesiones".
 */
class SesionV2HttpContractRedTest {

    private static final UUID ACTOR = UUID.fromString("4e9739aa-83ca-460e-bc77-9572bab52fab");
    private static final UUID GROUP = UUID.fromString("e38345e1-8b26-45ae-9ee0-07cf7ee535ad");
    private static final UUID SESSION = UUID.fromString("94ea3dc1-847a-4d4f-9222-d8bba0c6427c");
    private static final LocalDateTime UTC_14 = LocalDateTime.of(2026, 7, 15, 14, 0);

    private final CrearSesionInputPort create = mock(CrearSesionInputPort.class);
    private final ActualizarSesionInputPort update = mock(ActualizarSesionInputPort.class);
    private final Map<Class<?>, Object> collaborators = collaborators();

    private Map<Class<?>, Object> collaborators() {
        final Map<Class<?>, Object> map = new LinkedHashMap<>();
        map.put(CrearSesionInputPort.class, create);
        map.put(ActualizarSesionInputPort.class, update);
        map.put(ConsultarSesionInputPort.class, mock(ConsultarSesionInputPort.class));
        map.put(ConsultarSesionesPorGrupoInputPort.class, mock(ConsultarSesionesPorGrupoInputPort.class));
        map.put(CerrarSesionInputPort.class, mock(CerrarSesionInputPort.class));
        map.put(GenerarSesionesGrupoInputPort.class, mock(GenerarSesionesGrupoInputPort.class));
        map.put(AuthenticatedUserResolver.class, (AuthenticatedUserResolver) () -> ACTOR);
        return map;
    }

    @Test
    void d02d03PostWithBerlinSummerOffsetReachesPortAsSameUtcInstant() throws Exception {
        assertCreateMapsTo("2026-07-15T16:00:00+02:00", "2026-07-15T17:00:00+02:00", UTC_14, UTC_14.plusHours(1));
    }

    @Test
    void d02d03PostAcceptsZuluBogotaAndLondonWinterSummer() throws Exception {
        assertCreateMapsTo("2026-07-15T14:00:00Z", "2026-07-15T15:00:00Z", UTC_14, UTC_14.plusHours(1));
        assertCreateMapsTo("2026-07-15T09:00:00-05:00", "2026-07-15T10:00:00-05:00", UTC_14, UTC_14.plusHours(1));
        assertCreateMapsTo("2026-01-15T10:00:00+00:00", "2026-01-15T11:00:00+00:00",
                LocalDateTime.of(2026, 1, 15, 10, 0), LocalDateTime.of(2026, 1, 15, 11, 0));
        assertCreateMapsTo("2026-07-15T15:00:00+01:00", "2026-07-15T16:00:00+01:00", UTC_14, UTC_14.plusHours(1));
    }

    @Test
    void d04BerlinOverlapOffsetsAreDistinctInstants() throws Exception {
        final LocalDateTime first = createdStart("2026-10-25T02:30:00+02:00", "2026-10-25T03:30:00+02:00");
        final LocalDateTime second = createdStart("2026-10-25T02:30:00+01:00", "2026-10-25T03:30:00+01:00");
        assertEquals(LocalDateTime.of(2026, 10, 25, 0, 30), first);
        assertEquals(LocalDateTime.of(2026, 10, 25, 1, 30), second);
        assertNotEquals(first, second);
    }

    @Test
    void d02NaiveDateTimeIsRejectedWithoutReachingThePort() throws Exception {
        mvc().perform(post("/api/v2/sesiones").contentType("application/json").content(body(
                        "2026-07-15T16:00:00", "2026-07-15T17:00:00+02:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("fechaHoraInicio"));
        verifyNoInteractions(create);
    }

    @Test
    void d02BothInstantsAreRequired() throws Exception {
        mvc().perform(post("/api/v2/sesiones").contentType("application/json").content("""
                        {"grupo":"%s","nombre":"Clase de prueba","fechaHoraInicio":"2026-07-15T16:00:00+02:00"}
                        """.formatted(GROUP)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("fechaHoraFin"));
        verifyNoInteractions(create);
    }

    @Test
    void d02MissingInstantIsReportedAsFieldRequired() throws Exception {
        mvc().perform(post("/api/v2/sesiones").contentType("application/json").content("""
                        {"grupo":"%s","nombre":"Clase de prueba","fechaHoraFin":"2026-07-15T17:00:00+02:00"}
                        """.formatted(GROUP)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("fechaHoraInicio"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_REQUIRED"));
        verifyNoInteractions(create);
    }

    @Test
    void d02OffsetOutsideTheProfileIsFieldInvalidFormatNotInternalError() throws Exception {
        // MAINT-003C: the codec signals IllegalArgumentException, which GlobalExceptionHandler maps to 500.
        mvc().perform(post("/api/v2/sesiones").contentType("application/json").content(body(
                        "2026-07-15T16:00:00+02", "2026-07-15T17:00:00+02:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("fechaHoraInicio"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_INVALID_FORMAT"));
        verifyNoInteractions(create);
    }

    @Test
    void d02FieldErrorsAreAggregatedInOneValidationResponse() throws Exception {
        mvc().perform(post("/api/v2/sesiones").contentType("application/json").content("""
                        {"grupo":"%s","nombre":"","fechaHoraInicio":"2026-07-15T16:00:00",
                         "fechaHoraFin":"2026-07-15T17:00:00+02:00"}
                        """.formatted(GROUP)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[*].field", containsInAnyOrder("nombre", "fechaHoraInicio")));
        verifyNoInteractions(create);
    }

    @Test
    void d02ErrorBodyDoesNotEchoTheSuppliedDateTime() throws Exception {
        final String supplied = "2026-07-15T16:00:00+99:99";
        mvc().perform(post("/api/v2/sesiones").contentType("application/json").content(body(
                        supplied, "2026-07-15T17:00:00+02:00")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString(supplied))));
        verifyNoInteractions(create);
    }

    @Test
    void d02PatchValidatesInstantsBeforeReachingThePort() throws Exception {
        // v1 PATCH has no HTTP validator; v2 must not inherit that gap.
        mvc().perform(patch("/api/v2/sesiones/{id}", SESSION).contentType("application/json").content("""
                        {"nombre":"Clase movida",
                         "fechaHoraInicio":"2026-07-15T16:00:00","fechaHoraFin":"2026-07-15T17:30:00+02:00"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("fechaHoraInicio"));
        verifyNoInteractions(update);
    }

    @Test
    void d08ActorComesFromTokenAndBodyActorFieldsAreUnknown() throws Exception {
        mvc().perform(post("/api/v2/sesiones").contentType("application/json").content("""
                        {"grupo":"%s","nombre":"Clase de prueba","docente":"%s",
                         "fechaHoraInicio":"2026-07-15T16:00:00+02:00","fechaHoraFin":"2026-07-15T17:00:00+02:00"}
                        """.formatted(GROUP, UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("FIELD_UNKNOWN"));
        verifyNoInteractions(create);
    }

    @Test
    void d02d03PatchConvertsOffsetsAndKeepsFullReplacementSemantics() throws Exception {
        mvc().perform(patch("/api/v2/sesiones/{id}", SESSION).contentType("application/json").content("""
                        {"nombre":"Clase movida",
                         "fechaHoraInicio":"2026-07-15T16:00:00+02:00","fechaHoraFin":"2026-07-15T17:30:00+02:00"}
                        """))
                .andExpect(status().is2xxSuccessful());
        final var captor = ArgumentCaptor.forClass(ActualizarSesionDTO.class);
        verify(update).execute(captor.capture());
        assertEquals(SESSION, captor.getValue().getSesion());
        assertEquals(UTC_14, captor.getValue().getFechaHoraInicio());
        assertEquals(UTC_14.plusMinutes(90), captor.getValue().getFechaHoraFin());
        assertEquals(ACTOR, captor.getValue().getUsuarioEjecutor());
    }

    @Test
    void d09PutDoesNotExistInV2() throws Exception {
        mvc().perform(put("/api/v2/sesiones/{id}", SESSION).contentType("application/json").content("""
                        {"nombre":"Clase movida",
                         "fechaHoraInicio":"2026-07-15T16:00:00+02:00","fechaHoraFin":"2026-07-15T17:30:00+02:00"}
                        """))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(update);
    }

    private void assertCreateMapsTo(final String start, final String end,
                                    final LocalDateTime expectedStart, final LocalDateTime expectedEnd) throws Exception {
        org.mockito.Mockito.clearInvocations(create);
        mvc().perform(post("/api/v2/sesiones").contentType("application/json").content(body(start, end)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true));
        final var captor = ArgumentCaptor.forClass(CrearSesionDTO.class);
        verify(create).execute(captor.capture());
        assertEquals(GROUP, captor.getValue().getGrupo());
        assertEquals(expectedStart, captor.getValue().getFechaHoraInicio(), "inicio UTC para " + start);
        assertEquals(expectedEnd, captor.getValue().getFechaHoraFin(), "fin UTC para " + end);
        assertEquals(ACTOR, captor.getValue().getUsuarioEjecutor());
    }

    private LocalDateTime createdStart(final String start, final String end) throws Exception {
        org.mockito.Mockito.clearInvocations(create);
        mvc().perform(post("/api/v2/sesiones").contentType("application/json").content(body(start, end)))
                .andExpect(status().isCreated());
        final var captor = ArgumentCaptor.forClass(CrearSesionDTO.class);
        verify(create).execute(captor.capture());
        return captor.getValue().getFechaHoraInicio();
    }

    private static String body(final String start, final String end) {
        return """
                {"grupo":"%s","nombre":"Clase de prueba","fechaHoraInicio":"%s","fechaHoraFin":"%s"}
                """.formatted(GROUP, start, end);
    }

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(v2Controller())
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty()))
                .setMessageConverters(new JacksonJsonHttpMessageConverter(productionJsonMapper()))
                .build();
    }

    private Object v2Controller() {
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        for (BeanDefinition candidate : scanner.findCandidateComponents("co.edu.uco.asistenciasuco")) {
            final Class<?> type = load(candidate.getBeanClassName());
            final RequestMapping mapping = findMergedAnnotation(type, RequestMapping.class);
            if (mapping != null && (Arrays.asList(mapping.path()).contains("/api/v2/sesiones")
                    || Arrays.asList(mapping.value()).contains("/api/v2/sesiones"))) {
                return instantiate(type);
            }
        }
        return fail("RED (UTC-D01): ningún @RestController mapea /api/v2/sesiones");
    }

    private Object instantiate(final Class<?> type) {
        for (Constructor<?> constructor : type.getConstructors()) {
            final Object[] args = Arrays.stream(constructor.getParameterTypes()).map(collaborators::get).toArray();
            if (Arrays.stream(args).allMatch(java.util.Objects::nonNull)) {
                try {
                    return constructor.newInstance(args);
                } catch (ReflectiveOperationException exception) {
                    throw new AssertionError("No fue posible construir " + type.getName(), exception);
                }
            }
        }
        return fail(type.getName() + " debe depender solo de los input ports de sesión y AuthenticatedUserResolver");
    }

    private static Class<?> load(final String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(exception);
        }
    }

    private static JsonMapper productionJsonMapper() {
        final JsonMapper.Builder builder = JsonMapper.builder();
        try (var context = new AnnotationConfigApplicationContext(JacksonInputConfig.class)) {
            context.getBean(JsonMapperBuilderCustomizer.class).customize(builder);
        }
        final JsonMapper mapper = builder.build();
        assertNotNull(mapper);
        return mapper;
    }
}
