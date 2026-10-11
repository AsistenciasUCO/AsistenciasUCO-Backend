package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.TechnicalException;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationIssue;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditRequestAttributes;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestControllerAdvice
public final class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ResolverMensajeUsuarioInputPort resolverMensajeUsuario;

    public GlobalExceptionHandler(final ResolverMensajeUsuarioInputPort resolverMensajeUsuario) {
        this.resolverMensajeUsuario = Objects.requireNonNull(resolverMensajeUsuario, "El puerto ResolverMensajeUsuarioInputPort es obligatorio.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            final HttpMessageNotReadableException exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.invalidRequest();
        final Optional<ResolvedInputError> resolvedError = JacksonInputErrorResolver.resolve(exception);
        logFrameworkBadRequest(descriptor, exception, request, resolvedError.map(ResolvedInputError::field).stream().toList());
        return resolvedError
                .map(error -> buildResponse(descriptor, request, List.of(toFieldError(error))))
                .orElseGet(() -> buildResponse(descriptor, request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            final MethodArgumentTypeMismatchException exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.invalidRequest();
        logFrameworkBadRequest(descriptor, exception, request, List.of(exception.getName()));
        return buildResponse(descriptor, request, resolveTypeMismatchFieldError(exception.getName(), exception.getRequiredType()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestParameter(
            final MissingServletRequestParameterException exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.requestValidationError();
        final String field = exception.getParameterName();
        logFrameworkBadRequest(descriptor, exception, request, List.of(field));
        return buildResponse(descriptor, request, List.of(new ApiFieldError(
                field,
                ApiFieldErrorCode.FIELD_REQUIRED.name(),
                "El parametro '" + field + "' es obligatorio."
        )));
    }

    @ExceptionHandler({
            TypeMismatchException.class,
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            BindException.class
    })
    public ResponseEntity<ApiErrorResponse> handleFrameworkBadRequest(
            final Exception exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.invalidRequest();
        logFrameworkBadRequest(descriptor, exception, request);
        return buildResponse(descriptor, request);
    }

    @ExceptionHandler(RequestValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestValidation(
            final RequestValidationException exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.requestValidationError();
        logRequestValidation(descriptor, exception, request);
        return buildResponse(
                descriptor,
                request,
                exception.getValidationResult().issues().stream().map(ApiFieldErrorMapper::from).toList()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            final HttpRequestMethodNotSupportedException exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.methodNotAllowed();
        logFrameworkBadRequest(descriptor, exception, request);
        final ResponseEntity<ApiErrorResponse> response = buildResponse(descriptor, request);
        if (exception.getSupportedHttpMethods() == null || exception.getSupportedHttpMethods().isEmpty()) {
            return response;
        }
        return ResponseEntity.status(response.getStatusCode())
                .allow(exception.getSupportedHttpMethods().toArray(new HttpMethod[0]))
                .body(response.getBody());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            final NoResourceFoundException exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.resourceNotFound();
        logFrameworkNotFound(descriptor, exception, request);
        return buildResponse(descriptor, request);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplication(
            final ApplicationException exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.fromApplicationException(exception);
        if (descriptor.status().is5xxServerError()) {
            logInternal(descriptor, exception, request);
        } else {
            logControlled(descriptor, exception, request);
        }
        return buildResponse(descriptor, request);
    }

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ApiErrorResponse> handleTechnical(
            final TechnicalException exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.fromTechnicalException(exception);
        logInternal(descriptor, exception, request);
        return buildResponse(descriptor, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            final Exception exception,
            final HttpServletRequest request
    ) {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.internalError();
        logInternal(descriptor, exception, request);
        return buildResponse(descriptor, request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(final ApiErrorDescriptor descriptor, final HttpServletRequest request) {
        return buildResponse(descriptor, request, List.of());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            final ApiErrorDescriptor descriptor,
            final HttpServletRequest request,
            final List<ApiFieldError> details
    ) {
        AuditRequestAttributes.storeErrorCode(request, descriptor.code());
        final String resolvedMessage = resolveErrorMessage(descriptor);
        return ResponseEntity.status(descriptor.status()).body(new ApiErrorResponse(
                OffsetDateTime.now(),
                descriptor.status().value(),
                descriptor.status().getReasonPhrase(),
                descriptor.code(),
                resolvedMessage,
                safePath(request),
                CorrelationIdContext.getAsString(),
                details == null ? List.of() : details
        ));
    }

    private String resolveErrorMessage(final ApiErrorDescriptor descriptor) {
        try {
            final Optional<String> catalogMessage = resolverMensajeUsuario.execute(descriptor.code());
            if (catalogMessage.isPresent() && !TextHelper.isNullOrBlank(catalogMessage.get())) {
                return catalogMessage.get();
            }
        } catch (final RuntimeException e) {
            LOGGER.debug("No se pudo resolver mensaje de usuario para codigo: {}", descriptor.code());
        }
        return TextHelper.isNullOrBlank(descriptor.message()) ? descriptor.status().getReasonPhrase() : descriptor.message();
    }

    private void logControlled(
            final ApiErrorDescriptor descriptor,
            final Exception exception,
            final HttpServletRequest request
    ) {
        LOGGER.warn(
                "Handled application exception. status={}, code={}, path={}, correlationId={}",
                descriptor.status().value(),
                descriptor.code(),
                safePath(request),
                CorrelationIdContext.getAsString()
        );
    }

    private void logRequestValidation(
            final ApiErrorDescriptor descriptor,
            final RequestValidationException exception,
            final HttpServletRequest request
    ) {
        final List<String> validationFields = exception.getValidationResult()
                .issues()
                .stream()
                .map(ValidationIssue::field)
                .distinct()
                .sorted()
                .toList();
        LOGGER.warn(
                "Handled request validation. status={}, code={}, path={}, correlationId={}, validationFields={}",
                descriptor.status().value(),
                descriptor.code(),
                safePath(request),
                CorrelationIdContext.getAsString(),
                validationFields
        );
    }

    private void logFrameworkBadRequest(
            final ApiErrorDescriptor descriptor,
            final Exception exception,
            final HttpServletRequest request
    ) {
        logFrameworkBadRequest(descriptor, exception, request, List.of());
    }

    private void logFrameworkBadRequest(
            final ApiErrorDescriptor descriptor,
            final Exception exception,
            final HttpServletRequest request,
            final List<String> validationFields
    ) {
        if (validationFields != null && !validationFields.isEmpty()) {
            LOGGER.warn(
                    "Handled invalid request. status={}, code={}, path={}, correlationId={}, validationFields={}",
                    descriptor.status().value(),
                    descriptor.code(),
                    safePath(request),
                    CorrelationIdContext.getAsString(),
                    validationFields
            );
            return;
        }
        LOGGER.warn(
                "Handled invalid request. status={}, code={}, path={}, correlationId={}",
                descriptor.status().value(),
                descriptor.code(),
                safePath(request),
                CorrelationIdContext.getAsString()
        );
    }

    private List<ApiFieldError> resolveTypeMismatchFieldError(final String field, final Class<?> requiredType) {
        if (requiredType == null) {
            return List.of();
        }
        if (UUID.class.equals(requiredType)) {
            return List.of(new ApiFieldError(
                    field,
                    ApiFieldErrorCode.FIELD_INVALID_UUID.name(),
                    "El identificador '" + field + "' debe tener un formato UUID valido."
            ));
        }
        if (Integer.class.equals(requiredType) || int.class.equals(requiredType)
                || Long.class.equals(requiredType) || long.class.equals(requiredType)) {
            return List.of(new ApiFieldError(
                    field,
                    ApiFieldErrorCode.FIELD_INVALID_TYPE.name(),
                    "El campo '" + field + "' debe contener un valor numerico valido."
            ));
        }
        if (Boolean.class.equals(requiredType) || boolean.class.equals(requiredType)) {
            return List.of(new ApiFieldError(
                    field,
                    ApiFieldErrorCode.FIELD_INVALID_TYPE.name(),
                    "El campo '" + field + "' debe contener true o false."
            ));
        }
        return List.of();
    }

    private ApiFieldError toFieldError(final ResolvedInputError error) {
        return new ApiFieldError(error.field(), error.code().name(), error.message());
    }

    private void logFrameworkNotFound(
            final ApiErrorDescriptor descriptor,
            final Exception exception,
            final HttpServletRequest request
    ) {
        LOGGER.warn(
                "Handled resource not found. status={}, code={}, path={}, correlationId={}",
                descriptor.status().value(),
                descriptor.code(),
                safePath(request),
                CorrelationIdContext.getAsString()
        );
    }

    private void logInternal(
            final ApiErrorDescriptor descriptor,
            final Exception exception,
            final HttpServletRequest request
    ) {
        LOGGER.error(
                "Handled internal exception. status={}, code={}, path={}, correlationId={}, stackTrace={}",
                descriptor.status().value(),
                descriptor.code(),
                safePath(request),
                CorrelationIdContext.getAsString(),
                safeStackTrace(exception)
        );
    }

    private String safePath(final HttpServletRequest request) {
        return SensitiveDataSanitizer.sanitizeForLog(request.getRequestURI(), 240);
    }

    private String safeStackTrace(final Exception exception) {
        final StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return SensitiveDataSanitizer.sanitizeForLog(writer.toString(), 8_000);
    }
}
