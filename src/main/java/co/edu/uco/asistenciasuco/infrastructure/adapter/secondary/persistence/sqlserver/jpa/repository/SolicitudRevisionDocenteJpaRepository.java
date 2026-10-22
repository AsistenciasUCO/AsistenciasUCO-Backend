package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DetalleSolicitudRevisionDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionDocenteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpDetalleSolicitudRevisionDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpSolicitudRevisionDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Solicitudes de revision del docente titular (LB-004D, R2) por {@code usp_consultar_solicitudes_revision_docente}.
 * La paginacion es real (OFFSET/FETCH en SQL) y la titularidad se filtra en la DB por {@code Docente.usuario}.
 */
@Repository
public class SolicitudRevisionDocenteJpaRepository implements SolicitudRevisionDocenteQueryPort {

    static final String OP_CONSULTAR = "consultarSolicitudesRevisionDocente";

    static final String SQL_CONSULTAR = """
            EXEC dbo.usp_consultar_solicitudes_revision_docente
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @codigoEstado = :codigoEstado,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;

    static final String OP_DETALLE = "consultarDetalleSolicitudRevisionDocente";

    static final String SQL_DETALLE = """
            EXEC dbo.usp_consultar_detalle_solicitud_revision_docente
                 @idSolicitud = :idSolicitud,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;

    private final EntityManager entityManager;

    public SolicitudRevisionDocenteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public List<SolicitudRevisionDocenteProjection> consultarSolicitudesDocente(
            final UUID usuarioEjecutor,
            final String codigoEstado,
            final int numeroPagina,
            final int tamanoPagina
    ) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_CONSULTAR,
                "No fue posible consultar las solicitudes de revision del docente desde base de datos.",
                () -> entityManager.createNativeQuery(SQL_CONSULTAR, SpSolicitudRevisionDocenteEntity.class)
                        .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                        .setParameter("idCorrelacion", correlationId)
                        .setParameter("codigoEstado", codigoEstado)
                        .setParameter("numeroPagina", numeroPagina)
                        .setParameter("tamanoPagina", tamanoPagina)
                        .getResultList().stream()
                        .map(fila -> toProjection((SpSolicitudRevisionDocenteEntity) fila))
                        .toList()
        );
    }

    @Override
    public Optional<DetalleSolicitudRevisionDocenteProjection> consultarDetalleSolicitudDocente(
            final UUID idSolicitud,
            final UUID usuarioEjecutor
    ) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_DETALLE,
                "No fue posible consultar el detalle de la solicitud de revision del docente desde base de datos.",
                () -> entityManager.createNativeQuery(SQL_DETALLE, SpDetalleSolicitudRevisionDocenteEntity.class)
                        .setParameter("idSolicitud", idSolicitud)
                        .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                        .setParameter("idCorrelacion", correlationId)
                        .getResultList().stream()
                        .map(fila -> toDetalleProjection((SpDetalleSolicitudRevisionDocenteEntity) fila))
                        .findFirst()
        );
    }

    private static DetalleSolicitudRevisionDocenteProjection toDetalleProjection(
            final SpDetalleSolicitudRevisionDocenteEntity fila
    ) {
        final String procedencia = fila.procedenciaTemporalSesion();
        return new DetalleSolicitudRevisionDocenteProjection(
                fila.idSolicitudRevisionAsistencia(),
                fila.nombreSolicitud(),
                fila.fechaSolicitud(),
                fila.codigoEstado(),
                fila.nombreEstado(),
                fila.justificacionSolicitud(),
                fila.justificacionRespuesta(),
                fila.idEstudiante(),
                fila.nombreEstudiante(),
                fila.idSesion(),
                fila.numeroSesion(),
                // Solo se afirma un instante UTC cuando la DB conoce la procedencia (contrato UTC v2).
                procedencia == null ? null : SoporteArchivoJpaRepository.toUtcInstant(fila.fechaHoraInicioSesion()),
                procedencia,
                fila.idGrupo(),
                fila.codigoGrupo(),
                fila.nombreGrupo(),
                fila.idAsignatura(),
                fila.codigoAsignatura(),
                fila.nombreAsignatura(),
                fila.asistio(),
                fila.codigoRazonCausa(),
                fila.nombreRazonCausa(),
                fila.idArchivoSoporte(),
                fila.nombreSoporte(),
                fila.tipoContenidoSoporte(),
                fila.tamanioSoporteBytes(),
                SoporteArchivoJpaRepository.toUtcInstant(fila.fechaRegistroSoporteUtc())
        );
    }

    private static SolicitudRevisionDocenteProjection toProjection(final SpSolicitudRevisionDocenteEntity fila) {
        final String procedencia = fila.procedenciaTemporalSesion();
        return new SolicitudRevisionDocenteProjection(
                fila.idSolicitudRevisionAsistencia(),
                fila.nombreSolicitud(),
                fila.fechaSolicitud(),
                fila.codigoEstado(),
                fila.nombreEstado(),
                fila.justificacionSolicitud(),
                fila.justificacionRespuesta(),
                fila.idEstudiante(),
                fila.nombreEstudiante(),
                fila.idSesion(),
                fila.numeroSesion(),
                // Solo se afirma un instante UTC cuando la DB conoce la procedencia (contrato UTC v2).
                procedencia == null ? null : SoporteArchivoJpaRepository.toUtcInstant(fila.fechaHoraInicioSesion()),
                procedencia,
                fila.idGrupo(),
                fila.codigoGrupo(),
                fila.nombreGrupo(),
                fila.idAsignatura(),
                fila.codigoAsignatura(),
                fila.nombreAsignatura(),
                fila.idArchivoSoporte(),
                fila.nombreSoporte(),
                fila.tipoContenidoSoporte(),
                fila.tamanioSoporteBytes(),
                SoporteArchivoJpaRepository.toUtcInstant(fila.fechaRegistroSoporteUtc()),
                fila.totalRegistros() == null ? 0 : fila.totalRegistros()
        );
    }
}
