package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

/**
 * Fila de resultado de {@code dbo.usp_consultar_solicitudes_revision_docente} (LB-004D, R2). No es una tabla ni
 * una vista. Los DATETIME2(7) se leen con {@link LocalDateTimeJdbcType} (sin zona de la JVM).
 */
@Entity
public class SpSolicitudRevisionDocenteEntity extends SpSolicitudRevisionDocenteBaseEntity {
    @Column(name = "totalRegistros") private Integer totalRegistros;

    protected SpSolicitudRevisionDocenteEntity() { }

    public Integer totalRegistros() { return totalRegistros; }
}
