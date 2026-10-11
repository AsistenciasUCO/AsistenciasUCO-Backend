package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvSesionV2Entity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Lectura v2 de sesiones sobre {@code dbo.uv_sesion_v2} (UTC-D06-POST-FREEZE). Horas y procedencia
 * provienen de la misma fila; la decision CONFIRMADA/INDETERMINADA vive en Application.
 */
@Repository
public class SesionProcedenciaJpaRepository implements SesionProcedenciaQueryPort {

    private static final String SELECT_ROW = """
            select s
            from UvSesionV2Entity s
            """;
    static final String HQL_POR_ID = SELECT_ROW + " where s.id = :idSesion order by s.id";
    // Mismo orden que v1 (uv_sesion), con id como desempate estable para filas sin numero distinto.
    static final String HQL_POR_GRUPO = SELECT_ROW
            + " where s.idGrupo = :idGrupo order by s.fechaHoraInicio, s.numero, s.id";

    private final EntityManager entityManager;

    public SesionProcedenciaJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public Optional<SesionProcedenciaRepositoryProjection> consultarSesion(final UUID sesionId) {
        require(sesionId, "El identificador de la sesion v2 es obligatorio.");
        return JpaQueryExecutor.execute(
                "consultarSesionV2",
                "No fue posible consultar la sesion v2 desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_ID, UvSesionV2Entity.class)
                        .setParameter("idSesion", sesionId)
                        .setMaxResults(1)
                        .getResultList().stream()
                        .findFirst()
                        .map(SesionProcedenciaJpaRepository::toProjection)
        );
    }

    @Override
    public List<SesionProcedenciaRepositoryProjection> consultarSesionesPorGrupo(final UUID grupoId) {
        require(grupoId, "El identificador del grupo para consultar sesiones v2 es obligatorio.");
        return JpaQueryExecutor.execute(
                "consultarSesionesPorGrupoV2",
                "No fue posible consultar las sesiones v2 del grupo desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_GRUPO, UvSesionV2Entity.class)
                        .setParameter("idGrupo", grupoId)
                        .getResultList().stream()
                        .map(SesionProcedenciaJpaRepository::toProjection)
                        .toList()
        );
    }

    static SesionProcedenciaRepositoryProjection toProjection(final UvSesionV2Entity row) {
        return new SesionProcedenciaRepositoryProjection(
                row.id(), row.idGrupo(), row.nombre(), row.numero(), row.codigo(), row.numeroSemana(),
                row.codigoGrupo() == null ? null : String.valueOf(row.codigoGrupo()), row.nombreGrupo(),
                row.fechaHoraInicio(), row.fechaHoraFin(), row.procedenciaTemporal()
        );
    }

    private static void require(final Object value, final String message) {
        if (ObjectHelper.isNull(value)) throw new CrosscuttingException(message);
    }
}
