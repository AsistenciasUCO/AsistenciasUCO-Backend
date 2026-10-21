package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila de resultado de {@code dbo.usp_consultar_archivo_soporte_autorizado} (LB-004D, R1). No es una tabla
 * ni una vista: solo da un tipo a las columnas del resultset. Los instantes UTC se leen con
 * {@link LocalDateTimeJdbcType} ({@code ResultSet#getObject(int, Class)}), de modo que el DATETIME2(7) llega
 * tal cual, con sus 7 decimales y sin pasar por un {@code Timestamp} en la zona por defecto de la JVM.
 */
@Entity
public class SpSoporteArchivoAutorizadoEntity {
    @Id @Column(name = "idArchivoSoporte") private UUID idArchivoSoporte;
    @Column(name = "estado") private String estado;
    @Column(name = "nombreOriginal") private String nombreOriginal;
    @Column(name = "tipoContenido") private String tipoContenido;
    @Column(name = "tamanioBytes") private Long tamanioBytes;
    @Column(name = "checksumSha256") private String checksumSha256;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaRegistroUtc") private LocalDateTime fechaRegistroUtc;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaAdjuncionUtc") private LocalDateTime fechaAdjuncionUtc;
    @Column(name = "idSolicitudRevisionAsistencia") private UUID idSolicitudRevisionAsistencia;
    @Column(name = "relacionAcceso") private String relacionAcceso;

    protected SpSoporteArchivoAutorizadoEntity() { }

    public UUID idArchivoSoporte() { return idArchivoSoporte; }
    public String estado() { return estado; }
    public String nombreOriginal() { return nombreOriginal; }
    public String tipoContenido() { return tipoContenido; }
    public Long tamanioBytes() { return tamanioBytes; }
    public String checksumSha256() { return checksumSha256; }
    public LocalDateTime fechaRegistroUtc() { return fechaRegistroUtc; }
    public LocalDateTime fechaAdjuncionUtc() { return fechaAdjuncionUtc; }
    public UUID idSolicitudRevisionAsistencia() { return idSolicitudRevisionAsistencia; }
    public String relacionAcceso() { return relacionAcceso; }
}
