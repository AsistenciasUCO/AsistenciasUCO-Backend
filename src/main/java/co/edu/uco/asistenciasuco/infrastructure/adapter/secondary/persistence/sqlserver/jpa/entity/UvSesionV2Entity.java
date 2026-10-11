package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Proyeccion de solo lectura de {@code dbo.uv_sesion_v2} (UTC-D06-POST-FREEZE): las diez columnas de
 * {@code uv_sesion} mas {@code procedenciaTemporal}, leidas de la misma fila de {@code dbo.Sesion}.
 *
 * <p>Las horas se leen con {@link LocalDateTimeJdbcType} ({@code ResultSet#getObject(int, Class)}):
 * el DATETIME2(7) llega tal cual, con sus 7 decimales y sin pasar por un {@code Timestamp} construido
 * en la zona por defecto de la JVM. {@code UvSesionEntity} (v1) no se modifica.</p>
 */
@Entity
@Immutable
@Table(name = "uv_sesion_v2", schema = "dbo")
public class UvSesionV2Entity {
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
    @Column(name = "procedenciaTemporal") private String procedenciaTemporal;
    protected UvSesionV2Entity() { }

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

    public String procedenciaTemporal() {
        return procedenciaTemporal;
    }
}
