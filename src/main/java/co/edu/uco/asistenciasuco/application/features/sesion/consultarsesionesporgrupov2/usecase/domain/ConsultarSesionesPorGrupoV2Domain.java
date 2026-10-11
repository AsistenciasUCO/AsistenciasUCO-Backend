package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.UUID;

/**
 * Dominio del listado v2 de sesiones de un grupo.
 */
public record ConsultarSesionesPorGrupoV2Domain(UUID grupo, UUID usuarioEjecutor) {

    public ConsultarSesionesPorGrupoV2Domain {
        if (ObjectHelper.isNull(grupo)) {
            throw new ValidationException(GrupoErrorCode.ERR_GRUPO_REQUERIDO);
        }
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
        }
    }

    public UUID getGrupo() {
        return grupo;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }
}
