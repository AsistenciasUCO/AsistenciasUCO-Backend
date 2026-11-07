package co.edu.uco.asistenciasuco.application.exception.business;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;

/**
 * CULM-001 B.2: limite de intentos superado en un caso de uso (por ejemplo, por estudiante o por token del QR de
 * asistencia). Se publica como 429 con {@code Retry-After}; el codigo lo fija la definicion de error.
 */
public final class TooManyRequestsException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public TooManyRequestsException(final ErrorDefinition errorDefinition) {
        super(errorDefinition);
    }
}
