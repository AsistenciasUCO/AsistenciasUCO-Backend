package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.ActualizarDocenteProgramaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.validation.CorreccionDocumentoValidation;

/** HU092: al menos un campo; primer nombre/apellido no pueden quedar en blanco si vienen; segundos pueden limpiarse con "". */
public final class ActualizarDocenteProgramaRequestValidator implements Validator<ActualizarDocenteProgramaRequest> {

    private static final int MAX_NOMBRE = 50;

    @Override
    public ValidationResult validate(final ActualizarDocenteProgramaRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion a actualizar es obligatoria.").build();
        }
        if (request.getIdTipoVinculacion() == null && request.getPrimerNombre() == null && request.getSegundoNombre() == null
                && request.getPrimerApellido() == null && request.getSegundoApellido() == null
                && !request.solicitaCambioDocumento()) {
            builder.add("campos", ValidationErrorType.REQUIRED, "Debe indicar al menos un dato a actualizar.");
        }
        if (request.getIdTipoVinculacion() != null && request.getIdTipoVinculacion().getMostSignificantBits() == 0
                && request.getIdTipoVinculacion().getLeastSignificantBits() == 0) {
            builder.add("idTipoVinculacion", ValidationErrorType.INVALID_UUID, "El tipo de vinculacion no es valido.");
        }
        nombre(builder, "primerNombre", request.getPrimerNombre(), true);
        nombre(builder, "primerApellido", request.getPrimerApellido(), true);
        nombre(builder, "segundoNombre", request.getSegundoNombre(), false);
        nombre(builder, "segundoApellido", request.getSegundoApellido(), false);
        CorreccionDocumentoValidation.validar(builder, request, false);
        return builder.build();
    }

    private static void nombre(final ValidationResultBuilder builder, final String campo, final String valor, final boolean noPuedeQuedarVacio) {
        if (valor == null) {
            return;
        }
        if (noPuedeQuedarVacio && valor.isBlank()) {
            builder.add(campo, ValidationErrorType.REQUIRED, "El campo " + campo + " no puede quedar vacio.");
        } else if (valor.trim().length() > MAX_NOMBRE) {
            builder.add(campo, ValidationErrorType.INVALID_LENGTH, "El campo " + campo + " excede la longitud permitida.");
        }
    }
}
