package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2;

import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationHelper;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpUtcInstantCodec;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Validacion estricta del cuerpo de escritura v2 de sesiones (UTC-D02/D05/D08).
 *
 * <p>Todas las fallas se agregan en un unico {@code 400 VALIDATION_ERROR} con detalle por campo:
 * {@code FIELD_REQUIRED} (ausente o null), {@code FIELD_INVALID_FORMAT} (instante fuera del perfil
 * UTC-D02), {@code FIELD_UNKNOWN} (campos no declarados, p. ej. {@code docente} o
 * {@code usuarioEjecutor}: el actor sale solo del JWT). Ningun mensaje repite el valor recibido y
 * ninguna falla del codec llega al manejador generico como 500.</p>
 */
final class SesionV2RequestParser {

    static final String FIELD_GRUPO = "grupo";
    static final String FIELD_NOMBRE = "nombre";
    static final String FIELD_INICIO = "fechaHoraInicio";
    static final String FIELD_FIN = "fechaHoraFin";

    private static final Set<String> CAMPOS_CREAR = Set.of(FIELD_GRUPO, FIELD_NOMBRE, FIELD_INICIO, FIELD_FIN);
    private static final Set<String> CAMPOS_ACTUALIZAR = Set.of(FIELD_NOMBRE, FIELD_INICIO, FIELD_FIN);
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final int NOMBRE_MAXIMO = 50;
    private static final String MENSAJE_INSTANTE =
            "Debe ser fecha-hora RFC3339 con segundos, 0 a 7 decimales y offset Z o +/-HH:mm.";

    private SesionV2RequestParser() {
    }

    record CrearSesionV2(UUID grupo, String nombre, LocalDateTime inicioUtc, LocalDateTime finUtc) {
    }

    record ActualizarSesionV2(String nombre, LocalDateTime inicioUtc, LocalDateTime finUtc) {
    }

    static CrearSesionV2 parseCrear(final Map<String, Object> body) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (body == null) {
            RequestValidationGuard.validate(builder.add("request", ValidationErrorType.REQUIRED,
                    "La informacion de la sesion es obligatoria.").build());
        }
        rechazarDesconocidos(builder, body, CAMPOS_CREAR);
        final UUID grupo = uuidRequerido(builder, body, FIELD_GRUPO);
        final String nombre = nombreRequerido(builder, body);
        final LocalDateTime inicio = instanteRequerido(builder, body, FIELD_INICIO);
        final LocalDateTime fin = instanteRequerido(builder, body, FIELD_FIN);
        RequestValidationGuard.validate(builder.build());
        return new CrearSesionV2(grupo, nombre, inicio, fin);
    }

    static ActualizarSesionV2 parseActualizar(final Map<String, Object> body) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (body == null) {
            RequestValidationGuard.validate(builder.add("request", ValidationErrorType.REQUIRED,
                    "La informacion de la sesion es obligatoria.").build());
        }
        rechazarDesconocidos(builder, body, CAMPOS_ACTUALIZAR);
        final String nombre = nombreRequerido(builder, body);
        final LocalDateTime inicio = instanteRequerido(builder, body, FIELD_INICIO);
        final LocalDateTime fin = instanteRequerido(builder, body, FIELD_FIN);
        RequestValidationGuard.validate(builder.build());
        return new ActualizarSesionV2(nombre, inicio, fin);
    }

    private static void rechazarDesconocidos(
            final ValidationResultBuilder builder,
            final Map<String, Object> body,
            final Set<String> permitidos
    ) {
        body.keySet().stream()
                .filter(campo -> !permitidos.contains(campo))
                .sorted()
                .forEach(campo -> builder.add(campo, ValidationErrorType.UNKNOWN_FIELD,
                        "El campo no pertenece al contrato v2 de sesiones."));
    }

    private static UUID uuidRequerido(final ValidationResultBuilder builder, final Map<String, Object> body, final String campo) {
        final Object valor = body.get(campo);
        if (valor == null) {
            builder.add(campo, ValidationErrorType.REQUIRED, "El grupo es obligatorio.");
            return null;
        }
        if (!(valor instanceof String texto) || !UUID_PATTERN.matcher(texto).matches()) {
            builder.add(campo, ValidationErrorType.INVALID_UUID, "El identificador del grupo no es valido.");
            return null;
        }
        final UUID uuid = UUID.fromString(texto);
        if (!ValidationHelper.isNonEmptyUuid(uuid)) {
            builder.add(campo, ValidationErrorType.INVALID_UUID, "El identificador del grupo no es valido.");
            return null;
        }
        return uuid;
    }

    private static String nombreRequerido(final ValidationResultBuilder builder, final Map<String, Object> body) {
        final Object valor = body.get(FIELD_NOMBRE);
        if (valor != null && !(valor instanceof String)) {
            builder.add(FIELD_NOMBRE, ValidationErrorType.INVALID_TYPE, "El nombre debe ser texto.");
            return null;
        }
        final String nombre = (String) valor;
        if (!ValidationHelper.hasText(nombre)) {
            builder.add(FIELD_NOMBRE, ValidationErrorType.REQUIRED, "El nombre es obligatorio.");
            return null;
        }
        if (!ValidationHelper.isLengthBetween(nombre.trim(), 1, NOMBRE_MAXIMO)) {
            builder.add(FIELD_NOMBRE, ValidationErrorType.INVALID_LENGTH, "El nombre debe tener entre 1 y 50 caracteres.");
            return null;
        }
        return nombre;
    }

    private static LocalDateTime instanteRequerido(
            final ValidationResultBuilder builder,
            final Map<String, Object> body,
            final String campo
    ) {
        final Object valor = body.get(campo);
        if (valor == null) {
            builder.add(campo, ValidationErrorType.REQUIRED, "La fecha-hora con offset es obligatoria.");
            return null;
        }
        if (!(valor instanceof String texto)) {
            builder.add(campo, ValidationErrorType.INVALID_FORMAT, MENSAJE_INSTANTE);
            return null;
        }
        try {
            return HttpUtcInstantCodec.toUtcDatabaseDateTime(texto);
        } catch (IllegalArgumentException _) {
            builder.add(campo, ValidationErrorType.INVALID_FORMAT, MENSAJE_INSTANTE);
            return null;
        }
    }
}
