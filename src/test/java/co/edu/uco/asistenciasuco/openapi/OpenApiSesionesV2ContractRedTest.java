package co.edu.uco.asistenciasuco.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract-first RED for the UTC v2 write surface (MAINT-003B).
 *
 * <p>Only POST/PATCH are asserted: GET v2 depends on UTC-D06 (historical provenance), which is
 * pending, so no GET v2 expectation is encoded here. v1 must keep its frozen local wire.
 * Expected today: v2 tests fail because the canonical spec has no /api/v2 paths.
 */
class OpenApiSesionesV2ContractRedTest {

    private static final OpenAPI OPEN_API = new OpenAPIV3Parser().read(
            Path.of("docs", "contracts", "openapi", "openapi-golden-path.yaml").toAbsolutePath().toUri().toString());

    @Test
    void d01d09WriteSurfaceIsPostAndPatchOnlyWithoutPut() {
        final PathItem collection = OPEN_API.getPaths().get("/api/v2/sesiones");
        final PathItem item = OPEN_API.getPaths().get("/api/v2/sesiones/{sesionId}");
        assertNotNull(collection, "RED (UTC-D01): falta POST /api/v2/sesiones");
        assertNotNull(item, "RED (UTC-D01): falta PATCH /api/v2/sesiones/{sesionId}");
        assertNotNull(collection.getPost());
        assertNotNull(item.getPatch());
        assertNull(item.getPut(), "UTC-D09: PUT de compatibilidad solo existe en v1");
    }

    @Test
    void d02RequestInstantsAreRequiredRfc3339WithMandatoryOffset() {
        for (String schemaName : List.of("CrearSesionV2Request", "ActualizarSesionV2Request")) {
            final Schema<?> schema = schema(schemaName);
            assertTrue(schema.getRequired().containsAll(Set.of("fechaHoraInicio", "fechaHoraFin")));
            for (String field : List.of("fechaHoraInicio", "fechaHoraFin")) {
                final Schema<?> property = resolve(schema.getProperties().get(field));
                assertEquals("date-time", property.getFormat(), schemaName + "." + field);
                final Pattern pattern = Pattern.compile(property.getPattern());
                assertTrue(pattern.matcher("2026-07-15T16:00:00+02:00").matches());
                assertTrue(pattern.matcher("2026-07-15T14:00:00Z").matches());
                assertFalse(pattern.matcher("2026-07-15T16:00:00").matches(), "UTC-D02: naive rechazado");
            }
            assertFalse(schema.getProperties().containsKey("docente"), "UTC-D08: actor solo del JWT");
            assertFalse(schema.getProperties().containsKey("usuarioEjecutor"), "UTC-D08: actor solo del JWT");
        }
    }

    @Test
    void d08WritesStayDocenteOnlyWithBearerAndSharedErrors() {
        final PathItem collection = OPEN_API.getPaths().get("/api/v2/sesiones");
        final PathItem item = OPEN_API.getPaths().get("/api/v2/sesiones/{sesionId}");
        assertNotNull(collection, "RED (UTC-D01/D08): falta /api/v2/sesiones");
        assertNotNull(item, "RED (UTC-D01/D08): falta /api/v2/sesiones/{sesionId}");
        for (Operation operation : List.of(collection.getPost(), item.getPatch())) {
            assertEquals(List.of("DOCENTE"), operation.getExtensions().get("x-roles"));
            assertEquals(List.of(), operation.getSecurity().getFirst().get("bearerAuth"));
            assertTrue(operation.getResponses().keySet().containsAll(Set.of("400", "401", "403")));
        }
        assertTrue(collection.getPost().getResponses().containsKey("201"),
                "POST v2 mantiene 201 y el envelope de mensaje vigente");
    }

    @Test
    void d06V1KeepsFrozenLocalWire() {
        final Map<String, Schema> schemas = OPEN_API.getComponents().getSchemas();
        for (String v1 : List.of("CrearSesionRequest", "ActualizarSesionRequest", "Sesion")) {
            for (String field : List.of("fechaHoraInicio", "fechaHoraFin")) {
                // The parser may inline the $ref, so compare the frozen wire markers instead.
                final Schema<?> property = resolve(schemas.get(v1).getProperties().get(field));
                assertEquals("ISO_LOCAL_DATE_TIME_WITHOUT_OFFSET", property.getExtensions().get("x-wire-format"),
                        v1 + "." + field + " debe seguir usando el wire local v1");
                assertNull(property.getFormat(), v1 + "." + field + " no puede pasar a date-time");
            }
        }
    }

    private static Schema<?> schema(final String name) {
        final Schema<?> schema = OPEN_API.getComponents().getSchemas().get(name);
        assertNotNull(schema, "RED (UTC-D02): falta schema " + name);
        return schema;
    }

    private static Schema<?> resolve(final Object value) {
        final Schema<?> schema = (Schema<?>) value;
        if (schema.get$ref() == null) {
            return schema;
        }
        final String name = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
        return OPEN_API.getComponents().getSchemas().get(name);
    }
}
