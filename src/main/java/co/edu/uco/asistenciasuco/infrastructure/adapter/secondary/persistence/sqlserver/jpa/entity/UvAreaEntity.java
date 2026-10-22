package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_area} (LB-008 JPA-05).
 */
@Entity
@Immutable
@Table(name = "uv_area", schema = "dbo")
public class UvAreaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "codigo")
    private String codigo;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "totalAsignaturas")
    private Long totalAsignaturas;

    protected UvAreaEntity() {
        // requerido por JPA
    }

    public UUID id() {
        return id;
    }

    public String codigo() {
        return codigo;
    }

    public String nombre() {
        return nombre;
    }

    public Long totalAsignaturas() {
        return totalAsignaturas;
    }
}

