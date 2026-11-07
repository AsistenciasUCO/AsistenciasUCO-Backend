package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistenciaqr.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistenciaqr.request.RegistrarAsistenciaQrRequest;

import java.util.regex.Pattern;

/**
 * CULM-001 B.2 (contrato 3.2.7): validacion de FORMA del registro por QR. Token de 43 caracteres base64url o PIN de 6
 * digitos; campos no admitidos (los del cuerpo anterior) se rechazan. La regla "exactamente uno" ({@code AQR_007}) la
 * aplica el caso de uso.
 */
public final class RegistrarAsistenciaQrRequestValidator {

    private static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final Pattern PIN = Pattern.compile("^\\d{6}$");

    public ValidationResult validar(final RegistrarAsistenciaQrRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "Escanea el codigo QR o digita el PIN.").build();
        }
        for (final String campo : request.camposDesconocidos()) {
            builder.add(campo, ValidationErrorType.INVALID_FORMAT, "El campo " + campo + " no es admitido.");
        }
        if (tieneTexto(request.getToken()) && !TOKEN.matcher(request.getToken().strip()).matches()) {
            builder.add("token", ValidationErrorType.INVALID_FORMAT, "El codigo QR no es valido.");
        }
        if (tieneTexto(request.getPin()) && !PIN.matcher(request.getPin().strip()).matches()) {
            builder.add("pin", ValidationErrorType.INVALID_FORMAT, "El PIN debe tener 6 digitos.");
        }
        return builder.build();
    }

    private static boolean tieneTexto(final String valor) {
        return valor != null && !valor.isBlank();
    }
}
