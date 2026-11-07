package co.edu.uco.asistenciasuco.application.features.festivos.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * CULM-001 B.1: codigos de los dias festivos. Se exponen tal cual en {@code ApiErrorResponse.code}; el texto que ve el
 * usuario sale del catalogo de mensajes ({@code FES_*}) y estos textos son el respaldo ante desconexion.
 */
public enum FestivoErrorCode implements ErrorDefinition {
    FES_001("FES_001", "El año de festivos no es válido.", ErrorKind.VALIDATION),
    FES_002("FES_002", "El listado de festivos recibido no es válido.", ErrorKind.VALIDATION),
    FES_003("FES_003", "El listado de festivos no tiene una cantidad válida de días.", ErrorKind.TECHNICAL),
    FES_004("FES_004", "La fuente de festivos no es válida.", ErrorKind.VALIDATION),
    FES_006("FES_006", "Indica el periodo académico o el año, no ambos.", ErrorKind.VALIDATION),
    FES_007("FES_007", "El periodo académico no existe.", ErrorKind.NOT_FOUND),
    FES_009("FES_009", "Algunos grupos no se pudieron actualizar con los festivos.", ErrorKind.TECHNICAL),
    FES_010("FES_010", "No fue posible obtener los festivos del año; se usó el cálculo local.", ErrorKind.TECHNICAL);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    FestivoErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
