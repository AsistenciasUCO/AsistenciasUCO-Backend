package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

/**
 * Fila de resultado de {@code dbo.usp_consultar_detalle_solicitud_revision_docente} (HU036). No es una tabla ni
 * una vista. Los DATETIME2(7) se leen con {@link LocalDateTimeJdbcType} (sin zona de la JVM).
 */
@Entity
public class SpDetalleSolicitudRevisionDocenteEntity extends SpSolicitudRevisionDocenteBaseEntity {
    @Column(name = "asistio") private Boolean asistio;
    @Column(name = "codigoRazonCausa") private String codigoRazonCausa;
    @Column(name = "nombreRazonCausa") private String nombreRazonCausa;

    protected SpDetalleSolicitudRevisionDocenteEntity() { }

    public Boolean asistio() { return asistio; }
    public String codigoRazonCausa() { return codigoRazonCausa; }
    public String nombreRazonCausa() { return nombreRazonCausa; }
}
