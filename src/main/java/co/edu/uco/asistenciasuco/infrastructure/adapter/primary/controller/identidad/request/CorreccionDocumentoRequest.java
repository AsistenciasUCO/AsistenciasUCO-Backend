package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request;

import java.util.UUID;

/**
 * HU159 (decision de John 2026-11-05): campos comunes de la correccion de identidad. El tipo y el numero de documento
 * son opcionales (ausentes = no cambian) y van juntos; el motivo deja rastro en la auditoria. Lo extienden las
 * solicitudes de edicion de coordinador (HU121), docente (HU092) y la correccion de estudiante o decano.
 */
public abstract class CorreccionDocumentoRequest {
    private UUID idTipoIdentificacion;
    private Integer numeroIdentificacion;
    private String motivo;

    public UUID getIdTipoIdentificacion() { return idTipoIdentificacion; }
    public void setIdTipoIdentificacion(final UUID idTipoIdentificacion) { this.idTipoIdentificacion = idTipoIdentificacion; }
    public Integer getNumeroIdentificacion() { return numeroIdentificacion; }
    public void setNumeroIdentificacion(final Integer numeroIdentificacion) { this.numeroIdentificacion = numeroIdentificacion; }
    public String getMotivo() { return motivo; }
    public void setMotivo(final String motivo) { this.motivo = motivo; }

    /** {@code true} si la solicitud trae tipo o numero de documento. */
    public boolean solicitaCambioDocumento() {
        return idTipoIdentificacion != null || numeroIdentificacion != null;
    }
}
