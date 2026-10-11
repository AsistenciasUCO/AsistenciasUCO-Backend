package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "uv_sesion", schema = "dbo")
public class UvSesionEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "idGrupo") private UUID idGrupo;
    @Column(name = "nombre") private String nombre;
    @Column(name = "numero") private Integer numero;
    @Column(name = "codigo") private String codigo;
    @Column(name = "numeroSemana") private Integer numeroSemana;
    @Column(name = "codigoGrupo") private Integer codigoGrupo;
    @Column(name = "nombreGrupo") private String nombreGrupo;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaHoraInicio") private LocalDateTime fechaHoraInicio;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaHoraFin") private LocalDateTime fechaHoraFin;
    protected UvSesionEntity() { }

    public UUID id() {
        return id;
    }

    public UUID idGrupo() {
        return idGrupo;
    }

    public String nombre() {
        return nombre;
    }

    public Integer numero() {
        return numero;
    }

    public String codigo() {
        return codigo;
    }

    public Integer numeroSemana() {
        return numeroSemana;
    }

    public Integer codigoGrupo() {
        return codigoGrupo;
    }

    public String nombreGrupo() {
        return nombreGrupo;
    }

    public LocalDateTime fechaHoraInicio() {
        return fechaHoraInicio;
    }

    public LocalDateTime fechaHoraFin() {
        return fechaHoraFin;
    }
}

