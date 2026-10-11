package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationIssue;

public final class ApiFieldErrorMapper {

    private ApiFieldErrorMapper() {
    }

    public static ApiFieldError from(final ValidationIssue issue) {
        return new ApiFieldError(issue.field(), toApiCode(issue.type()).name(), issue.message());
    }

    private static ApiFieldErrorCode toApiCode(final ValidationErrorType type) {
        return switch (type) {
            case REQUIRED -> ApiFieldErrorCode.FIELD_REQUIRED;
            case INVALID_FORMAT -> ApiFieldErrorCode.FIELD_INVALID_FORMAT;
            case INVALID_LENGTH -> ApiFieldErrorCode.FIELD_INVALID_LENGTH;
            case OUT_OF_RANGE -> ApiFieldErrorCode.FIELD_OUT_OF_RANGE;
            case INVALID_TYPE -> ApiFieldErrorCode.FIELD_INVALID_TYPE;
            case INVALID_UUID -> ApiFieldErrorCode.FIELD_INVALID_UUID;
            case INVALID_VALUE -> ApiFieldErrorCode.FIELD_INVALID_VALUE;
            case UNKNOWN_FIELD -> ApiFieldErrorCode.FIELD_UNKNOWN;
        };
    }
}
