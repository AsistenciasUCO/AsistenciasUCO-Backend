package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.validation;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.request.MatriculaQrRequests;

import java.util.regex.Pattern;

/**
 * HU176B: validacion de FORMA de los cuerpos publicos. La regla "token o PIN, no ambos" (MQR_007) la aplica el caso de uso;
 * aqui solo se acota el formato y la longitud para no enviar basura a la base de datos.
 */
public final class MatriculaQrRequestValidator {

    private static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final Pattern PIN = Pattern.compile("^[0-9]{6}$");
    private static final int MAX_NOMBRE = 50;
    private static final int MAX_CORREO = 100;
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 255;
    private static final long MIN_DOCUMENTO = 100_000L;

    public ValidationResult validarCredencial(final MatriculaQrRequests.Credencial request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "El codigo de matricula es obligatorio.").build();
        }
        validarCredencial(request.token(), request.pin(), builder);
        return builder.build();
    }

    public ValidationResult validarRegistro(final MatriculaQrRequests.Registro request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "Los datos del registro son obligatorios.").build();
        }
        validarCredencial(request.token(), request.pin(), builder);
        if (request.idTipoIdentificacion() == null) {
            builder.add("idTipoIdentificacion", ValidationErrorType.REQUIRED, "El tipo de identificacion es obligatorio.");
        }
        if (request.numeroIdentificacion() == null) {
            builder.add("numeroIdentificacion", ValidationErrorType.REQUIRED, "El numero de identificacion es obligatorio.");
        } else if (request.numeroIdentificacion() < MIN_DOCUMENTO || request.numeroIdentificacion() > Integer.MAX_VALUE) {
            builder.add("numeroIdentificacion", ValidationErrorType.OUT_OF_RANGE, "El numero de identificacion no es valido.");
        }
        nombre("primerNombre", request.primerNombre(), true, builder);
        nombre("segundoNombre", request.segundoNombre(), false, builder);
        nombre("primerApellido", request.primerApellido(), true, builder);
        nombre("segundoApellido", request.segundoApellido(), false, builder);
        correo(request.correo(), builder);
        password(request.password(), builder);
        return builder.build();
    }

    private static void validarCredencial(final String token, final String pin, final ValidationResultBuilder builder) {
        if (tieneTexto(token) && !TOKEN.matcher(token).matches()) {
            builder.add("token", ValidationErrorType.INVALID_FORMAT, "El codigo de matricula no es valido.");
        }
        if (tieneTexto(pin) && !PIN.matcher(pin).matches()) {
            builder.add("pin", ValidationErrorType.INVALID_FORMAT, "El PIN debe tener 6 digitos.");
        }
    }

    private static void nombre(final String campo, final String valor, final boolean obligatorio, final ValidationResultBuilder builder) {
        if (!tieneTexto(valor)) {
            if (obligatorio) {
                builder.add(campo, ValidationErrorType.REQUIRED, "El campo " + campo + " es obligatorio.");
            }
            return;
        }
        if (valor.strip().length() > MAX_NOMBRE) {
            builder.add(campo, ValidationErrorType.INVALID_LENGTH, "Cada nombre o apellido puede tener maximo 50 caracteres.");
        }
    }

    private static void correo(final String valor, final ValidationResultBuilder builder) {
        if (!tieneTexto(valor)) {
            builder.add("correo", ValidationErrorType.REQUIRED, "El correo es obligatorio.");
        } else if (valor.strip().length() > MAX_CORREO || !valor.contains("@")) {
            builder.add("correo", ValidationErrorType.INVALID_FORMAT, "El correo no es valido.");
        }
    }

    private static void password(final String valor, final ValidationResultBuilder builder) {
        if (!tieneTexto(valor)) {
            builder.add("password", ValidationErrorType.REQUIRED, "La contrasena es obligatoria.");
        } else if (valor.length() < MIN_PASSWORD || valor.length() > MAX_PASSWORD) {
            builder.add("password", ValidationErrorType.INVALID_LENGTH, "La contrasena debe tener entre 8 y 255 caracteres.");
        }
    }

    private static boolean tieneTexto(final String valor) {
        return valor != null && !valor.isBlank();
    }
}
