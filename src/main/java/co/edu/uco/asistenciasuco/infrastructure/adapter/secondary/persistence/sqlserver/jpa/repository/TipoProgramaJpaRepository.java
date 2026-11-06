package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.TipoProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvTipoProgramaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * Lectura de {@code uv_tipo_programa} (HU023 / HU154). Regla de John (2026-11-04): los tipos de programa son un
 * catalogo de solo consulta; se cargan por seed y no hay comandos de escritura.
 */
@Repository
public class TipoProgramaJpaRepository implements TipoProgramaQueryPort {

    static final String HQL_LISTAR = """
            select t
            from UvTipoProgramaEntity t
            order by t.nombre, t.id
            """;

    private final EntityManager entityManager;

    public TipoProgramaJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de tipos de programa es obligatorio.");
    }

    @Override
    public List<TipoProgramaProjection> consultarTiposPrograma() {
        return JpaQueryExecutor.execute(
                "consultarTiposPrograma",
                "No fue posible consultar los tipos de programa desde base de datos.",
                () -> entityManager.createQuery(HQL_LISTAR, UvTipoProgramaEntity.class).getResultList().stream()
                        .map(row -> new TipoProgramaProjection(
                                row.id(), row.codigo(), row.nombre(), row.estaActivoTipoPrograma()))
                        .toList()
        );
    }
}
