package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request;

/** CULM-001 B.1 (HU047): cuerpo de {@code PATCH /api/v1/docente/sesiones/{sesionId}/cancelar}. */
public final class CancelarSesionRequest {

    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(final String motivo) {
        this.motivo = motivo;
    }
}
