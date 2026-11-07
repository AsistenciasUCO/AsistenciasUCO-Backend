package co.edu.uco.asistenciasuco.application.features.tomaasistencia.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * CULM-001 B.2 (DEC-013): codigos de las tomas de asistencia. Se exponen tal cual en {@code ApiErrorResponse.code}; el
 * texto que ve el usuario sale del catalogo de mensajes ({@code TOM_*}) y estos textos son el respaldo ante desconexion.
 */
public enum TomaAsistenciaErrorCode implements ErrorDefinition {
    TOM_001("TOM_001", "La toma de asistencia no existe.", ErrorKind.NOT_FOUND),
    TOM_002("TOM_002", "El modo de la toma no es válido.", ErrorKind.VALIDATION),
    TOM_003("TOM_003", "Ya hay una toma abierta en esta sesión; ciérrala antes de abrir otra.", ErrorKind.CONFLICT),
    TOM_004("TOM_004", "La sesión ya tiene el máximo de tomas permitido.", ErrorKind.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    TomaAsistenciaErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
