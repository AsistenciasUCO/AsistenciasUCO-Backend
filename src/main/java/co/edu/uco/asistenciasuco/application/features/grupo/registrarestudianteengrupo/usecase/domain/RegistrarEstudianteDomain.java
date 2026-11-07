package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain;


import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.UsuarioRegistroDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;

import java.util.UUID;

/**
 * Dominio de la operacion registrar estudiante en grupo.
 */
public final class RegistrarEstudianteDomain {

    private final UsuarioRegistroDomain usuarioRegistro;
    private final UUID grupoId;
    private final UUID usuarioEjecutor;
    private final UUID tipoIdentificacionPendiente;
    private final Integer numeroIdentificacionPendiente;
    private final String passwordPendiente;

    /**
     * CULM-001 A.2-15: matricula de un estudiante ya registrado sin correo en la solicitud (el modal solo recibe el correo
     * enmascarado). El caso de uso lo resuelve con {@code usp_buscar_estudiante_por_documento_grupo} antes de validar nombres
     * y correo; mientras tanto solo se conocen el documento, el grupo y el ejecutor.
     */
    public static RegistrarEstudianteDomain pendienteDeResolverPorDocumento(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion,
            final String password,
            final UUID grupoId,
            final UUID usuarioEjecutor
    ) {
        return new RegistrarEstudianteDomain(tipoIdentificacionId, numeroIdentificacion, password, grupoId, usuarioEjecutor);
    }

    private RegistrarEstudianteDomain(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion,
            final String password,
            final UUID grupoId,
            final UUID usuarioEjecutor
    ) {
        this.usuarioRegistro = null;
        this.tipoIdentificacionPendiente = tipoIdentificacionId;
        this.numeroIdentificacionPendiente = numeroIdentificacion;
        this.passwordPendiente = password;
        this.grupoId = validarGrupo(grupoId);
        this.usuarioEjecutor = usuarioEjecutor;
    }

    /** {@code true} si falta resolver correo y nombres del estudiante existente por su documento. */
    public boolean esPendienteDeResolverPorDocumento() {
        return usuarioRegistro == null;
    }

    /** Dominio completo con los datos del estudiante resuelto; valida nombres y correo como un alta normal. */
    public RegistrarEstudianteDomain resueltoCon(
            final String correo,
            final String primerApellido,
            final String segundoApellido,
            final String primerNombre,
            final String segundoNombre
    ) {
        return new RegistrarEstudianteDomain(getTipoIdentificacionId(), getNumeroIdentificacion(), primerApellido, segundoApellido,
                primerNombre, segundoNombre, correo, getPassword(), grupoId, usuarioEjecutor);
    }

    public RegistrarEstudianteDomain(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion,
            final String primerApellido,
            final String segundoApellido,
            final String primerNombre,
            final String segundoNombre,
            final String correo,
            final String password,
            final UUID grupoId,
            final UUID usuarioEjecutor
    ) {
        this.usuarioRegistro = UsuarioRegistroDomain.crear(
                tipoIdentificacionId,
                numeroIdentificacion,
                primerApellido,
                segundoApellido,
                primerNombre,
                segundoNombre,
                correo,
                password
        );
        this.grupoId = validarGrupo(grupoId);
        this.usuarioEjecutor = usuarioEjecutor;
        this.tipoIdentificacionPendiente = null;
        this.numeroIdentificacionPendiente = null;
        this.passwordPendiente = null;
    }

    private static UUID validarGrupo(final UUID grupoId) {
        if (grupoId == null) {
            throw new ValidationException(GrupoErrorCode.ERR_GRUPO_REQUERIDO);
        }
        if (new UUID(0L, 0L).equals(grupoId)) {
            throw new ValidationException(GrupoErrorCode.ERR_GRUPO_INVALIDO);
        }
        return grupoId;
    }

    public UUID getTipoIdentificacionId() {
        return usuarioRegistro == null ? tipoIdentificacionPendiente : usuarioRegistro.getTipoIdentificacionId();
    }

    public Integer getNumeroIdentificacion() {
        return usuarioRegistro == null ? numeroIdentificacionPendiente : usuarioRegistro.getNumeroIdentificacion();
    }

    public String getPrimerApellido() {
        return usuarioRegistro == null ? null : usuarioRegistro.getPrimerApellido();
    }

    public String getSegundoApellido() {
        return usuarioRegistro == null ? null : usuarioRegistro.getSegundoApellido();
    }

    public String getPrimerNombre() {
        return usuarioRegistro == null ? null : usuarioRegistro.getPrimerNombre();
    }

    public String getSegundoNombre() {
        return usuarioRegistro == null ? null : usuarioRegistro.getSegundoNombre();
    }

    public String getCorreo() {
        return usuarioRegistro == null ? null : usuarioRegistro.getCorreo();
    }

    public String getPassword() {
        return usuarioRegistro == null ? passwordPendiente : usuarioRegistro.getPassword();
    }

    public UUID getGrupoId() {
        return grupoId;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

    public String resolverCredencialNueva() {
        return PasswordRegistroRule.resolverCredencialNueva(getPassword(), getNumeroIdentificacion());
    }
}
