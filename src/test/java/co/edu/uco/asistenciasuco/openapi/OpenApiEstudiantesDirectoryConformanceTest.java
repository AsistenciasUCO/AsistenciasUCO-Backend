package co.edu.uco.asistenciasuco.openapi;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.ConsultarEstudiantePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.ConsultarEstudiantesInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudianteResumenDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.EstudianteController;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Contract-first gate for the paginated student directory GET /api/v1/estudiantes.
 * The expectations come from the AS-IS controller, the Application DTO records and the
 * HTTP behavior validated with real JWT (MAINT-001 VALIDATION), not from annotations.
 */
class OpenApiEstudiantesDirectoryConformanceTest {

    private static final String PATH = "/api/v1/estudiantes";
    private static final OpenAPI OPEN_API = loadOpenApi();

    @Test
    void directoryOperationIsPublishedWithStableIdRolesAndBearer() {
        final Operation operation = operation();
        assertEquals("consultarEstudiantes", operation.getOperationId());
        assertEquals(List.of("COORDINADOR", "ADMINISTRADOR"), operation.getExtensions().get("x-roles"),
                "Solo COORDINADOR y ADMINISTRADOR acceden al directorio general (SecurityConfig)");
        assertNotNull(operation.getSecurity());
        assertEquals(1, operation.getSecurity().size());
        assertEquals(List.of(), operation.getSecurity().getFirst().get("bearerAuth"));
        assertNull(operation.getRequestBody(), "Un GET de directorio no debe inventar requestBody");
        assertTrue(!Boolean.TRUE.equals(operation.getDeprecated()));
    }

    @Test
    void queryParametersMatchControllerRequestParamsExactly() throws NoSuchMethodException {
        final Map<String, Parameter> query = queryParameters();
        assertEquals(controllerRequestParams(), query.keySet(),
                "Los query params del contrato deben ser exactamente los @RequestParam del controller");
        query.values().forEach(parameter -> assertTrue(!Boolean.TRUE.equals(parameter.getRequired()),
                () -> parameter.getName() + " es opcional en el AS-IS"));
        assertTrue(operation().getParameters().stream()
                        .anyMatch(parameter -> "#/components/parameters/CorrelationId".equals(parameter.get$ref())),
                "Falta el header reusable CorrelationId");
    }

    @Test
    void queryParametersDeclareObservedTypesFormatsAndBounds() {
        final Map<String, Parameter> query = queryParameters();
        for (String uuidParam : List.of("tipoIdentificacionId", "institucionId", "facultadId", "programaId", "grupoId")) {
            assertType(query.get(uuidParam).getSchema(), uuidParam, "string");
            assertEquals("uuid", query.get(uuidParam).getSchema().getFormat(), uuidParam + " debe ser uuid");
        }
        final Schema<?> numero = query.get("numeroIdentificacion").getSchema();
        assertType(numero, "numeroIdentificacion", "integer");
        assertEquals(1, numero.getMinimum().intValue(), "numeroIdentificacion debe ser positivo");

        final Schema<?> nombre = query.get("nombre").getSchema();
        assertType(nombre, "nombre", "string");
        assertEquals(100, nombre.getMaxLength());
        final Schema<?> correo = query.get("correo").getSchema();
        assertType(correo, "correo", "string");
        assertEquals(100, correo.getMaxLength());

        assertType(query.get("activo").getSchema(), "activo", "boolean");

        final Schema<?> page = query.get("page").getSchema();
        assertType(page, "page", "integer");
        assertEquals(0, page.getMinimum().intValue(), "page es 0-based");
        assertEquals(0, ((Number) page.getDefault()).intValue());
        final Schema<?> size = query.get("size").getSchema();
        assertType(size, "size", "integer");
        assertEquals(1, size.getMinimum().intValue());
        assertEquals(100, size.getMaximum().intValue());
        assertEquals(20, ((Number) size.getDefault()).intValue());
        assertTrue(query.get("page").getDescription().contains("2147483647"),
                "page debe documentar el límite page*size <= 2147483647 que produce 400");
    }

    @Test
    void responsesAreExactlyTheObservedStatusesWithSharedErrorEnvelope() {
        final Operation operation = operation();
        assertEquals(Set.of("200", "400", "401", "403", "500"), operation.getResponses().keySet());
        assertEquals("#/components/responses/BadRequest", operation.getResponses().get("400").get$ref());
        assertEquals("#/components/responses/Unauthorized", operation.getResponses().get("401").get$ref());
        assertEquals("#/components/responses/Forbidden", operation.getResponses().get("403").get$ref());
        assertEquals("#/components/responses/InternalError", operation.getResponses().get("500").get$ref());

        final var success = operation.getResponses().get("200");
        assertEquals("#/components/headers/CorrelationId", success.getHeaders().get("X-Correlation-Id").get$ref());
        assertEquals("#/components/schemas/EstudiantePagina",
                success.getContent().get("application/json").getSchema().get$ref());
    }

    @Test
    void pageAndItemSchemasMatchApplicationRecordsAndDbNullability() {
        assertClosedObject("EstudiantePagina", recordComponents(EstudiantePaginaDTO.class));
        assertClosedObject("EstudianteResumen", recordComponents(EstudianteResumenDTO.class));

        final Schema<?> items = property("EstudiantePagina", "items");
        assertType(items, "EstudiantePagina.items", "array");
        // Swagger Parser 3.1 may inline nested array refs; accept the ref or the identical shape.
        final Schema<?> itemSchema = items.getItems();
        assertNotNull(itemSchema, "EstudiantePagina.items debe declarar items");
        if (itemSchema.get$ref() != null) {
            assertEquals("#/components/schemas/EstudianteResumen", itemSchema.get$ref());
        } else {
            assertEquals(schema("EstudianteResumen").getProperties().keySet(), itemSchema.getProperties().keySet(),
                    "EstudiantePagina.items debe tener la forma de EstudianteResumen");
        }
        assertType(property("EstudiantePagina", "totalItems"), "totalItems", "integer");
        assertEquals("int64", property("EstudiantePagina", "totalItems").getFormat());
        for (String counter : List.of("totalPages", "page", "size")) {
            assertType(property("EstudiantePagina", counter), counter, "integer");
            assertEquals(0, property("EstudiantePagina", counter).getMinimum().intValue());
        }

        for (String uuid : List.of("id", "idUsuario", "tipoIdentificacionId")) {
            assertType(property("EstudianteResumen", uuid), uuid, "string");
            assertEquals("uuid", property("EstudianteResumen", uuid).getFormat());
        }
        assertType(property("EstudianteResumen", "numeroIdentificacion"), "numeroIdentificacion", "integer");
        assertType(property("EstudianteResumen", "estaActivoUsuario"), "estaActivoUsuario", "boolean");
        for (String text : List.of("primerApellido", "segundoApellido", "primerNombre", "segundoNombre",
                "nombreCompleto", "correo")) {
            assertType(property("EstudianteResumen", text), text, "string");
        }
        assertFalse(schema("EstudianteResumen").getProperties().containsKey("password"),
                "El directorio no debe exponer columnas sensibles de uv_usuario");
    }

    @Test
    void controllerSuccessStatusIsTheDeclared200() {
        final ConsultarEstudiantesInputPort port = mock(ConsultarEstudiantesInputPort.class);
        when(port.execute(any())).thenReturn(new EstudiantePaginaDTO(List.of(), 0L, 0, 0, 20));
        final EstudianteController controller = new EstudianteController(port, mock(ConsultarEstudiantePorIdInputPort.class));
        final int status = controller.consultarEstudiantes(null, null, null, null, null, null, null, null, null, 0, 20)
                .getStatusCode().value();
        assertEquals(200, status);
        assertNotNull(operation().getResponses().get(Integer.toString(status)));
    }

    private static Set<String> controllerRequestParams() throws NoSuchMethodException {
        final Method method = EstudianteController.class.getDeclaredMethod("consultarEstudiantes",
                UUID.class, Integer.class, String.class, String.class, UUID.class, UUID.class, UUID.class,
                UUID.class, Boolean.class, Integer.class, Integer.class);
        return Arrays.stream(method.getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(RequestParam.class))
                .map(java.lang.reflect.Parameter::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Map<String, Parameter> queryParameters() {
        final Map<String, Parameter> query = new LinkedHashMap<>();
        operation().getParameters().stream()
                .filter(parameter -> parameter.get$ref() == null)
                .filter(parameter -> "query".equals(parameter.getIn()))
                .forEach(parameter -> query.put(parameter.getName(), parameter));
        return query;
    }

    private static Set<String> recordComponents(final Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static void assertClosedObject(final String name, final Set<String> expected) {
        final Schema<?> object = schema(name);
        assertType(object, name, "object");
        assertTrue(Boolean.FALSE.equals(object.getAdditionalProperties())
                        || object.getAdditionalProperties() instanceof Schema<?> booleanSchema
                        && Boolean.FALSE.equals(booleanSchema.getBooleanSchemaValue()),
                () -> name + " debe declarar additionalProperties=false");
        assertEquals(expected, object.getProperties().keySet(), () -> "Property drift en " + name);
        assertEquals(expected, Set.copyOf(object.getRequired()), () -> "Required drift en " + name);
    }

    private static void assertType(final Schema<?> value, final String label, final String expected) {
        assertNotNull(value, () -> "Falta schema de " + label);
        final Set<String> types = value.getTypes() != null
                ? value.getTypes()
                : value.getType() == null ? Set.of() : Set.of(value.getType());
        assertEquals(Set.of(expected), types, () -> label + " debe ser " + expected);
    }

    private static Schema<?> property(final String schemaName, final String propertyName) {
        final Schema<?> property = schema(schemaName).getProperties().get(propertyName);
        assertNotNull(property, () -> "Falta property " + schemaName + "." + propertyName);
        return property;
    }

    private static Schema<?> schema(final String name) {
        final Schema<?> schema = OPEN_API.getComponents().getSchemas().get(name);
        assertNotNull(schema, () -> "Falta schema " + name);
        return schema;
    }

    private static Operation operation() {
        final PathItem item = OPEN_API.getPaths().get(PATH);
        assertNotNull(item, "Falta path " + PATH + " en el OpenAPI canónico");
        assertNotNull(item.getGet(), "Falta GET " + PATH);
        return item.getGet();
    }

    private static OpenAPI loadOpenApi() {
        final Path spec = Path.of("docs", "contracts", "openapi", "openapi-golden-path.yaml").toAbsolutePath();
        final OpenAPI openAPI = new OpenAPIV3Parser().read(spec.toUri().toString());
        if (openAPI == null) {
            throw new IllegalStateException("No fue posible cargar " + spec);
        }
        return openAPI;
    }
}
