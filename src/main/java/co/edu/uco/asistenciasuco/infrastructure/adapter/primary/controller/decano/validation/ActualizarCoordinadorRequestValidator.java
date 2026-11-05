package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.ActualizarCoordinadorRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.validation.CorreccionDocumentoValidation;

/**
 * Validacion de forma de {@code PUT /api/v1/decano/coordinadores/{id}}. Las reglas de negocio (unicidad del correo,
 * ambito) las decide la DB.
 */
public final class ActualizarCoordinadorRequestValidator implements Validator<ActualizarCoordinadorRequest> {

    private static final int MAX_NOMBRE = 50;
    private static final int MAX_CORREO = 100;

    @Override
    public ValidationResult validate(final ActualizarCoordinadorRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "Los datos del coordinador son obligatorios.").build();
        }
        validarObligatorio(builder, "primerNombre", request.getPrimerNombre(), "El primer nombre es obligatorio.");
        validarObligatorio(builder, "primerApellido", request.getPrimerApellido(), "El primer apellido es obligatorio.");
        validarOpcional(builder, "segundoNombre", request.getSegundoNombre());
        validarOpcional(builder, "segundoApellido", request.getSegundoApellido());

        if (!ValidationHelper.hasText(request.getCorreo())) {
            builder.add("correo", ValidationErrorType.REQUIRED, "El correo electronico es obligatorio.");
        } else {
            builder.addIf(!ValidationHelper.isValidEmail(request.getCorreo()), "correo",
                    ValidationErrorType.INVALID_FORMAT, "El correo electronico no tiene un formato valido.");
            builder.addIf(request.getCorreo().trim().length() > MAX_CORREO, "correo",
                    ValidationErrorType.INVALID_LENGTH, "El correo electronico no puede superar " + MAX_CORREO + " caracteres.");
        }
        CorreccionDocumentoValidation.validar(builder, request, false);
        return builder.build();
    }

    private static void validarObligatorio(final ValidationResultBuilder builder, final String campo, final String valor,
                                           final String mensajeRequerido) {
        if (!ValidationHelper.hasText(valor)) {
            builder.add(campo, ValidationErrorType.REQUIRED, mensajeRequerido);
            return;
        }
        validarOpcional(builder, campo, valor);
    }

    private static void validarOpcional(final ValidationResultBuilder builder, final String campo, final String valor) {
        builder.addIf(valor != null && valor.trim().length() > MAX_NOMBRE, campo, ValidationErrorType.INVALID_LENGTH,
                "El campo " + campo + " no puede superar " + MAX_NOMBRE + " caracteres.");
        builder.addIf(!ValidationHelper.isValidPersonName(valor), campo, ValidationErrorType.INVALID_FORMAT,
                "El campo " + campo + " contiene caracteres no permitidos.");
    }
}
