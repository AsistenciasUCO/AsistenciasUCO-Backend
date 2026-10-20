package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila de resultado de {@code dbo.usp_consultar_solicitudes_revision_docente} (LB-004D, R2). No es una tabla ni
 * una vista. Los DATETIME2(7) se leen con {@link LocalDateTimeJdbcType} (sin zona de la JVM).
 */
@Entity
public class SpSolicitudRevisionDocenteEntity {
    @Id @Column(name = "idSolicitudRevisionAsistencia") private UUID idSolicitudRevisionAsistencia;
    @Column(name = "nombreSolicitud") private String nombreSolicitud;
    @Column(name = "fechaSolicitud") private LocalDate fechaSolicitud;
    @Column(name = "codigoEstado") private String codigoEstado;
    @Column(name = "nombreEstado") private String nombreEstado;
    @Column(name = "justificacionSolicitud") private String justificacionSolicitud;
    @Column(name = "justificacionRespuesta") private String justificacionRespuesta;
    @Column(name = "idEstudiante") private UUID idEstudiante;
    @Column(name = "nombreEstudiante") private String nombreEstudiante;
    @Column(name = "idSesion") private UUID idSesion;
    @Column(name = "numeroSesion") private Integer numeroSesion;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaHoraInicioSesion") private LocalDateTime fechaHoraInicioSesion;
    @Column(name = "procedenciaTemporalSesion") private String procedenciaTemporalSesion;
    @Column(name = "idGrupo") private UUID idGrupo;
    @Column(name = "codigoGrupo") private Integer codigoGrupo;
    @Column(name = "nombreGrupo") private String nombreGrupo;
    @Column(name = "idAsignatura") private UUID idAsignatura;
    @Column(name = "codigoAsignatura") private String codigoAsignatura;
    @Column(name = "nombreAsignatura") private String nombreAsignatura;
    @Column(name = "idArchivoSoporte") private UUID idArchivoSoporte;
    @Column(name = "nombreSoporte") private String nombreSoporte;
    @Column(name = "tipoContenidoSoporte") private String tipoContenidoSoporte;
    @Column(name = "tamanioSoporteBytes") private Long tamanioSoporteBytes;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaRegistroSoporteUtc") private LocalDateTime fechaRegistroSoporteUtc;
    @Column(name = "totalRegistros") private Integer totalRegistros;

    protected SpSolicitudRevisionDocenteEntity() { }

    public UUID idSolicitudRevisionAsistencia() { return idSolicitudRevisionAsistencia; }
    public String nombreSolicitud() { return nombreSolicitud; }
    public LocalDate fechaSolicitud() { return fechaSolicitud; }
    public String codigoEstado() { return codigoEstado; }
    public String nombreEstado() { return nombreEstado; }
    public String justificacionSolicitud() { return justificacionSolicitud; }
    public String justificacionRespuesta() { return justificacionRespuesta; }
    public UUID idEstudiante() { return idEstudiante; }
    public String nombreEstudiante() { return nombreEstudiante; }
    public UUID idSesion() { return idSesion; }
    public Integer numeroSesion() { return numeroSesion; }
    public LocalDateTime fechaHoraInicioSesion() { return fechaHoraInicioSesion; }
    public String procedenciaTemporalSesion() { return procedenciaTemporalSesion; }
    public UUID idGrupo() { return idGrupo; }
    public Integer codigoGrupo() { return codigoGrupo; }
    public String nombreGrupo() { return nombreGrupo; }
    public UUID idAsignatura() { return idAsignatura; }
    public String codigoAsignatura() { return codigoAsignatura; }
    public String nombreAsignatura() { return nombreAsignatura; }
    public UUID idArchivoSoporte() { return idArchivoSoporte; }
    public String nombreSoporte() { return nombreSoporte; }
    public String tipoContenidoSoporte() { return tipoContenidoSoporte; }
    public Long tamanioSoporteBytes() { return tamanioSoporteBytes; }
    public LocalDateTime fechaRegistroSoporteUtc() { return fechaRegistroSoporteUtc; }
    public Integer totalRegistros() { return totalRegistros; }
}
