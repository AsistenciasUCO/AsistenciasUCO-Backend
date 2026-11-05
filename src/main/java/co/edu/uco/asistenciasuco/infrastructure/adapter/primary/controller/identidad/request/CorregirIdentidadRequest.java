package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request;

/**
 * Cuerpo de la correccion de identidad (HU159) de un estudiante o de un decano: nombres y apellidos, documento
 * opcional (tipo y numero juntos) y motivo obligatorio.
 */
public final class CorregirIdentidadRequest extends CorreccionDocumentoRequest {
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;

    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(final String primerNombre) { this.primerNombre = primerNombre; }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(final String segundoNombre) { this.segundoNombre = segundoNombre; }
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(final String primerApellido) { this.primerApellido = primerApellido; }
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(final String segundoApellido) { this.segundoApellido = segundoApellido; }
}
