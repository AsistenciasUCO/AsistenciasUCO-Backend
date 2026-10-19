package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.RegistrarEstudianteRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpValidationMessages;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;

import java.util.UUID;

public final class RegistrarEstudianteRequestValidator implements Validator<RegistrarEstudianteRequest> {

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MIN_IDENTIFICATION_DIGITS = 6;
    private static final int MAX_IDENTIFICATION_DIGITS = 10;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 255;
    private static final String REQUIRED_STUDENT_INFORMATION = "La informacion del estudiante es obligatoria.";

    @Override
    public ValidationResult validate(final RegistrarEstudianteRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, REQUIRED_STUDENT_INFORMATION).build();
        }

        if (request.getEstudianteId() != null) {
            return builder.build();
        }

        UuidFieldValidationHelper.validateRequiredUuid(
                builder,
                "tipoIdentificacionId",
                request.getTipoIdentificacionId(),
                "El tipo de identificacion es obligatorio.",
                HttpValidationMessages.INVALID_UUID
        );
        validateNumeroIdentificacion(request.getNumeroIdentificacion(), builder);

        final boolean esMatriculaPorIdentificacion = request.getPrimerNombre() == null
                && request.getPrimerApellido() == null
                && request.getCorreo() == null;

        if (esMatriculaPorIdentificacion) {
            return builder.build();
        }

        validateRequiredPersonName("primerApellido", request.getPrimerApellido(), builder);
        validateOptionalPersonName("segundoApellido", request.getSegundoApellido(), builder);
        validateRequiredPersonName("primerNombre", request.getPrimerNombre(), builder);
        validateOptionalPersonName("segundoNombre", request.getSegundoNombre(), builder);
        validateCorreo(request.getCorreo(), builder);
        validatePassword(request.getPassword(), builder);
        return builder.build();
    }

    public ValidationResult validateGrupoId(final UUID grupoId) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        UuidFieldValidationHelper.validateRequiredUuid(
                builder,
                "grupoId",
                grupoId,
                "El grupo es obligatorio.",
                HttpValidationMessages.INVALID_UUID
        );
        return builder.build();
    }

    public ValidationResult validate(final UUID grupoId, final RegistrarEstudianteRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        validateGrupoId(grupoId).issues().forEach(issue -> builder.add(issue.field(), issue.type(), issue.message()));
        validate(request).issues().forEach(issue -> builder.add(issue.field(), issue.type(), issue.message()));
        return builder.build();
    }

    private static void validateNumeroIdentificacion(final Integer value, final ValidationResultBuilder builder) {
        if (value == null) {
            builder.add("numeroIdentificacion", ValidationErrorType.REQUIRED, "El numero de identificacion es obligatorio.");
        } else if (!ValidationHelper.isPositive(value)) {
            builder.add(
                    "numeroIdentificacion",
                    ValidationErrorType.OUT_OF_RANGE,
                    HttpValidationMessages.INVALID_IDENTIFICATION_NUMBER
            );
        } else if (!ValidationHelper.hasDigitCount(value, MIN_IDENTIFICATION_DIGITS, MAX_IDENTIFICATION_DIGITS)) {
            builder.add(
                    "numeroIdentificacion",
                    ValidationErrorType.OUT_OF_RANGE,
                    "El numero de identificacion debe contener entre 6 y 10 digitos."
            );
        }
    }

    private static void validateRequiredPersonName(
            final String field,
            final String value,
            final ValidationResultBuilder builder
    ) {
        if (!ValidationHelper.hasText(value)) {
            builder.add(field, ValidationErrorType.REQUIRED, requiredPersonNameMessage(field));
            return;
        }
        validatePersonNameFormatAndLength(field, value, builder);
    }

    private static void validateOptionalPersonName(
            final String field,
            final String value,
            final ValidationResultBuilder builder
    ) {
        if (ValidationHelper.hasText(value)) {
            validatePersonNameFormatAndLength(field, value, builder);
        }
    }

    private static void validatePersonNameFormatAndLength(
            final String field,
            final String value,
            final ValidationResultBuilder builder
    ) {
        final String normalized = ValidationHelper.normalizeSpaces(value);
        builder.addIf(
                !ValidationHelper.isLengthAtMost(normalized, MAX_NAME_LENGTH),
                field,
                ValidationErrorType.INVALID_LENGTH,
                "Cada nombre o apellido puede tener maximo 50 caracteres."
        );
        builder.addIf(
                !ValidationHelper.isValidPersonName(normalized),
                field,
                ValidationErrorType.INVALID_FORMAT,
                HttpValidationMessages.INVALID_PERSON_NAME
        );
    }

    private static void validateCorreo(final String value, final ValidationResultBuilder builder) {
        if (!ValidationHelper.hasText(value)) {
            builder.add("correo", ValidationErrorType.REQUIRED, "El correo es obligatorio.");
            return;
        }
        final String normalized = value.trim();
        builder.addIf(
                !ValidationHelper.isLengthAtMost(normalized, MAX_EMAIL_LENGTH),
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

    private static void validatePassword(final String value, final ValidationResultBuilder builder) {
        if (value == null) {
            return;
        }
        final int length = value.length();
        builder.addIf(
                length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH,
                "password",
                ValidationErrorType.INVALID_LENGTH,
                "La clave debe tener entre 8 y 255 caracteres."
        );
    }

    private static String requiredPersonNameMessage(final String field) {
        return switch (field) {
            case "primerNombre" -> "El primer nombre es obligatorio.";
            case "primerApellido" -> "El primer apellido es obligatorio.";
            default -> HttpValidationMessages.REQUIRED;
        };
    }
}
