package co.edu.uco.asistenciasuco.application.features.asistencia.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum AsistenciaErrorCode implements ErrorDefinition {
    ERR_ESTUDIANTE_NO_PERTENECE_SESION("ERR_ESTUDIANTE_NO_PERTENECE_SESION", "El estudiante no pertenece a la sesion.", ErrorKind.FORBIDDEN),
    ERR_ASISTENCIA_REQUERIDA("ERR_ASISTENCIA_REQUERIDA", "La asistencia es obligatoria.", ErrorKind.VALIDATION),
    ERR_PRESENTE_REQUERIDO("ERR_PRESENTE_REQUERIDO", "Debe indicar si el estudiante asistio.", ErrorKind.VALIDATION),
    ERR_SESION_ASISTENCIA_REQUERIDA("ERR_SESION_ASISTENCIA_REQUERIDA", "La sesion de asistencia es obligatoria.", ErrorKind.VALIDATION),
    ERR_REGISTROS_ASISTENCIA_REQUERIDOS("ERR_REGISTROS_ASISTENCIA_REQUERIDOS", "Los registros de asistencia son obligatorios.", ErrorKind.VALIDATION),
    ERR_ESTADO_ASISTENCIA_REQUERIDO("ERR_ESTADO_ASISTENCIA_REQUERIDO", "El estado de asistencia es obligatorio.", ErrorKind.VALIDATION),
    ERR_CODIGO_VERIFICACION_REQUERIDO("ERR_CODIGO_VERIFICACION_REQUERIDO", "El codigo de verificacion es obligatorio.", ErrorKind.VALIDATION),
    ERR_CATEGORIA_REVISION_REQUERIDA("ERR_CATEGORIA_REVISION_REQUERIDA", "La categoria de revision es obligatoria.", ErrorKind.VALIDATION),
    ERR_JUSTIFICACION_REVISION_REQUERIDA("ERR_JUSTIFICACION_REVISION_REQUERIDA", "La justificacion de revision es obligatoria.", ErrorKind.VALIDATION),
    ERR_SOLICITUD_REVISION_REQUERIDA("ERR_SOLICITUD_REVISION_REQUERIDA", "La solicitud de revision es obligatoria.", ErrorKind.VALIDATION),
    ERR_ACCION_REVISION_REQUERIDA("ERR_ACCION_REVISION_REQUERIDA", "La accion sobre la solicitud es obligatoria.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_ASISTENCIA_LONGITUD_INVALIDA("ERR_OBSERVACION_ASISTENCIA_LONGITUD_INVALIDA", "Cuando se indique una observacion de asistencia, debe tener entre 5 y 250 caracteres.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_ASISTENCIA_REQUERIDA("ERR_OBSERVACION_ASISTENCIA_REQUERIDA", "Debe indicar una observacion cuando el estudiante no asiste.", ErrorKind.VALIDATION),
    ERR_MOTIVO_REVISION_REQUERIDO("ERR_MOTIVO_REVISION_REQUERIDO", "El motivo de revision es obligatorio.", ErrorKind.VALIDATION),
    ERR_MOTIVO_REVISION_LONGITUD_INVALIDA("ERR_MOTIVO_REVISION_LONGITUD_INVALIDA", "El motivo de revision debe tener entre 10 y 300 caracteres.", ErrorKind.VALIDATION),
    ERR_ESTADO_ASISTENCIA_INVALIDO("ERR_ESTADO_ASISTENCIA_INVALIDO", "El estado de asistencia debe ser AN, SJC o EX.", ErrorKind.VALIDATION),
    ERR_USUARIO_EJECUTOR_ASISTENCIA_REQUERIDO("ERR_USUARIO_EJECUTOR_ASISTENCIA_REQUERIDO", "El usuario autenticado que ejecuta el registro de asistencias es obligatorio.", ErrorKind.VALIDATION),
    ERR_DOCENTE_SIN_TITULARIDAD_SESION("ERR_DOCENTE_SIN_TITULARIDAD_SESION", "El docente autenticado no tiene titularidad sobre la sesion indicada.", ErrorKind.FORBIDDEN),
    // SES-01: ventana de registro ordinario [inicio, fin) contra el reloj del servidor; DB: SES_007 / SES_008 / SES_009.
    ERR_SESION_NO_INICIADA("ERR_SESION_NO_INICIADA", "La sesion aun no ha iniciado; la asistencia solo se registra durante la sesion.", ErrorKind.CONFLICT),
    ERR_SESION_CERRADA("ERR_SESION_CERRADA", "La sesion ya finalizo; la asistencia solo puede modificarse mediante un reclamo.", ErrorKind.CONFLICT),
    ERR_SESION_HORARIO_INDETERMINADO("ERR_SESION_HORARIO_INDETERMINADO", "El horario de la sesion no esta confirmado; no es posible registrar asistencia.", ErrorKind.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    AsistenciaErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
