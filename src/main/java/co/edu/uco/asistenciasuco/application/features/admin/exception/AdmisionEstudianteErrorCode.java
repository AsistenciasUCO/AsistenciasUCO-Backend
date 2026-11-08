package co.edu.uco.asistenciasuco.application.features.admin.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * Rechazos de la admision de estudiantes por el administrador (CULM-001 D.4, HU156). El codigo coincide con el del catalogo de
 * mensajes (DB / App Configuration): el usuario recibe el texto institucional y no el generico de CONFLICT o FORBIDDEN.
 * {@code ADS_001} (exito) no es un error.
 */
public enum AdmisionEstudianteErrorCode implements ErrorDefinition {
    ADS_002("ADS_002", "El estudiante ya está admitido en este programa.", ErrorKind.CONFLICT),
    ADS_003("ADS_003", "El programa no existe o no pertenece a tu institución.", ErrorKind.FORBIDDEN),
    ADS_004("ADS_004", "El documento y el correo corresponden a personas distintas.", ErrorKind.CONFLICT),
    ADS_005("ADS_005", "El correo debe ser institucional.", ErrorKind.VALIDATION),
    ADS_006("ADS_006", "Indica una contraseña inicial para el estudiante nuevo.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    AdmisionEstudianteErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.kind = kind;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    @Override
    public ErrorKind kind() {
        return kind;
    }
}
