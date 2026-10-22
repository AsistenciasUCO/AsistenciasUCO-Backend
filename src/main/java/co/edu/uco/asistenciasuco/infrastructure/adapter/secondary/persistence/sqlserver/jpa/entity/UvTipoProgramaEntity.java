package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_tipo_programa} (HU076).
 */
@Entity
@Immutable
@Table(name = "uv_tipo_programa", schema = "dbo")
public class UvTipoProgramaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "estaActivoTipoPrograma")
    private Boolean estaActivoTipoPrograma;

    @Column(name = "estaActivoTextoTipoPrograma")
    private String estaActivoTextoTipoPrograma;

    protected UvTipoProgramaEntity() {
        // requerido por JPA
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public Boolean estaActivoTipoPrograma() {
        return estaActivoTipoPrograma;
    }

    public String estaActivoTextoTipoPrograma() {
        return estaActivoTextoTipoPrograma;
    }
}
