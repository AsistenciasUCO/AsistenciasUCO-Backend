package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.AdminParametroController.ActualizarParametroRequest;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * CULM-001 D.3 (HU168): forma del cuerpo del PUT. El motivo minimo, el tipo, el rango y la version los valida la DB
 * ({@code PRM_004..PRM_009}); aqui solo obligatoriedad, tope de longitud (valor 4000, motivo 500) y fecha ISO-8601 UTC.
 */
public final class ActualizarParametroRequestValidator implements Validator<ActualizarParametroRequest> {

    public static final int LONGITUD_MAXIMA_VALOR = 4000;
    public static final int LONGITUD_MAXIMA_MOTIVO = 500;

    @Override
    public ValidationResult validate(final ActualizarParametroRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null || !ValidationHelper.hasText(request.valor())) {
            return builder.add("valor", ValidationErrorType.REQUIRED, "El valor es obligatorio.").build();
        }
        builder.addIf(request.valor().strip().length() > LONGITUD_MAXIMA_VALOR, "valor", ValidationErrorType.INVALID_LENGTH,
                "El valor no puede superar 4000 caracteres.");
        if (!ValidationHelper.hasText(request.fechaModificacionEsperada())) {
            builder.add("fechaModificacionEsperada", ValidationErrorType.REQUIRED, "La versión leída del parámetro es obligatoria.");
        } else {
            builder.addIf(!esInstanteIso(request.fechaModificacionEsperada()), "fechaModificacionEsperada",
                    ValidationErrorType.INVALID_FORMAT, "La versión del parámetro debe ser una fecha ISO-8601 en UTC.");
        }
        if (!ValidationHelper.hasText(request.motivo())) {
            builder.add("motivo", ValidationErrorType.REQUIRED, "El motivo es obligatorio.");
        } else {
            builder.addIf(request.motivo().strip().length() > LONGITUD_MAXIMA_MOTIVO, "motivo",
                    ValidationErrorType.INVALID_LENGTH, "El motivo no puede superar 500 caracteres.");
        }
        return builder.build();
    }

    private static boolean esInstanteIso(final String texto) {
        try {
            Instant.parse(texto.strip());
            return true;
        } catch (final DateTimeParseException exception) {
            return false;
        }
    }
}
