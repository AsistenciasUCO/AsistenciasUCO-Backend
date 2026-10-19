package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto;

import java.util.UUID;

/**
 * DTO de entrada para solicitar la revision de una asistencia.
 */
public final class SolicitarRevisionAsistenciaDTO {

    private UUID sesion;
    private String categoria;
    private String justificacion;
    private UUID soporteArchivoId;
    private UUID usuario;

    public SolicitarRevisionAsistenciaDTO() {
        super();
    }

    public SolicitarRevisionAsistenciaDTO(
            final UUID sesion,
            final String categoria,
            final String justificacion,
            final UUID soporteArchivoId,
            final UUID usuario
    ) {
        setSesion(sesion);
        setCategoria(categoria);
        setJustificacion(justificacion);
        setSoporteArchivoId(soporteArchivoId);
        setUsuario(usuario);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(final String categoria) {
        this.categoria = categoria;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public void setJustificacion(final String justificacion) {
        this.justificacion = justificacion;
    }

    /** Identificador opaco (fileId) del soporte ya subido; opcional. Nunca una URL ni un nombre de archivo. */
    public UUID getSoporteArchivoId() {
        return soporteArchivoId;
    }

    public void setSoporteArchivoId(final UUID soporteArchivoId) {
        this.soporteArchivoId = soporteArchivoId;
    }

    public UUID getUsuario() {
        return usuario;
    }

    public void setUsuario(final UUID usuario) {
        this.usuario = usuario;
    }

}
