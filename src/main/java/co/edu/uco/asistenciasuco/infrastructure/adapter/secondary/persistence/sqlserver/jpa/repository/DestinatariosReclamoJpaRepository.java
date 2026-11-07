package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.DestinatariosReclamoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioReclamoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador JPA para consultar destinatarios de notificaciones de reclamo de asistencia
 * mediante {@code dbo.usp_consultar_destinatarios_reclamo_asistencia}.
 */
@Repository
public class DestinatariosReclamoJpaRepository implements DestinatariosReclamoPort {

    static final String OP_DESTINATARIOS_SESION = "consultarDestinatariosReclamoPorSesion";
    static final String OP_DESTINATARIOS_SOLICITUD = "consultarDestinatariosReclamoPorSolicitud";

    static final String SQL_CONSULTAR_DESTINATARIOS = """
            EXEC dbo.usp_consultar_destinatarios_reclamo_asistencia
                 @idSesion = :idSesion,
                 @idEstudiante = :idEstudiante,
                 @idSolicitud = :idSolicitud,
                 @idCorrelacion = :idCorrelacion
            """;

    private final EntityManager entityManager;

    public DestinatariosReclamoJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public List<DestinatarioReclamoProjection> consultarDestinatariosPorSesion(
            final UUID idSesion,
            final UUID idEstudiante
    ) {
        final UUID correlationId = CorrelationIdContext.get() != null ? CorrelationIdContext.get() : UUID.randomUUID();
        return JpaQueryExecutor.execute(
                OP_DESTINATARIOS_SESION,
                "No fue posible consultar destinatarios del reclamo por sesion.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_CONSULTAR_DESTINATARIOS)
                        .setParameter("idSesion", idSesion)
                        .setParameter("idEstudiante", idEstudiante)
                        .setParameter("idSolicitud", null)
                        .setParameter("idCorrelacion", correlationId)
                        .getResultList()).stream()
                        .map(fila -> toProjection((Object[]) fila))
                        .toList()
        );
    }

    @Override
    public List<DestinatarioReclamoProjection> consultarDestinatariosPorSolicitud(final UUID idSolicitud) {
        final UUID correlationId = CorrelationIdContext.get() != null ? CorrelationIdContext.get() : UUID.randomUUID();
        return JpaQueryExecutor.execute(
                OP_DESTINATARIOS_SOLICITUD,
                "No fue posible consultar destinatarios del reclamo por solicitud.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_CONSULTAR_DESTINATARIOS)
                        .setParameter("idSesion", null)
                        .setParameter("idEstudiante", null)
                        .setParameter("idSolicitud", idSolicitud)
                        .setParameter("idCorrelacion", correlationId)
                        .getResultList()).stream()
                        .map(fila -> toProjection((Object[]) fila))
                        .toList()
        );
    }

    private static DestinatarioReclamoProjection toProjection(final Object[] fila) {
        final String rol = fila.length > 0 && fila[0] != null ? String.valueOf(fila[0]) : "";
        final UUID idUsuario = fila.length > 1 && fila[1] != null ? toUuid(fila[1]) : null;
        final String nombreCompleto = fila.length > 2 && fila[2] != null ? String.valueOf(fila[2]) : "";
        final String correo = fila.length > 3 && fila[3] != null ? String.valueOf(fila[3]) : null;
        final UUID idGrupo = fila.length > 4 && fila[4] != null ? toUuid(fila[4]) : null;
        final String nombreGrupo = fila.length > 5 && fila[5] != null ? String.valueOf(fila[5]) : "";
        final String fechaSesion = fila.length > 6 && fila[6] != null ? String.valueOf(fila[6]) : "";
        return new DestinatarioReclamoProjection(rol, idUsuario, nombreCompleto, correo, idGrupo, nombreGrupo, fechaSesion);
    }

    private static UUID toUuid(final Object val) {
        if (val instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(String.valueOf(val));
    }
}
