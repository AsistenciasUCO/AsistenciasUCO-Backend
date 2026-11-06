package co.edu.uco.asistenciasuco.application.features.matriculaqr.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * HU176B: codigos de error de la matricula por QR/PIN. Los mensajes por defecto respaldan los textos del catalogo
 * ({@code MQR_001..MQR_007}). MQR_001 es el mismo para cualquier token invalido (no revela si existio, vencio o se cerro).
 * MQR_003 (limite de intentos, 429) lo emite el filtro publico y no es una excepcion de aplicacion.
 */
public enum MatriculaQrErrorCode implements ErrorDefinition {
    MQR_001("MQR_001", "El código de matrícula no es válido o ya no está disponible. Solicítale al docente uno nuevo.", ErrorKind.NOT_FOUND),
    MQR_002("MQR_002", "La matrícula por código solo está disponible desde la red Wi-Fi del campus UCO.", ErrorKind.FORBIDDEN),
    MQR_004("MQR_004", "Ya estás matriculado en este grupo.", ErrorKind.CONFLICT),
    MQR_005("MQR_005", "Ya tienes una cuenta registrada. Inicia sesión para completar tu matrícula.", ErrorKind.CONFLICT),
    MQR_006("MQR_006", "No fue posible generar un código único. Inténtalo de nuevo.", ErrorKind.CONFLICT),
    MQR_007("MQR_007", "Debes indicar el código QR o el PIN, pero no ambos.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    MatriculaQrErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
