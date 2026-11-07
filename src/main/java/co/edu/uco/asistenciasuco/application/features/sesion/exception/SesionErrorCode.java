package co.edu.uco.asistenciasuco.application.features.sesion.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum SesionErrorCode implements ErrorDefinition {
    ERR_SESION_NO_EXISTE("ERR_SESION_NO_EXISTE", "La sesion consultada no existe.", ErrorKind.NOT_FOUND),
    ERR_SESION_REQUERIDA("ERR_SESION_REQUERIDA", "La sesion es obligatoria.", ErrorKind.VALIDATION),
    ERR_NOMBRE_SESION_REQUERIDO("ERR_NOMBRE_SESION_REQUERIDO", "El nombre de la sesion es obligatorio.", ErrorKind.VALIDATION),
    ERR_NOMBRE_SESION_LONGITUD_INVALIDA("ERR_NOMBRE_SESION_LONGITUD_INVALIDA", "El nombre de la sesion debe tener entre 1 y 50 caracteres.", ErrorKind.VALIDATION),
    ERR_DOCENTE_REQUERIDO("ERR_DOCENTE_REQUERIDO", "El docente autenticado es obligatorio para operar la sesion.", ErrorKind.VALIDATION),
    ERR_RANGO_FECHAS_SESION_INVALIDO("ERR_RANGO_FECHAS_SESION_INVALIDO", "El rango de fechas de la sesion no es valido.", ErrorKind.VALIDATION),
    ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA("ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA", "Cuando se indique una descripcion, debe tener entre 10 y 250 caracteres.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_CIERRE_REQUERIDA("ERR_OBSERVACION_CIERRE_REQUERIDA", "La observacion de cierre es obligatoria.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_CIERRE_LONGITUD_INVALIDA("ERR_OBSERVACION_CIERRE_LONGITUD_INVALIDA", "La observacion de cierre debe tener entre 10 y 250 caracteres.", ErrorKind.VALIDATION),
    // CULM-001 B.1 (HU047): cancelacion de sesion por el docente titular. Codigos de la DB expuestos tal cual.
    SES_011("SES_011", "La sesión ya estaba cancelada.", ErrorKind.CONFLICT),
    SES_012("SES_012", "No es posible cancelar una sesión que ya tiene asistencia registrada.", ErrorKind.CONFLICT),
    SES_013("SES_013", "El motivo de cancelación debe tener entre 10 y 500 caracteres.", ErrorKind.VALIDATION),
    SES_016("SES_016", "La sesión ya terminó y tiene asistencia registrada; no se puede cancelar. Las correcciones se hacen por reclamo.", ErrorKind.CONFLICT),
    // CULM-001 A.2-10: sesion solapada en el grupo o con otro grupo del docente. 400 (el OpenAPI de las sesiones declara 400)
    // con su codigo de catalogo para que el usuario reciba el texto institucional; este es el texto de contingencia.
    SES_017("SES_017", "La sesión se cruza con otra sesión del grupo o del docente en ese horario.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    SesionErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
