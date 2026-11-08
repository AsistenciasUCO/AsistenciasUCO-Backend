package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request;

import java.util.UUID;

/**
 * Cuerpo de {@code POST /api/v1/admin/estudiantes/admision} (CULM-001 D.4, HU156). {@code password} es opcional: solo se exige
 * para una persona nueva y la decide el caso de uso. Su representacion de texto no imprime la clave (OBS-12).
 */
public final class AdmitirEstudianteRequest {
    private UUID tipoIdentificacionId;
    private Integer numeroIdentificacion;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String correo;
    private String password;
    private UUID idPrograma;

    public UUID getTipoIdentificacionId() { return tipoIdentificacionId; }
    public void setTipoIdentificacionId(final UUID tipoIdentificacionId) { this.tipoIdentificacionId = tipoIdentificacionId; }
    public Integer getNumeroIdentificacion() { return numeroIdentificacion; }
    public void setNumeroIdentificacion(final Integer numeroIdentificacion) { this.numeroIdentificacion = numeroIdentificacion; }
    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(final String primerNombre) { this.primerNombre = primerNombre; }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(final String segundoNombre) { this.segundoNombre = segundoNombre; }
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(final String primerApellido) { this.primerApellido = primerApellido; }
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(final String segundoApellido) { this.segundoApellido = segundoApellido; }
    public String getCorreo() { return correo; }
    public void setCorreo(final String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(final String password) { this.password = password; }
    public UUID getIdPrograma() { return idPrograma; }
    public void setIdPrograma(final UUID idPrograma) { this.idPrograma = idPrograma; }

    @Override
    public String toString() {
        return "AdmitirEstudianteRequest[idPrograma=" + idPrograma + ", primerNombre=" + primerNombre + "]";
    }
}
