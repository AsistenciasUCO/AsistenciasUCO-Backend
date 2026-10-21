package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain;


import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.util.UUID;

/**
 * Dominio de la operacion solicitar revision de asistencia.
 */
public final class SolicitarRevisionAsistenciaDomain {

    private final UUID sesion;
    private final String categoria;
    private final String justificacion;
    private final UUID soporteArchivoId;
    private final UUID usuario;

    public SolicitarRevisionAsistenciaDomain(
            final UUID sesion,
            final String categoria,
            final String justificacion,
            final UUID soporteArchivoId,
            final UUID usuario
    ) {
        validarSesion(sesion);
        validarUsuario(usuario);
        this.categoria = validarTextoObligatorio(categoria, AsistenciaErrorCode.ERR_CATEGORIA_REVISION_REQUERIDA);
        this.justificacion = validarTextoObligatorio(justificacion, AsistenciaErrorCode.ERR_JUSTIFICACION_REVISION_REQUERIDA);
        this.soporteArchivoId = soporteArchivoId;

        this.sesion = sesion;
        this.usuario = usuario;
    }

    private void validarSesion(final UUID sesion) {
        if (ObjectHelper.isNull(sesion)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_SESION_ASISTENCIA_REQUERIDA);
        }
    }

    private void validarUsuario(final UUID usuario) {
        if (ObjectHelper.isNull(usuario)) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
        }
    }

    private String validarTextoObligatorio(final String valor, final AsistenciaErrorCode errorCode) {
        final String normalizado = TextHelper.trim(valor);
        if (TextHelper.isNullOrBlank(normalizado)) {
            throw new ValidationException(errorCode);
        }
        if (!TextHelper.hasLengthBetween(normalizado, 1, 300)) {
            throw new ValidationException(AsistenciaErrorCode.ERR_MOTIVO_REVISION_LONGITUD_INVALIDA);
        }
        return normalizado;
    }

    public UUID getSesion() {
        return sesion;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getJustificacion() {
        return justificacion;
    }

    /** fileId opaco del soporte (opcional): SQL valida propiedad y estado antes de vincularlo. */
    public UUID getSoporteArchivoId() {
        return soporteArchivoId;
    }

    public UUID getUsuario() {
        return usuario;
    }

}
