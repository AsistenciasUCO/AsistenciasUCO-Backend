package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AreaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvAreaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * Consulta JPA de {@code uv_area} (LB-008 JPA-05). Regla de John (2026-11-04): las areas son un catalogo de solo
 * consulta; se cargan por seed y no hay comandos de escritura.
 */
@Repository
public class AreaJpaRepository implements AreaQueryPort {

    static final String HQL_LISTAR = """
            select a
            from UvAreaEntity a
            order by a.nombre, a.id
            """;

    private final EntityManager entityManager;

    public AreaJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de areas es obligatorio.");
    }

    @Override
    public List<AreaProjection> consultarAreas() {
        return JpaQueryExecutor.execute(
                "consultarAreas",
                "No fue posible consultar las areas desde base de datos.",
                () -> entityManager.createQuery(HQL_LISTAR, UvAreaEntity.class).getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toArea)
                        .toList()
        );
    }
}
