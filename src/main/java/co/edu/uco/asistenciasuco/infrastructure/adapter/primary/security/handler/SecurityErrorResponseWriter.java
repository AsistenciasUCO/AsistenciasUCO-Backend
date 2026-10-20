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

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public final class SecurityErrorResponseWriter {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    void write(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final HttpStatus status,
            final ErrorDefinition errorDefinition
    ) throws IOException {
        AuditRequestAttributes.storeErrorCode(request, errorDefinition.code());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // Serializacion estandar de Jackson: escapa comillas, controles y caracteres especiales del path reflejado.
        JSON_MAPPER.writeValue(response.getOutputStream(), new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                errorDefinition.code(),
                errorDefinition.defaultMessage(),
                safePath(request),
                CorrelationIdContext.getAsString(),
                List.of()
        ));
    }

    private String safePath(final HttpServletRequest request) {
        return SensitiveDataSanitizer.sanitizeForLog(request.getRequestURI(), 240);
    }
}
