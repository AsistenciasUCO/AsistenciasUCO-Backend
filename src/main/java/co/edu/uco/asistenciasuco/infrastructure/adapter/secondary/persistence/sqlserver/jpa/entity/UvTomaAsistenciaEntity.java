package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CULM-001 B.2 (contrato 2.5, 3.3): proyeccion de solo lectura de {@code dbo.uv_toma_asistencia} (una fila por toma). Se
 * usa para resolver el grupo de la toma (titularidad en Application) y releer su estado tras un comando. Las fechas son
 * DATETIME2 en UTC y se leen sin pasar por la zona de la JVM.
 */
@Entity
@Immutable
@Table(name = "uv_toma_asistencia", schema = "dbo")
public class UvTomaAsistenciaEntity {
    @Id @Column(name = "idToma") private UUID idToma;
    @Column(name = "idSesion") private UUID idSesion;
    @Column(name = "idGrupo") private UUID idGrupo;
    @Column(name = "numero") private Short numero;
    @Column(name = "codigoModo") private String codigoModo;
    @Column(name = "codigoEstado") private String codigoEstado;
    @Column(name = "estaAbierta") private Boolean estaAbierta;
    @Column(name = "esImplicita") private Boolean esImplicita;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaAperturaUtc") private LocalDateTime fechaAperturaUtc;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaCierreUtc") private LocalDateTime fechaCierreUtc;
    @Column(name = "horasToma") private BigDecimal horasToma;
    @Column(name = "totalRegistros") private Integer totalRegistros;
    @Column(name = "totalPresentes") private Integer totalPresentes;
    @Column(name = "totalAusentes") private Integer totalAusentes;
    @Column(name = "totalExcusas") private Integer totalExcusas;
    @Column(name = "tieneQrActivo") private Boolean tieneQrActivo;

    protected UvTomaAsistenciaEntity() { }

    public UUID idToma() { return idToma; }
    public UUID idSesion() { return idSesion; }
    public UUID idGrupo() { return idGrupo; }
    public Short numero() { return numero; }
    public String codigoModo() { return codigoModo; }
    public String codigoEstado() { return codigoEstado; }
    public Boolean estaAbierta() { return estaAbierta; }
    public Boolean esImplicita() { return esImplicita; }
    public LocalDateTime fechaAperturaUtc() { return fechaAperturaUtc; }
    public LocalDateTime fechaCierreUtc() { return fechaCierreUtc; }
    public BigDecimal horasToma() { return horasToma; }
    public Integer totalRegistros() { return totalRegistros; }
    public Integer totalPresentes() { return totalPresentes; }
    public Integer totalAusentes() { return totalAusentes; }
    public Integer totalExcusas() { return totalExcusas; }
    public Boolean tieneQrActivo() { return tieneQrActivo; }
}
