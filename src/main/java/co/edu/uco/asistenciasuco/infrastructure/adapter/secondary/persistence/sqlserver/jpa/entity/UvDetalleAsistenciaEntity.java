package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_detalle_asistencia}
 * (freeze DB). No es un objeto de dominio, no tiene relaciones y no expone API de escritura.
 * Solo se mapean las columnas que necesita la query piloto de asistencia.
 */
@Entity
@Immutable
@Table(name = "uv_detalle_asistencia", schema = "dbo")
public class UvDetalleAsistenciaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idAsistencia")
    private UUID idAsistencia;

    /** Consecutivo del detalle dentro de la asistencia; permite quedarse con el ultimo si hubiera varios. */
    @Column(name = "codigo")
    private Integer codigo;

    /** Nullable en el mapeo (defensivo): la semantica publica sigue siendo la de un flag {@code bit}. */
    @Column(name = "asistio")
    private Boolean asistio;

    @Column(name = "codigoRazonCausa")
    private String codigoRazonCausa;
    /** Requerido por el reporte de asistencia (JPA-05); no altera el piloto de asistencia. */
    @Column(name = "nombreRazonCausa")
    private String nombreRazonCausa;

    protected UvDetalleAsistenciaEntity() {
        // requerido por JPA
    }
}
