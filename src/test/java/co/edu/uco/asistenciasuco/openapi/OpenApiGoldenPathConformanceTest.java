package co.edu.uco.asistenciasuco.openapi;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.ConsultarAsistenciasPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.AsistenciaConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.RegistrarAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.RegistrarAsistenciasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.SolicitarRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto.HorarioDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.ActualizarGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.ConsultarGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.ConsultarEstudiantesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.EstudianteGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.CrearGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.ActualizarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.ConsultarSesionesPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.GenerarSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.AsistenciaController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.AsistenciaQueryController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciasSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistroAsistenciaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.DocentePortalController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.EstudianteController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.GrupoController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.SesionController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.ActualizarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CrearSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2.SesionV2ConsultaController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2.SesionV2Controller;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.controller.RealtimeEventsController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response.RealtimeEventResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

class OpenApiGoldenPathConformanceTest {

    private static final UUID ACTOR = UUID.fromString("4e9739aa-83ca-460e-bc77-9572bab52fab");
    private static final UUID GROUP = UUID.fromString("e38345e1-8b26-45ae-9ee0-07cf7ee535ad");
    private static final UUID SESSION = UUID.fromString("94ea3dc1-847a-4d4f-9222-d8bba0c6427c");
    private static final UUID STUDENT = UUID.fromString("3403376f-88de-44ee-bad3-bae038f49831");
    private static final String LOCAL_SESSION_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?$";
    private static final Set<OperationKey> GOLDEN_PATH_OPERATIONS = Set.of(
            new OperationKey("/api/v1/docente/horarios", HttpMethod.GET),
            new OperationKey("/api/v1/sesiones/grupo/{grupoId}", HttpMethod.GET),
            new OperationKey("/api/v1/grupos/{grupoId}/estudiantes", HttpMethod.GET),
            new OperationKey("/api/v1/grupos/{grupoId}/asistencias", HttpMethod.GET),
            new OperationKey("/api/v1/asistencias/lote", HttpMethod.POST),
            new OperationKey("/api/v1/realtime/stream", HttpMethod.GET),
            new OperationKey("/api/v1/sesiones", HttpMethod.POST),
            new OperationKey("/api/v1/sesiones/{sesionId}", HttpMethod.PATCH),
            new OperationKey("/api/v1/sesiones/{sesionId}", HttpMethod.PUT)
    );
    private static final OpenAPI OPEN_API = loadOpenApi();

    @Test
    void canonicalServerIsRelativeAndPortable() {
        assertNotNull(OPEN_API.getServers());
        assertEquals(1, OPEN_API.getServers().size(), "El contrato debe declarar un solo server portable");
        assertEquals("/", OPEN_API.getServers().getFirst().getUrl(),
                "El server canónico no debe congelar localhost ni un dominio de despliegue");
        assertEquals("Servidor que publica esta especificación", OPEN_API.getServers().getFirst().getDescription());
    }

    @Test
    void specContainsExactlyTheFrozenControllerMappings() throws ReflectiveOperationException {
        final Set<OperationKey> controllerMappings = new java.util.LinkedHashSet<>();
        addMapping(controllerMappings, DocentePortalController.class, "consultarHorarios");
        addMapping(controllerMappings, SesionController.class, "consultarSesionesPorGrupo", UUID.class);
        addMapping(controllerMappings, GrupoController.class, "listarEstudiantesGrupo", UUID.class);
        addMapping(controllerMappings, AsistenciaQueryController.class, "consultarAsistenciasPorGrupo", UUID.class, UUID.class);
        addMapping(controllerMappings, AsistenciaController.class, "registrarAsistenciasLote", RegistrarAsistenciasSesionRequest.class);
        addMapping(controllerMappings, RealtimeEventsController.class, "subscribe", UUID.class);
        addMapping(controllerMappings, SesionController.class, "crearSesion", CrearSesionRequest.class);
        addMapping(controllerMappings, SesionController.class, "actualizarSesion", UUID.class, ActualizarSesionRequest.class);
        addMapping(controllerMappings, SesionController.class, "actualizarSesionLegacy", UUID.class, ActualizarSesionRequest.class);
        // MAINT-003F: contrato 1.1.0 agrega UTC v2 de sesiones (UTC-D01: POST, PATCH, GET id, GET grupo; sin PUT).
        addMapping(controllerMappings, SesionV2Controller.class, "crearSesion", Map.class);
        addMapping(controllerMappings, SesionV2Controller.class, "actualizarSesion", UUID.class, Map.class);
        addMapping(controllerMappings, SesionV2ConsultaController.class, "consultarSesion", UUID.class);
        addMapping(controllerMappings, SesionV2ConsultaController.class, "consultarSesionesPorGrupo", UUID.class);
        // MAINT-001 contract-first: directorio paginado publicado fuera del set Golden Path.
        addMapping(controllerMappings, EstudianteController.class, "consultarEstudiantes",
                UUID.class, Integer.class, String.class, String.class, UUID.class, UUID.class, UUID.class,
                UUID.class, Boolean.class, Integer.class, Integer.class);

        final Set<OperationKey> specificationMappings = new java.util.LinkedHashSet<>();
        OPEN_API.getPaths().forEach((path, item) -> item.readOperationsMap()
                .forEach((method, operation) -> specificationMappings.add(
                        new OperationKey(path, HttpMethod.valueOf(method.name())))));

        assertEquals(controllerMappings, specificationMappings);
        assertEquals(14, specificationMappings.size());
        assertEquals(Map.of(HttpMethod.GET, 8L, HttpMethod.POST, 3L, HttpMethod.PUT, 1L, HttpMethod.PATCH, 2L),
                specificationMappings.stream().collect(java.util.stream.Collectors.groupingBy(
                        OperationKey::method, java.util.stream.Collectors.counting())));
    }

    @Test
    void sessionUpdateExposesPatchAsTargetAndPutAsDeprecatedLegacyWithSameContract() {
        final Operation patch = operation(new OperationKey("/api/v1/sesiones/{sesionId}", HttpMethod.PATCH));
        final Operation put = operation(new OperationKey("/api/v1/sesiones/{sesionId}", HttpMethod.PUT));
        assertEquals("actualizarSesion", patch.getOperationId());
        assertEquals("actualizarSesionLegacy", put.getOperationId());
        assertTrue(!Boolean.TRUE.equals(patch.getDeprecated()), "PATCH no debe estar deprecated");
        assertEquals(Boolean.TRUE, put.getDeprecated(), "PUT legacy debe estar deprecated");
        assertEquals(put.getRequestBody().getContent().get("application/json").getSchema().get$ref(),
                patch.getRequestBody().getContent().get("application/json").getSchema().get$ref());
        assertNull(patch.getRequestBody().getContent().get("application/json-patch+json"));
        assertEquals(put.getResponses().keySet(), patch.getResponses().keySet());
    }

    @Test
    void successStatusesAndMediaTypesMatchTheRunningControllerContracts() {
        final Map<OperationKey, Integer> actualStatuses = invokeGoldenPathSuccesses();
        final Map<OperationKey, String> expectedResponseMedia = Map.of(
                new OperationKey("/api/v1/realtime/stream", HttpMethod.GET), "text/event-stream"
        );
        final Set<OperationKey> jsonRequests = Set.of(
                new OperationKey("/api/v1/asistencias/lote", HttpMethod.POST),
                new OperationKey("/api/v1/sesiones", HttpMethod.POST),
                new OperationKey("/api/v1/sesiones/{sesionId}", HttpMethod.PATCH),
                new OperationKey("/api/v1/sesiones/{sesionId}", HttpMethod.PUT)
        );

        actualStatuses.forEach((key, status) -> {
            final Operation operation = operation(key);
            assertNotNull(operation.getResponses().get(Integer.toString(status)),
                    () -> key + " no declara su success status real " + status);
            assertEquals(1L, operation.getResponses().keySet().stream().filter(code -> code.startsWith("2")).count(),
                    () -> key + " debe congelar un único success status observado");

            final String media = expectedResponseMedia.getOrDefault(key, "application/json");
            assertNotNull(operation.getResponses().get(Integer.toString(status)).getContent().get(media),
                    () -> key + " no declara response media type " + media);

            if (jsonRequests.contains(key)) {
                assertNotNull(operation.getRequestBody());
                assertNotNull(operation.getRequestBody().getContent().get("application/json"),
                        () -> key + " no declara request application/json");
            } else {
                assertEquals(null, operation.getRequestBody(), () -> key + " no debe inventar requestBody");
            }
        });
    }

    @Test
    void criticalSchemasDeclareExactlyTheFrozenPropertiesAndRequiredFields() {
        assertObjectSchema("HorarioDocente",
                Set.of("id", "idDocente", "idGrupo", "codigoMateria", "nombreMateria", "seccion", "dia",
                        "horaInicio", "horaFin", "totalEstudiantes"),
                Set.of("id", "idDocente", "idGrupo", "codigoMateria", "nombreMateria", "seccion", "dia",
                        "horaInicio", "horaFin", "totalEstudiantes"));
        assertObjectSchema("Sesion",
                Set.of("sesion", "grupo", "nombre", "numero", "codigo", "numeroSemana", "codigoGrupo",
                        "nombreGrupo", "fechaHoraInicio", "fechaHoraFin"),
                Set.of("sesion", "grupo", "nombre", "numero", "codigo", "numeroSemana", "codigoGrupo",
                        "nombreGrupo", "fechaHoraInicio", "fechaHoraFin"));
        assertObjectSchema("EstudianteGrupo",
                Set.of("id", "idEstudiante", "documento", "nombreCompleto", "correo", "codigoEstado", "nombreEstado"),
                Set.of("id", "idEstudiante", "documento", "nombreCompleto", "correo", "codigoEstado", "nombreEstado"));
        assertObjectSchema("Asistencia",
                Set.of("asistencia", "estudiante", "grupo", "sesion", "presente", "estado", "observacion"),
                Set.of("asistencia", "estudiante", "grupo", "sesion", "presente", "estado"));
        assertObjectSchema("RegistrarAsistenciasSesionRequest", Set.of("sesionId", "registros"),
                Set.of("sesionId", "registros"));
        assertObjectSchema("RegistroAsistenciaRequest", Set.of("estudianteId", "estado"),
                Set.of("estudianteId", "estado"));
        assertObjectSchema("CrearSesionRequest", Set.of("grupo", "nombre", "fechaHoraInicio", "fechaHoraFin"),
                Set.of("grupo", "nombre", "fechaHoraInicio", "fechaHoraFin"));
        assertObjectSchema("ActualizarSesionRequest", Set.of("nombre", "fechaHoraInicio", "fechaHoraFin"),
                Set.of("nombre", "fechaHoraInicio", "fechaHoraFin"));
        assertObjectSchema("RealtimeEventResponse", Set.of("eventId", "type", "occurredAt", "correlationId", "payload"),
                Set.of("eventId", "type", "occurredAt", "payload"));
        assertObjectSchema("AsistenciasSesionActualizadasPayload", Set.of("grupo", "sesion", "totalRegistros"),
                Set.of("grupo", "sesion", "totalRegistros"));
        assertObjectSchema("ApiMessageResponse", Set.of("exitoso", "mensaje"), Set.of("exitoso", "mensaje"));
        assertObjectSchema("ApiVoidResponse", Set.of("exitoso", "datos"), Set.of("exitoso", "datos"));
        assertObjectSchema("HorarioDocenteListResponse", Set.of("exitoso", "datos", "total"),
                Set.of("exitoso", "datos", "total"));
        assertObjectSchema("SesionListResponse", Set.of("exitoso", "datos", "total"),
                Set.of("exitoso", "datos", "total"));
        assertObjectSchema("EstudianteGrupoListResponse", Set.of("exitoso", "datos", "total"),
                Set.of("exitoso", "datos", "total"));
        assertObjectSchema("AsistenciaListResponse", Set.of("exitoso", "datos", "total"),
                Set.of("exitoso", "datos", "total"));
        assertObjectSchema("ApiFieldError", Set.of("field", "code", "message"), Set.of("field", "code", "message"));
        assertObjectSchema("ApiErrorResponse",
                Set.of("timestamp", "status", "error", "code", "message", "path", "correlationId", "details"),
                Set.of("timestamp", "status", "error", "code", "message", "path", "correlationId"));
    }

    @Test
    void criticalOpenApiSchemasMatchProviderDtoPropertyShapes() {
        final Map<String, Class<?>> providerDtos = new LinkedHashMap<>();
        providerDtos.put("HorarioDocente", HorarioDocenteDTO.class);
        providerDtos.put("Sesion", SesionConsultadaDTO.class);
        providerDtos.put("EstudianteGrupo", EstudianteGrupoDTO.class);
        providerDtos.put("Asistencia", AsistenciaConsultadaDTO.class);
        providerDtos.put("RegistrarAsistenciasSesionRequest", RegistrarAsistenciasSesionRequest.class);
        providerDtos.put("RegistroAsistenciaRequest", RegistroAsistenciaRequest.class);
        providerDtos.put("CrearSesionRequest", CrearSesionRequest.class);
        providerDtos.put("ActualizarSesionRequest", ActualizarSesionRequest.class);
        providerDtos.put("RealtimeEventResponse", RealtimeEventResponse.class);

        assertEquals(9, providerDtos.size(), "El gate debe cubrir los nueve DTOs críticos autorizados");
        providerDtos.forEach((schemaName, dtoType) -> {
            final Schema<?> openApiSchema = schema(schemaName);
            assertNotNull(openApiSchema.getProperties(), () -> schemaName + " debe declarar properties");
            assertEquals(properties(dtoType), openApiSchema.getProperties().keySet(),
                    () -> "Property shape drift entre " + dtoType.getSimpleName() + " y " + schemaName);
        });
    }

    @Test
    void criticalSchemasDeclareFrozenTypesUuidFormatsAndConstraints() {
        assertUuidProperties("HorarioDocente", "id", "idDocente", "idGrupo");
        assertUuidProperties("Sesion", "sesion", "grupo");
        assertUuidProperties("EstudianteGrupo", "id", "idEstudiante");
        assertUuidProperties("Asistencia", "asistencia", "estudiante", "grupo", "sesion");
        assertUuidProperties("RegistrarAsistenciasSesionRequest", "sesionId");
        assertUuidProperties("RegistroAsistenciaRequest", "estudianteId");
        assertUuidProperties("CrearSesionRequest", "grupo");
        assertUuidProperties("RealtimeEventResponse", "eventId", "correlationId");
        assertUuidProperties("AsistenciasSesionActualizadasPayload", "grupo", "sesion");
        assertUuidProperties("ApiErrorResponse", "correlationId");

        assertPropertyType("HorarioDocente", "totalEstudiantes", "integer");
        assertPropertyType("Sesion", "numero", "integer");
        assertPropertyType("Sesion", "numeroSemana", "integer");
        assertPropertyType("Asistencia", "presente", "boolean");
        assertPropertyType("RegistrarAsistenciasSesionRequest", "registros", "array");
        assertPropertyType("AsistenciasSesionActualizadasPayload", "totalRegistros", "integer");
        assertPropertyType("ApiErrorResponse", "status", "integer");
        assertPropertyType("ApiErrorResponse", "details", "array");

        assertEquals(List.of("AN", "SJC", "EX"), schema("AttendanceStatus").getEnum());
        assertNameConstraints("Sesion");
        assertNameConstraints("CrearSesionRequest");
        assertNameConstraints("ActualizarSesionRequest");
        assertEquals(1, property("RegistrarAsistenciasSesionRequest", "registros").getMinItems());
        assertNotNull(property("RegistrarAsistenciasSesionRequest", "registros").getItems());

        final Set<String> forbiddenGhosts = Set.of(
                "tema", "topic", "descripcion", "aula", "room", "tipo", "status", "docente", "docenteName"
        );
        assertFalse(schema("Sesion").getProperties().keySet().stream().anyMatch(forbiddenGhosts::contains));
        assertFalse(schema("CrearSesionRequest").getProperties().keySet().stream().anyMatch(forbiddenGhosts::contains));
        assertFalse(schema("ActualizarSesionRequest").getProperties().keySet().stream().anyMatch(forbiddenGhosts::contains));
    }

    @Test
    void localSessionWireAndUtcInstantsUseDifferentOpenApiSemantics() {
        final Schema<?> local = schema("LocalSessionDateTime");
        assertSchemaType(local, "LocalSessionDateTime", "string");
        assertNull(local.getFormat(), "Un ISO local sin offset no es OpenAPI/RFC 3339 date-time");
        assertEquals(LOCAL_SESSION_PATTERN, local.getPattern());
        assertTrue(local.getDescription().contains("sin Z/offset"));
        assertEquals("ISO_LOCAL_DATE_TIME_WITHOUT_OFFSET", local.getExtensions().get("x-wire-format"));
        assertEquals("UTC", local.getExtensions().get("x-persistence-semantics"));
        assertEquals("2026-09-24T13:00:00", local.getExample());

        assertStringFormat("RealtimeEventResponse", "occurredAt", "date-time");
        assertStringFormat("ApiErrorResponse", "timestamp", "date-time");
    }

    @Test
    void allGoldenPathOperationsDeclareReusableBearerAndCorrelationContracts() {
        final SecurityScheme bearer = OPEN_API.getComponents().getSecuritySchemes().get("bearerAuth");
        assertNotNull(bearer, "Falta el security scheme reusable bearerAuth");
        assertEquals(SecurityScheme.Type.HTTP, bearer.getType());
        assertEquals("bearer", bearer.getScheme());
        assertEquals("JWT", bearer.getBearerFormat());

        assertNotNull(OPEN_API.getComponents().getParameters().get("CorrelationId"));
        assertEquals("X-Correlation-Id", OPEN_API.getComponents().getParameters().get("CorrelationId").getName());
        assertEquals("header", OPEN_API.getComponents().getParameters().get("CorrelationId").getIn());
        assertEquals(Boolean.FALSE, OPEN_API.getComponents().getParameters().get("CorrelationId").getRequired());
        assertEquals("uuid", OPEN_API.getComponents().getParameters().get("CorrelationId").getSchema().getFormat());
        assertNotNull(OPEN_API.getComponents().getHeaders().get("CorrelationId"));
        assertEquals("uuid", OPEN_API.getComponents().getHeaders().get("CorrelationId").getSchema().getFormat());
        for (String parameterName : List.of("GrupoIdPath", "SesionIdPath", "GrupoIdQuery", "SesionIdQuery")) {
            final Schema<?> parameterSchema = OPEN_API.getComponents().getParameters().get(parameterName).getSchema();
            assertSchemaType(parameterSchema, "components.parameters." + parameterName, "string");
            assertEquals("uuid", parameterSchema.getFormat(),
                    () -> "components.parameters." + parameterName + " debe usar format uuid");
        }
        assertTrue(OPEN_API.getComponents().getResponses().keySet()
                .containsAll(Set.of("BadRequest", "Unauthorized", "Forbidden", "NotFound", "InternalError")));

        assertEquals(GOLDEN_PATH_OPERATIONS, invokeGoldenPathSuccesses().keySet());
        invokeGoldenPathSuccesses().forEach((key, successStatus) -> {
            final Operation operation = operation(key);
            assertNotNull(operation.getSecurity(), () -> key + " no declara security");
            assertEquals(1, operation.getSecurity().size(), () -> key + " debe declarar un único requirement Bearer");
            assertTrue(operation.getSecurity().getFirst().containsKey("bearerAuth"),
                    () -> key + " no referencia bearerAuth");
            assertEquals(List.of(), operation.getSecurity().getFirst().get("bearerAuth"),
                    () -> key + " no debe inventar scopes OAuth");

            assertNotNull(operation.getParameters(), () -> key + " no declara parameters");
            assertTrue(operation.getParameters().stream().anyMatch(parameter ->
                            "#/components/parameters/CorrelationId".equals(parameter.get$ref())),
                    () -> key + " no declara el request parameter reusable CorrelationId");

            final var success = operation.getResponses().get(Integer.toString(successStatus));
            assertNotNull(success, () -> key + " no declara success " + successStatus);
            assertNotNull(success.getHeaders(), () -> key + " no declara headers de success");
            assertNotNull(success.getHeaders().get("X-Correlation-Id"),
                    () -> key + " no declara X-Correlation-Id en success");
            assertEquals("#/components/headers/CorrelationId",
                    success.getHeaders().get("X-Correlation-Id").get$ref(),
                    () -> key + " no reutiliza el header CorrelationId");
        });
    }

    private static Map<OperationKey, Integer> invokeGoldenPathSuccesses() {
        final AuthenticatedUserResolver identity = () -> ACTOR;

        final ConsultarHorariosDocenteInputPort schedules = mock(ConsultarHorariosDocenteInputPort.class);
        when(schedules.execute(ACTOR)).thenReturn(List.of());
        final DocentePortalController docente = new DocentePortalController(
                schedules,
                mock(ConsultarAsignaturasDocenteInputPort.class),
                mock(co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.ResolverSolicitudRevisionAsistenciaInputPort.class),
                mock(co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.ConsultarReclamosDocenteInputPort.class),
                identity
        );

        final ConsultarSesionesPorGrupoInputPort sessionsByGroup = mock(ConsultarSesionesPorGrupoInputPort.class);
        when(sessionsByGroup.execute(any())).thenReturn(List.of());
        final CrearSesionInputPort createSession = mock(CrearSesionInputPort.class);
        final ActualizarSesionInputPort updateSession = mock(ActualizarSesionInputPort.class);
        final SesionController sesion = new SesionController(
                createSession,
                mock(ConsultarSesionInputPort.class),
                sessionsByGroup,
                mock(CerrarSesionInputPort.class),
                updateSession,
                mock(GenerarSesionesGrupoInputPort.class),
                identity
        );

        final ConsultarEstudiantesGrupoInputPort students = mock(ConsultarEstudiantesGrupoInputPort.class);
        when(students.execute(any())).thenReturn(List.of());
        final GrupoController grupo = new GrupoController(
                mock(CrearGrupoInputPort.class),
                mock(ActualizarGrupoInputPort.class),
                mock(RegistrarEstudianteInputPort.class),
                mock(ConsultarGruposInputPort.class),
                students,
                identity
        );

        final ConsultarAsistenciasPorGrupoInputPort attendanceQuery = mock(ConsultarAsistenciasPorGrupoInputPort.class);
        when(attendanceQuery.execute(any())).thenReturn(List.of());
        final AsistenciaQueryController asistenciaQuery = new AsistenciaQueryController(attendanceQuery, identity);
        final AsistenciaController asistencia = new AsistenciaController(
                mock(RegistrarAsistenciaInputPort.class),
                mock(RegistrarAsistenciasSesionInputPort.class),
                mock(SolicitarRevisionAsistenciaInputPort.class),
                identity
        );

        final CrearSesionRequest createRequest = new CrearSesionRequest();
        createRequest.setGrupo(GROUP);
        createRequest.setNombre("Sesión de contrato");
        createRequest.setFechaHoraInicio("2026-09-24T13:00:00");
        createRequest.setFechaHoraFin("2026-09-24T15:00:00");
        final ActualizarSesionRequest updateRequest = new ActualizarSesionRequest();
        updateRequest.setNombre("Sesión actualizada");
        updateRequest.setFechaHoraInicio("2026-09-24T13:00:00");
        updateRequest.setFechaHoraFin("2026-09-24T15:30:00");
        final RegistroAsistenciaRequest record = new RegistroAsistenciaRequest();
        record.setEstudianteId(STUDENT);
        record.setEstado("AN");
        final RegistrarAsistenciasSesionRequest batchRequest = new RegistrarAsistenciasSesionRequest();
        batchRequest.setSesionId(SESSION);
        batchRequest.setRegistros(List.of(record));

        final Map<OperationKey, Integer> statuses = new LinkedHashMap<>();
        statuses.put(new OperationKey("/api/v1/docente/horarios", HttpMethod.GET), docente.consultarHorarios().getStatusCode().value());
        statuses.put(new OperationKey("/api/v1/sesiones/grupo/{grupoId}", HttpMethod.GET), sesion.consultarSesionesPorGrupo(GROUP).getStatusCode().value());
        statuses.put(new OperationKey("/api/v1/grupos/{grupoId}/estudiantes", HttpMethod.GET), grupo.listarEstudiantesGrupo(GROUP).getStatusCode().value());
        statuses.put(new OperationKey("/api/v1/grupos/{grupoId}/asistencias", HttpMethod.GET), asistenciaQuery.consultarAsistenciasPorGrupo(GROUP, SESSION).getStatusCode().value());
        statuses.put(new OperationKey("/api/v1/asistencias/lote", HttpMethod.POST), asistencia.registrarAsistenciasLote(batchRequest).getStatusCode().value());
        statuses.put(new OperationKey("/api/v1/realtime/stream", HttpMethod.GET), 200);
        statuses.put(new OperationKey("/api/v1/sesiones", HttpMethod.POST), sesion.crearSesion(createRequest).getStatusCode().value());
        statuses.put(new OperationKey("/api/v1/sesiones/{sesionId}", HttpMethod.PATCH), invokeUpdate(sesion, "actualizarSesion", updateRequest));
        statuses.put(new OperationKey("/api/v1/sesiones/{sesionId}", HttpMethod.PUT), invokeUpdate(sesion, "actualizarSesionLegacy", updateRequest));
        return statuses;
    }

    private static int invokeUpdate(final SesionController controller, final String methodName,
                                    final ActualizarSesionRequest request) {
        try {
            final Method method = SesionController.class.getDeclaredMethod(methodName, UUID.class, ActualizarSesionRequest.class);
            return ((org.springframework.http.ResponseEntity<?>) method.invoke(controller, SESSION, request))
                    .getStatusCode().value();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("El controller no expone " + methodName, exception);
        }
    }

    private static void addMapping(
            final Set<OperationKey> mappings,
            final Class<?> controller,
            final String methodName,
            final Class<?>... parameterTypes
    ) throws ReflectiveOperationException {
        final Method method = controller.getDeclaredMethod(methodName, parameterTypes);
        final RequestMapping classMapping = findMergedAnnotation(controller, RequestMapping.class);
        final RequestMapping methodMapping = findMergedAnnotation(method, RequestMapping.class);
        assertNotNull(methodMapping, () -> "Falta mapping en " + controller.getSimpleName() + "." + methodName);
        final String classPath = classMapping == null ? "" : firstPath(classMapping);
        final String methodPath = firstPath(methodMapping);
        mappings.add(new OperationKey(classPath + methodPath, HttpMethod.valueOf(methodMapping.method()[0].name())));
    }

    private static String firstPath(final RequestMapping mapping) {
        if (mapping.path().length > 0) {
            return mapping.path()[0];
        }
        if (mapping.value().length > 0) {
            return mapping.value()[0];
        }
        return "";
    }

    private static Operation operation(final OperationKey key) {
        final PathItem item = OPEN_API.getPaths().get(key.path());
        assertNotNull(item, () -> "Falta path " + key.path());
        final Operation operation = item.readOperationsMap().get(PathItem.HttpMethod.valueOf(key.method().name()));
        assertNotNull(operation, () -> "Falta operación " + key);
        return operation;
    }

    private static void assertObjectSchema(
            final String schemaName,
            final Set<String> expectedProperties,
            final Set<String> expectedRequired
    ) {
        final Schema<?> objectSchema = schema(schemaName);
        assertSchemaType(objectSchema, schemaName, "object");
        final Object additionalProperties = objectSchema.getAdditionalProperties();
        final boolean rejectsAdditionalProperties = Boolean.FALSE.equals(additionalProperties)
                || additionalProperties instanceof Schema<?> booleanSchema
                && Boolean.FALSE.equals(booleanSchema.getBooleanSchemaValue());
        assertTrue(rejectsAdditionalProperties,
                () -> schemaName + " debe rechazar ghost fields con additionalProperties=false");
        assertNotNull(objectSchema.getProperties(), () -> schemaName + " debe declarar properties");
        assertEquals(expectedProperties, objectSchema.getProperties().keySet(),
                () -> "Properties contractuales divergentes en " + schemaName);
        assertNotNull(objectSchema.getRequired(), () -> schemaName + " debe declarar required");
        assertEquals(expectedRequired, Set.copyOf(objectSchema.getRequired()),
                () -> "Required contractuales divergentes en " + schemaName);
        assertEquals(expectedRequired.size(), objectSchema.getRequired().size(),
                () -> schemaName + " no debe repetir required");
    }

    private static Set<String> properties(final Class<?> type) {
        if (type.isRecord()) {
            return Arrays.stream(type.getRecordComponents())
                    .map(component -> component.getName())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        try {
            return Arrays.stream(Introspector.getBeanInfo(type).getPropertyDescriptors())
                    .map(descriptor -> descriptor.getName())
                    .filter(name -> !"class".equals(name))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        } catch (IntrospectionException exception) {
            throw new AssertionError("No fue posible inspeccionar properties JavaBean de " + type.getName(), exception);
        }
    }

    private static void assertUuidProperties(final String schemaName, final String... propertyNames) {
        for (String propertyName : propertyNames) {
            assertStringFormat(schemaName, propertyName, "uuid");
        }
    }

    private static void assertStringFormat(final String schemaName, final String propertyName, final String format) {
        final Schema<?> property = property(schemaName, propertyName);
        assertSchemaType(property, schemaName + "." + propertyName, "string");
        assertEquals(format, property.getFormat(), () -> schemaName + "." + propertyName + " debe usar format " + format);
    }

    private static void assertPropertyType(final String schemaName, final String propertyName, final String type) {
        assertSchemaType(property(schemaName, propertyName), schemaName + "." + propertyName, type);
    }

    private static void assertNameConstraints(final String schemaName) {
        final Schema<?> name = property(schemaName, "nombre");
        assertSchemaType(name, schemaName + ".nombre", "string");
        assertEquals(1, name.getMinLength(), () -> schemaName + ".nombre debe tener minLength=1");
        assertEquals(50, name.getMaxLength(), () -> schemaName + ".nombre debe tener maxLength=50");
    }

    private static void assertSchemaType(final Schema<?> value, final String label, final String expectedType) {
        final Set<String> actualTypes = value.getTypes() != null
                ? value.getTypes()
                : value.getType() == null ? Set.of() : Set.of(value.getType());
        assertEquals(Set.of(expectedType), actualTypes, () -> label + " debe ser " + expectedType);
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

    private static OpenAPI loadOpenApi() {
        final Path spec = Path.of("docs", "contracts", "openapi", "openapi-golden-path.yaml").toAbsolutePath();
        final OpenAPI openAPI = new OpenAPIV3Parser().read(spec.toUri().toString());
        if (openAPI == null) {
            throw new IllegalStateException("No fue posible cargar " + spec);
        }
        return openAPI;
    }

    private record OperationKey(String path, HttpMethod method) {
    }
}
