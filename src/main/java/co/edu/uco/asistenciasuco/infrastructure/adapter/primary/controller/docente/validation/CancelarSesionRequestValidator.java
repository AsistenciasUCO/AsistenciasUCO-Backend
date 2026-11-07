package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.CancelarSesionRequest;

/**
 * CULM-001 B.1 (HU047): forma del cuerpo. El motivo es obligatorio y, recortado, no supera 500 caracteres; la longitud
 * efectiva ({@code SESION/MOTIVO_CANCELACION_LONGITUD_*}) la valida la DB con {@code SES_013}.
 */
public final class CancelarSesionRequestValidator implements Validator<CancelarSesionRequest> {

    public static final int LONGITUD_MAXIMA_MOTIVO = 500;

    @Override
    public ValidationResult validate(final CancelarSesionRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null || !ValidationHelper.hasText(request.getMotivo())) {
            return builder.add("motivo", ValidationErrorType.REQUIRED, "El motivo de cancelacion es obligatorio.").build();
        }
        return builder.addIf(request.getMotivo().strip().length() > LONGITUD_MAXIMA_MOTIVO, "motivo",
                ValidationErrorType.INVALID_LENGTH, "El motivo de cancelacion no puede superar 500 caracteres.").build();
    }
}
