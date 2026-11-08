package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.AdmitirEstudianteRequest;

/**
 * Validacion de forma de la admision de un estudiante (CULM-001 D.4, HU156): documento, nombres, correo y programa. La
 * jerarquia, el correo institucional, la unicidad y la clave inicial las decide el caso de uso y la base de datos. Los mensajes
 * nunca incluyen valores recibidos (la clave tampoco se evalua aqui).
 */
public final class AdmitirEstudianteRequestValidator implements Validator<AdmitirEstudianteRequest> {

    private static final int MAX_NOMBRE = 50;
    private static final int MAX_CORREO = 255;

    @Override
    public ValidationResult validate(final AdmitirEstudianteRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "Los datos del estudiante son obligatorios.").build();
        }
        builder.addIf(request.getTipoIdentificacionId() == null || !ValidationHelper.isNonEmptyUuid(request.getTipoIdentificacionId()),
                "tipoIdentificacionId", ValidationErrorType.REQUIRED, "El tipo de identificacion es obligatorio.");
        builder.addIf(request.getNumeroIdentificacion() == null || !ValidationHelper.isPositive(request.getNumeroIdentificacion()),
                "numeroIdentificacion", request.getNumeroIdentificacion() == null ? ValidationErrorType.REQUIRED : ValidationErrorType.INVALID_FORMAT,
                "El numero de identificacion es obligatorio y debe ser positivo.");
        nombre(builder, "primerNombre", request.getPrimerNombre(), true);
        nombre(builder, "primerApellido", request.getPrimerApellido(), true);
        nombre(builder, "segundoNombre", request.getSegundoNombre(), false);
        nombre(builder, "segundoApellido", request.getSegundoApellido(), false);
        if (!ValidationHelper.hasText(request.getCorreo())) {
            builder.add("correo", ValidationErrorType.REQUIRED, "El correo es obligatorio.");
        } else {
            builder.addIf(request.getCorreo().trim().length() > MAX_CORREO, "correo", ValidationErrorType.INVALID_LENGTH,
                    "El correo no puede superar " + MAX_CORREO + " caracteres.");
            builder.addIf(!ValidationHelper.isValidEmail(request.getCorreo().trim()), "correo", ValidationErrorType.INVALID_FORMAT,
                    "El correo no tiene un formato valido.");
        }
        builder.addIf(request.getIdPrograma() == null || !ValidationHelper.isNonEmptyUuid(request.getIdPrograma()),
                "idPrograma", ValidationErrorType.REQUIRED, "El programa es obligatorio.");
        return builder.build();
    }

    private static void nombre(final ValidationResultBuilder builder, final String campo, final String valor, final boolean obligatorio) {
        if (obligatorio && !ValidationHelper.hasText(valor)) {
            builder.add(campo, ValidationErrorType.REQUIRED, "El campo " + campo + " es obligatorio.");
            return;
        }
        builder.addIf(valor != null && valor.trim().length() > MAX_NOMBRE, campo, ValidationErrorType.INVALID_LENGTH,
                "El campo " + campo + " no puede superar " + MAX_NOMBRE + " caracteres.");
        builder.addIf(ValidationHelper.hasText(valor) && !ValidationHelper.isValidPersonName(valor), campo, ValidationErrorType.INVALID_FORMAT,
                "El campo " + campo + " contiene caracteres no permitidos.");
    }
}
