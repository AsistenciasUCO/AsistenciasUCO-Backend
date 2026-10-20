package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_solicitud_revision_asistencia} (freeze DB).
 * Solo se mapean las columnas necesarias para saber si una asistencia ya tiene una solicitud de revision y en que estado.
 */
@Entity
@Immutable
@Table(name = "uv_solicitud_revision_asistencia", schema = "dbo")
public class UvSolicitudRevisionAsistenciaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idAsistencia")
    private UUID idAsistencia;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "nombreEstado")
    private String nombreEstado;

    protected UvSolicitudRevisionAsistenciaEntity() {
        // requerido por JPA
    }
}
