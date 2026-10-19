package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para registrar un estudiante en un grupo.
 */
public final class RegistrarEstudianteDTO {

    private UUID estudianteId;
    private UUID tipoIdentificacionId;
    private Integer numeroIdentificacion;
    private String primerApellido;
    private String segundoApellido;
    private String primerNombre;
    private String segundoNombre;
    private String correo;
    private String password;
    private UUID grupoId;
    private UUID usuarioEjecutor;

    public RegistrarEstudianteDTO() {
        super();
    }

    public RegistrarEstudianteDTO(
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
        this(null, tipoIdentificacionId, numeroIdentificacion, primerApellido, segundoApellido, primerNombre, segundoNombre, correo, password, grupoId, usuarioEjecutor);
    }

    public RegistrarEstudianteDTO(
            final UUID estudianteId,
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
        setEstudianteId(estudianteId);
        setTipoIdentificacionId(tipoIdentificacionId);
        setNumeroIdentificacion(numeroIdentificacion);
        setPrimerApellido(primerApellido);
        setSegundoApellido(segundoApellido);
        setPrimerNombre(primerNombre);
        setSegundoNombre(segundoNombre);
        setCorreo(correo);
        setPassword(password);
        setGrupoId(grupoId);
        setUsuarioEjecutor(usuarioEjecutor);
    }

    public UUID getEstudianteId() {
        return estudianteId;
    }

    public void setEstudianteId(final UUID estudianteId) {
        this.estudianteId = estudianteId;
    }

    public UUID getTipoIdentificacionId() {
        return tipoIdentificacionId;
    }

    public void setTipoIdentificacionId(final UUID tipoIdentificacionId) {
        this.tipoIdentificacionId = tipoIdentificacionId;
    }

    public Integer getNumeroIdentificacion() {
        return numeroIdentificacion;
    }

    public void setNumeroIdentificacion(final Integer numeroIdentificacion) {
        this.numeroIdentificacion = numeroIdentificacion;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(final String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public void setSegundoApellido(final String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public void setPrimerNombre(final String primerNombre) {
        this.primerNombre = primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public void setSegundoNombre(final String segundoNombre) {
        this.segundoNombre = segundoNombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(final String correo) {
        this.correo = correo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public UUID getGrupoId() {
        return grupoId;
    }

    public void setGrupoId(final UUID grupoId) {
        this.grupoId = grupoId;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

    public void setUsuarioEjecutor(final UUID usuarioEjecutor) {
        this.usuarioEjecutor = usuarioEjecutor;
    }
}
