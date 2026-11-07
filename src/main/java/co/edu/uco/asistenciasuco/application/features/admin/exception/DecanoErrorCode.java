package co.edu.uco.asistenciasuco.application.features.admin.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * CULM-001 A.2-8: errores de negocio de la gestion del decano por el administrador. El codigo coincide con el del
 * catalogo de mensajes (DB / App Configuration), de modo que el usuario recibe el texto institucional y no el generico
 * de {@code CONFLICT}; el texto de aqui es solo la contingencia ante desconexion del catalogo.
 */
public enum DecanoErrorCode implements ErrorDefinition {
    DEC_002("DEC_002", "No se puede desactivar al decano de una facultad con programas activos.", ErrorKind.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    DecanoErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
