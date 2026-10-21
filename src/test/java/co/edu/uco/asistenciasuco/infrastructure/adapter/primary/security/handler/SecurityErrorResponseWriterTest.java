package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SecurityErrorResponseWriterTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter();

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    @Test
    void writesStableErrorStructureAsJson() throws Exception {
        final UUID correlation = UUID.randomUUID();
        CorrelationIdContext.set(correlation);

        final JsonNode body = writeFor("/api/v1/docente/reclamos", HttpStatus.UNAUTHORIZED);

        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asString()).isEqualTo("Unauthorized");
        assertThat(body.get("code").asString()).isEqualTo("SEC_TEST");
        assertThat(body.get("message").asString()).isEqualTo("Acceso \"restringido\"");
        assertThat(body.get("path").asString()).isEqualTo("/api/v1/docente/reclamos");
        assertThat(body.get("correlationId").asString()).isEqualTo(correlation.toString());
        assertThat(body.get("timestamp").asString()).isNotBlank();
        assertThat(body.has("details")).isFalse();
    }

    @Test
    void writesNullCorrelationAsJsonNull() throws Exception {
        final JsonNode body = writeFor("/api/v1/x", HttpStatus.FORBIDDEN);

        assertThat(body.has("correlationId")).isTrue();
        assertThat(body.get("correlationId").isNull()).isTrue();
    }

    static String[] maliciousPaths() {
        return new String[] {
                "/api/v1/<script>alert(1)</script>",
                "/api/v1/\"};alert(document.domain);//",
                "/api/v1/</script><img src=x onerror=alert(1)>",
                "/api/v1/a" + (char) 0x5C + "x\"c",
                "/api/v1/" + (char) 0x2028 + (char) 0x2029 + "unicode",
                "/api/v1/%3Cscript%3E%22"
        };
    }

    @ParameterizedTest
    @MethodSource("maliciousPaths")
    void reflectedPathNeverBreaksOutOfTheJsonDocument(final String maliciousPath) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", maliciousPath);
        request.setRequestURI(maliciousPath);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(request, response, HttpStatus.UNAUTHORIZED, definition());

        final String raw = response.getContentAsString(StandardCharsets.UTF_8);
        final JsonNode body = MAPPER.readTree(raw);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(body.propertyNames().stream().toList())
                .containsExactlyInAnyOrder("timestamp", "status", "error", "code", "message", "path", "correlationId");
        assertThat(body.get("path").asString()).isEqualTo(maliciousPath);
        assertThat(body.get("code").asString()).isEqualTo("SEC_TEST");
    }

    @Test
    void controlCharactersInThePathAreRemovedBeforeSerialization() throws Exception {
        final JsonNode body = writeFor("/api/v1/a\r\nSet-Cookie: x=1\u0000", HttpStatus.UNAUTHORIZED);

        assertThat(body.get("path").asString()).doesNotContain("\r").doesNotContain("\n").doesNotContain("\u0000");
    }

    @Test
    void longPathsAreTruncatedAndStayValidJson() throws Exception {
        final JsonNode body = writeFor("/" + "a".repeat(2_000), HttpStatus.UNAUTHORIZED);

        assertThat(body.get("path").asString().length()).isLessThanOrEqualTo(240);
    }

    private JsonNode writeFor(final String path, final HttpStatus status) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(request, response, status, definition());

        assertThat(response.getStatus()).isEqualTo(status.value());
        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        return MAPPER.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }

    private static ErrorDefinition definition() {
        return new ErrorDefinition() {
            @Override
            public String code() {
                return "SEC_TEST";
            }

            @Override
            public String defaultMessage() {
                return "Acceso \"restringido\"";
            }

            @Override
            public ErrorKind kind() {
                return null;
            }
        };
    }
}
