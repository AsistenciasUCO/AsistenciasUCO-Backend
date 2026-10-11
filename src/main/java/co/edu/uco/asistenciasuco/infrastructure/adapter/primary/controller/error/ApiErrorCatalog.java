package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.TechnicalException;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import org.springframework.http.HttpStatus;

final class ApiErrorCatalog {

    private ApiErrorCatalog() {
    }

    static ApiErrorDescriptor fromApplicationException(final ApplicationException exception) {
        return exception.getErrorDefinition()
                .map(ApiErrorCatalog::from)
                .orElseGet(() -> fromUnknownApplicationException(exception));
    }

    static ApiErrorDescriptor fromTechnicalException(final TechnicalException exception) {
        return exception.getErrorDefinition()
                .map(ApiErrorCatalog::from)
                .orElseGet(ApiErrorCatalog::internalError);
    }

    static ApiErrorDescriptor invalidRequest() {
        return from(CommonErrorCode.INVALID_REQUEST);
    }

    static ApiErrorDescriptor requestValidationError() {
        return from(CommonErrorCode.VALIDATION_ERROR);
    }

    static ApiErrorDescriptor resourceNotFound() {
        return from(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    static ApiErrorDescriptor internalError() {
        return from(CommonErrorCode.INTERNAL_ERROR);
    }

    /** Metodo HTTP no soportado por una ruta existente (p. ej. PUT en /api/v2/sesiones/{id}, UTC-D09). */
    static ApiErrorDescriptor methodNotAllowed() {
        return new ApiErrorDescriptor(
                "METHOD_NOT_ALLOWED",
                "El metodo HTTP no esta soportado para este recurso.",
                HttpStatus.METHOD_NOT_ALLOWED
        );
    }

    static ApiErrorDescriptor from(final ErrorDefinition errorDefinition) {
        return new ApiErrorDescriptor(errorDefinition.code(), errorDefinition.defaultMessage(), statusFor(errorDefinition.kind()));
    }

    private static ApiErrorDescriptor fromUnknownApplicationException(final ApplicationException exception) {
        final HttpStatus status = statusForExceptionType(exception);
        final String message = TextHelper.isNullOrBlank(exception.getMessage())
                ? CommonErrorCode.BUSINESS_ERROR.defaultMessage()
                : exception.getMessage();
        return new ApiErrorDescriptor(exception.getCode(), message, status);
    }

    private static HttpStatus statusForExceptionType(final ApplicationException exception) {
        if (exception instanceof ValidationException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (exception instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (exception instanceof ConflictException) {
            return HttpStatus.CONFLICT;
        }
        if (exception instanceof ForbiddenException) {
            return HttpStatus.FORBIDDEN;
        }
        if (exception instanceof FeatureUnavailableException) {
            return HttpStatus.NOT_IMPLEMENTED;
        }
        if (exception instanceof InternalApplicationException) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.UNPROCESSABLE_CONTENT;
    }

    private static HttpStatus statusFor(final ErrorKind kind) {
        return switch (kind) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case TECHNICAL -> HttpStatus.INTERNAL_SERVER_ERROR;
            case BUSINESS -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    }

}
