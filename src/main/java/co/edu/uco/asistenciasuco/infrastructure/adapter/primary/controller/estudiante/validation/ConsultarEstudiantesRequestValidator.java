package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.request.ConsultarEstudiantesRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpValidationMessages;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;

import java.util.UUID;

public final class ConsultarEstudiantesRequestValidator implements Validator<ConsultarEstudiantesRequest> {

    private static final int MAX_TEXT_LENGTH = 100;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public ValidationResult validate(final ConsultarEstudiantesRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "Los filtros de estudiantes son obligatorios.").build();
        }
        UuidFieldValidationHelper.validateOptionalUuid(builder, "tipoIdentificacionId", request.getTipoIdentificacionId(), HttpValidationMessages.INVALID_UUID);
        UuidFieldValidationHelper.validateOptionalUuid(builder, "institucionId", request.getInstitucionId(), HttpValidationMessages.INVALID_UUID);
        UuidFieldValidationHelper.validateOptionalUuid(builder, "facultadId", request.getFacultadId(), HttpValidationMessages.INVALID_UUID);
        UuidFieldValidationHelper.validateOptionalUuid(builder, "programaId", request.getProgramaId(), HttpValidationMessages.INVALID_UUID);
        UuidFieldValidationHelper.validateOptionalUuid(builder, "grupoId", request.getGrupoId(), HttpValidationMessages.INVALID_UUID);
        validateNumeroIdentificacion(builder, request.getNumeroIdentificacion());
        validateNombre(builder, request.getNombre());
        validateCorreo(builder, request.getCorreo());
        validatePage(builder, request.getPage());
        validateSize(builder, request.getSize());
        validateOffset(builder, request.getPage(), request.getSize());
        return builder.build();
    }

    public ValidationResult validateEstudianteId(final UUID estudianteId) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        UuidFieldValidationHelper.validateRequiredUuid(
                builder,
                "estudianteId",
                estudianteId,
                HttpValidationMessages.REQUIRED,
                HttpValidationMessages.INVALID_UUID
        );
        return builder.build();
    }

    private static void validateNumeroIdentificacion(final ValidationResultBuilder builder, final Integer value) {
        builder.addIf(
                value != null && !ValidationHelper.isPositive(value),
                "numeroIdentificacion",
                ValidationErrorType.OUT_OF_RANGE,
                HttpValidationMessages.INVALID_IDENTIFICATION_NUMBER
        );
    }

    private static void validateNombre(final ValidationResultBuilder builder, final String value) {
        if (!ValidationHelper.hasText(value)) {
            return;
        }
        final String normalized = ValidationHelper.normalizeSpaces(value);
        builder.addIf(
                !ValidationHelper.isLengthAtMost(normalized, MAX_TEXT_LENGTH),
                "nombre",
                ValidationErrorType.INVALID_LENGTH,
                "El nombre puede tener maximo 100 caracteres."
        );
        builder.addIf(
                !ValidationHelper.isValidPersonName(normalized),
                "nombre",
                ValidationErrorType.INVALID_FORMAT,
                HttpValidationMessages.INVALID_PERSON_NAME
        );
    }

    private static void validateCorreo(final ValidationResultBuilder builder, final String value) {
        if (!ValidationHelper.hasText(value)) {
            return;
        }
        final String normalized = value.trim();
        builder.addIf(
                !ValidationHelper.isLengthAtMost(normalized, MAX_TEXT_LENGTH),
                "correo",
                ValidationErrorType.INVALID_LENGTH,
                "El correo puede tener maximo 100 caracteres."
        );
        builder.addIf(
                !ValidationHelper.isValidEmail(normalized),
                "correo",
                ValidationErrorType.INVALID_FORMAT,
                HttpValidationMessages.INVALID_EMAIL
        );
    }

    private static void validatePage(final ValidationResultBuilder builder, final Integer value) {
        builder.addIf(
                value != null && value < 0,
                "page",
                ValidationErrorType.OUT_OF_RANGE,
                HttpValidationMessages.INVALID_PAGE
        );
    }

    /**
     * Both parameters can be valid independently but exceed the JPA int offset
     * when multiplied. The repository repeats this invariant for non-HTTP callers.
     */
    private static void validateOffset(
            final ValidationResultBuilder builder,
            final Integer page,
            final Integer size
    ) {
        if (page != null && size != null && page >= 0
                && size >= MIN_PAGE_SIZE && size <= MAX_PAGE_SIZE) {
            builder.addIf((long) page * size > Integer.MAX_VALUE,
                    "page",
                    ValidationErrorType.OUT_OF_RANGE,
                    HttpValidationMessages.INVALID_PAGE);
        }
    }

    private static void validateSize(final ValidationResultBuilder builder, final Integer value) {
        builder.addIf(
                value != null && (value < MIN_PAGE_SIZE || value > MAX_PAGE_SIZE),
                "size",
                ValidationErrorType.OUT_OF_RANGE,
                HttpValidationMessages.INVALID_PAGE_SIZE
        );
    }
}
