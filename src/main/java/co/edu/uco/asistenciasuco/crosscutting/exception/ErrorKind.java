package co.edu.uco.asistenciasuco.crosscutting.exception;

public enum ErrorKind {
    VALIDATION,
    BUSINESS,
    NOT_FOUND,
    CONFLICT,
    FORBIDDEN,
    UNAUTHORIZED,
    /** CULM-001 B.2: limite de intentos superado (429). */
    TOO_MANY_REQUESTS,
    TECHNICAL
}
