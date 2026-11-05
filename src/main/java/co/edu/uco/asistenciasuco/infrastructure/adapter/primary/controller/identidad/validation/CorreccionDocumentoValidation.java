package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request.CorreccionDocumentoRequest;

/**
 * HU159: validacion de forma comun a la correccion de documento. Tipo y numero van juntos; el numero es positivo; el
 * motivo tiene hasta 500 caracteres y es obligatorio si {@code motivoSiempre} o si cambia el documento. Las reglas de
 * negocio (unicidad, interruptor IDENTIDAD.PERMITIR_CORREGIR_DOCUMENTO) las decide la base de datos.
 */
public final class CorreccionDocumentoValidation {

    private static final int MAX_MOTIVO = 500;

    private CorreccionDocumentoValidation() {
    }

    public static void validar(final ValidationResultBuilder builder, final CorreccionDocumentoRequest request,
                               final boolean motivoSiempre) {
        final boolean conTipo = request.getIdTipoIdentificacion() != null;
        final boolean conNumero = request.getNumeroIdentificacion() != null;
        builder.addIf(conTipo != conNumero, conTipo ? "numeroIdentificacion" : "idTipoIdentificacion",
                ValidationErrorType.REQUIRED, "El tipo y el numero de documento se corrigen juntos.");
        builder.addIf(conNumero && request.getNumeroIdentificacion() <= 0, "numeroIdentificacion",
                ValidationErrorType.INVALID_FORMAT, "El numero de documento debe ser un numero positivo.");
        final boolean conMotivo = ValidationHelper.hasText(request.getMotivo());
        builder.addIf((motivoSiempre || request.solicitaCambioDocumento()) && !conMotivo, "motivo",
                ValidationErrorType.REQUIRED, "El motivo de la correccion es obligatorio.");
        builder.addIf(conMotivo && request.getMotivo().trim().length() > MAX_MOTIVO, "motivo",
                ValidationErrorType.INVALID_LENGTH, "El motivo no puede superar " + MAX_MOTIVO + " caracteres.");
    }
}
