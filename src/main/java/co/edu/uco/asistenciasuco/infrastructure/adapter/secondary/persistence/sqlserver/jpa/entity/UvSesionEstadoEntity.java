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
 * CULM-001 B.1 (§2.5, §3.2.4): proyeccion de solo lectura de {@code dbo.uv_sesion_estado} (una fila por sesion). Solo se
 * mapean las columnas de cancelacion que exponen las respuestas de sesion; las horas siguen viniendo de
 * {@code uv_sesion}/{@code uv_sesion_v2}. {@code fechaCancelacionUtc} es DATETIME2 en UTC y se lee sin pasar por la zona
 * de la JVM.
 */
@Entity
@Immutable
@Table(name = "uv_sesion_estado", schema = "dbo")
public class UvSesionEstadoEntity {
    @Id @Column(name = "idSesion") private UUID idSesion;
    @Column(name = "idGrupo") private UUID idGrupo;
    @Column(name = "estaCancelada") private Boolean estaCancelada;
    @Column(name = "codigoTipoCancelacion") private String codigoTipoCancelacion;
    @Column(name = "motivoCancelacion") private String motivoCancelacion;
    @Column(name = "nombreDiaFestivo") private String nombreDiaFestivo;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaCancelacionUtc") private LocalDateTime fechaCancelacionUtc;

    protected UvSesionEstadoEntity() { }

    public UUID idSesion() {
        return idSesion;
    }

    public UUID idGrupo() {
        return idGrupo;
    }

    public boolean estaCancelada() {
        return Boolean.TRUE.equals(estaCancelada);
    }

    public String codigoTipoCancelacion() {
        return codigoTipoCancelacion;
    }

    public String motivoCancelacion() {
        return motivoCancelacion;
    }

    public String nombreDiaFestivo() {
        return nombreDiaFestivo;
    }

    public LocalDateTime fechaCancelacionUtc() {
        return fechaCancelacionUtc;
    }
}
