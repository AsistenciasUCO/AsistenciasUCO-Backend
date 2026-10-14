package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditRequestAttributes;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.ApiErrorResponse;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public final class SecurityErrorResponseWriter {

    public void write(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final HttpStatus status,
            final ErrorDefinition errorDefinition
    ) throws IOException {
        AuditRequestAttributes.storeErrorCode(request, errorDefinition.code());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(toJson(new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                errorDefinition.code(),
                errorDefinition.defaultMessage(),
                safePath(request),
                CorrelationIdContext.getAsString(),
                List.of()
        )));
    }

    private String safePath(final HttpServletRequest request) {
        return SensitiveDataSanitizer.sanitizeForLog(request.getRequestURI(), 240);
    }

    private String toJson(final ApiErrorResponse error) {
        return """
                {"timestamp":"%s","status":%d,"error":"%s","code":"%s","message":"%s","path":"%s","correlationId":%s}\
                """.formatted(
                escape(error.timestamp().toString()),
                error.status(),
                escape(error.error()),
                escape(error.code()),
                escape(error.message()),
                escape(error.path()),
                nullableJsonString(error.correlationId())
        );
    }

    private String nullableJsonString(final String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private String escape(final String value) {
        if (value == null) {
            return "";
        }
        final StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append("\\u%04x".formatted((int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
