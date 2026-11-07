package co.edu.uco.asistenciasuco.application.features.grupo.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum GrupoErrorCode implements ErrorDefinition {
    ERR_MATRICULA_DUPLICADA("ERR_MATRICULA_DUPLICADA", "La matricula ya se encuentra registrada para este grupo.", ErrorKind.CONFLICT),
    ERR_CUPO_SUPERADO("ERR_CUPO_SUPERADO", "No hay cupos disponibles para el grupo.", ErrorKind.CONFLICT),
    ERR_CRUCE_HORARIO_ESTUDIANTE("ERR_CRUCE_HORARIO_ESTUDIANTE", "Existe cruce de horario para el estudiante.", ErrorKind.CONFLICT),
    ERR_CRUCE_HORARIO_DOCENTE("ERR_CRUCE_HORARIO_DOCENTE", "Existe cruce de horario para el docente.", ErrorKind.CONFLICT),
    ERR_GRUPO_NO_EXISTE("ERR_GRUPO_NO_EXISTE", "El grupo consultado no existe.", ErrorKind.NOT_FOUND),
    ERR_GRUPO_NO_HABILITADO("ERR_GRUPO_NO_HABILITADO", "El grupo no se encuentra habilitado.", ErrorKind.CONFLICT),
    ERR_GRUPO_REQUERIDO("ERR_GRUPO_REQUERIDO", "El grupo es obligatorio.", ErrorKind.VALIDATION),
    ERR_GRUPO_INVALIDO("ERR_GRUPO_INVALIDO", "El identificador del grupo no es valido.", ErrorKind.VALIDATION),
    // CULM-001 A.2-15: matricula manual sin correo de un documento que no resuelve a un estudiante matriculable (400, correo).
    ERR_CORREO_REQUERIDO_MATRICULA("FIELD_REQUIRED", "El correo es obligatorio: no hay un estudiante registrado que pueda matricular con ese documento.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    GrupoErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
