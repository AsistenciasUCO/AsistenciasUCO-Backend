package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request;

import java.util.UUID;

public final class SolicitarRevisionAsistenciaRequest {

    private UUID sesionId;
    private UUID sesion;
    private String categoria;
    private String justificacion;
    private String motivo;
    private UUID soporteArchivoId;
    /** Legacy (DEPRECATED): se acepta por compatibilidad de forma pero NUNCA se usa como fuente de verdad. */
    private String soporteNombre;
    /** Legacy (DEPRECATED): ver soporteNombre. */
    private String soporteUrl;

    public UUID getSesionId() {
        return sesionId == null ? sesion : sesionId;
    }

    public void setSesionId(final UUID sesionId) {
        this.sesionId = sesionId;
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
        return justificacion == null ? motivo : justificacion;
    }

    public void setJustificacion(final String justificacion) {
        this.justificacion = justificacion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(final String motivo) {
        this.motivo = motivo;
    }

    public UUID getSoporteArchivoId() {
        return soporteArchivoId;
    }

    public void setSoporteArchivoId(final UUID soporteArchivoId) {
        this.soporteArchivoId = soporteArchivoId;
    }

    public String getSoporteNombre() {
        return soporteNombre;
    }

    public void setSoporteNombre(final String soporteNombre) {
        this.soporteNombre = soporteNombre;
    }

    public String getSoporteUrl() {
        return soporteUrl;
    }

    public void setSoporteUrl(final String soporteUrl) {
        this.soporteUrl = soporteUrl;
    }
}
