package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_programa} (HU076).
 */
@Entity
@Immutable
@Table(name = "uv_programa", schema = "dbo")
public class UvProgramaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nombrePrograma")
    private String nombrePrograma;

    @Column(name = "idFacultad")
    private UUID idFacultad;

    @Column(name = "nombreFacultad")
    private String nombreFacultad;

    @Column(name = "idInstitucion")
    private UUID idInstitucion;

    @Column(name = "nombreInstitucion")
    private String nombreInstitucion;

    @Column(name = "idCoordinador")
    private UUID idCoordinador;

    @Column(name = "nombreCoordinador")
    private String nombreCoordinador;

    @Column(name = "estaActivoPrograma")
    private Boolean estaActivoPrograma;

    @Column(name = "estaActivoTextoPrograma")
    private String estaActivoTextoPrograma;

    @Column(name = "idTipoPrograma")
    private UUID idTipoPrograma;

    @Column(name = "nombreTipoPrograma")
    private String nombreTipoPrograma;

    protected UvProgramaEntity() {
        // requerido por JPA
    }

    public UUID id() {
        return id;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }

    public UUID idFacultad() {
        return idFacultad;
    }

    public String nombreFacultad() {
        return nombreFacultad;
    }

    public UUID idInstitucion() {
        return idInstitucion;
    }

    public String nombreInstitucion() {
        return nombreInstitucion;
    }

    public UUID idCoordinador() {
        return idCoordinador;
    }

    public String nombreCoordinador() {
        return nombreCoordinador;
    }

    public Boolean estaActivoPrograma() {
        return estaActivoPrograma;
    }

    public String estaActivoTextoPrograma() {
        return estaActivoTextoPrograma;
    }

    public UUID idTipoPrograma() {
        return idTipoPrograma;
    }

    public String nombreTipoPrograma() {
        return nombreTipoPrograma;
    }
}
