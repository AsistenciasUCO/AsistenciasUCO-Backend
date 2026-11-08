package co.edu.uco.asistenciasuco.application.features.admin.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * CULM-001 D.3 (HU168) y D.6 (HU172): codigos de la edicion de parametros y de la consulta de auditoria. Coinciden con el
 * catalogo de mensajes (PRM_*, AUD_*); el texto de aqui es la contingencia ante desconexion del catalogo y no lleva
 * marcadores de parametro.
 */
public enum ParametroErrorCode implements ErrorDefinition {
    PRM_002("PRM_002", "El parámetro no existe.", ErrorKind.NOT_FOUND),
    PRM_003("PRM_003", "Este parámetro no se puede editar.", ErrorKind.FORBIDDEN),
    PRM_004("PRM_004", "El valor no tiene el tipo requerido.", ErrorKind.VALIDATION),
    PRM_005("PRM_005", "El valor está fuera del rango permitido.", ErrorKind.VALIDATION),
    PRM_006("PRM_006", "El valor no es uno de los permitidos.", ErrorKind.VALIDATION),
    PRM_007("PRM_007", "El valor viola una regla entre parámetros.", ErrorKind.VALIDATION),
    PRM_008("PRM_008", "Otro usuario modificó el parámetro. Recarga y vuelve a intentarlo.", ErrorKind.CONFLICT),
    PRM_009("PRM_009", "Indica un motivo con la longitud mínima requerida.", ErrorKind.VALIDATION),
    PRM_010("PRM_010", "La edición de parámetros no está disponible con el catálogo de configuración activo.", ErrorKind.CONFLICT),
    PRM_012("PRM_012", "La edición de parámetros requiere una sola institución activa.", ErrorKind.CONFLICT),
    AUD_001("AUD_001", "La consulta de auditoría requiere una sola institución activa.", ErrorKind.CONFLICT),
    AUD_002("AUD_002", "Los filtros de solo asistencia y solo administración no se pueden combinar.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    ParametroErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
