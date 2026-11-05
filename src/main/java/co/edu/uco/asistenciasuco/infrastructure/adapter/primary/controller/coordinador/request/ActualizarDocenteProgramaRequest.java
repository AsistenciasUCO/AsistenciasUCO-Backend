package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request.CorreccionDocumentoRequest;

import java.util.UUID;

/**
 * Cuerpo de {@code PUT /api/v1/coordinador/docentes/{idDocente}}. Los campos ausentes no se modifican; el correo no se
 * edita. HU159 (2026-11-05): el tipo y el numero de documento son opcionales y, si cambian, exigen motivo.
 */
public final class ActualizarDocenteProgramaRequest extends CorreccionDocumentoRequest {
    private UUID idTipoVinculacion;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;

    public UUID getIdTipoVinculacion() { return idTipoVinculacion; }
    public void setIdTipoVinculacion(final UUID idTipoVinculacion) { this.idTipoVinculacion = idTipoVinculacion; }
    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(final String primerNombre) { this.primerNombre = primerNombre; }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(final String segundoNombre) { this.segundoNombre = segundoNombre; }
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(final String primerApellido) { this.primerApellido = primerApellido; }
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(final String segundoApellido) { this.segundoApellido = segundoApellido; }
}
