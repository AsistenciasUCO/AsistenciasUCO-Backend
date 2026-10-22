package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request;

import java.util.UUID;

public final class CrearProgramaRequest {

    private String nombre;
    private Integer codigo;
    private UUID idTipoPrograma;
    private UUID idCoordinador;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(final String nombre) {
        this.nombre = nombre;
    }

    public Integer getCodigo() {
        return codigo;
    }

    public void setCodigo(final Integer codigo) {
        this.codigo = codigo;
    }

    public UUID getIdTipoPrograma() {
        return idTipoPrograma;
    }

    public void setIdTipoPrograma(final UUID idTipoPrograma) {
        this.idTipoPrograma = idTipoPrograma;
    }

    public UUID getIdCoordinador() {
        return idCoordinador;
    }

    public void setIdCoordinador(final UUID idCoordinador) {
        this.idCoordinador = idCoordinador;
    }
}
