package co.edu.uco.asistenciasuco.application.features.asistenciaqr.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * CULM-001 B.2 (HU175, HU176; DEC-015): codigos del QR/PIN rotativo de asistencia. {@code AQR_001} es el mismo error para
 * cualquier token o PIN desconocido, cerrado o vencido (no revela si existio). {@code AQR_002}, {@code AQR_010} y
 * {@code AQR_011} los aplica el backend antes de llamar a la base de datos.
 */
public enum AsistenciaQrErrorCode implements ErrorDefinition {
    AQR_001("AQR_001", "El código QR o el PIN no es válido o ya venció. Pide el PIN vigente al docente.", ErrorKind.NOT_FOUND),
    AQR_002("AQR_002", "Debes estar conectado a la red Wi-Fi del campus para registrar tu asistencia.", ErrorKind.FORBIDDEN),
    AQR_003("AQR_003", "Esta toma es manual; no admite código QR.", ErrorKind.CONFLICT),
    AQR_004("AQR_004", "No estás matriculado en el grupo de esta clase.", ErrorKind.FORBIDDEN),
    AQR_005("AQR_005", "La toma de asistencia ya se cerró.", ErrorKind.CONFLICT),
    AQR_006("AQR_006", "No fue posible generar el código. Intenta de nuevo.", ErrorKind.CONFLICT),
    AQR_007("AQR_007", "Escanea el código QR o digita el PIN.", ErrorKind.VALIDATION),
    AQR_010("AQR_010", "El registro de asistencia por QR no está disponible.", ErrorKind.NOT_FOUND),
    AQR_011("AQR_011", "Hiciste demasiados intentos. Espera un minuto e intenta de nuevo.", ErrorKind.TOO_MANY_REQUESTS);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    AsistenciaQrErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
