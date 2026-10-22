package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.TipoProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvTipoProgramaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/** Consulta JPA-only de {@code uv_tipo_programa} (HU076). */
@Repository
public class TipoProgramaJpaRepository implements TipoProgramaQueryPort {

    static final String HQL_LISTAR_ACTIVOS = """
            select tp
            from UvTipoProgramaEntity tp
            where tp.estaActivoTipoPrograma = true
            order by tp.nombre, tp.id
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
                () -> entityManager.createQuery(HQL_LISTAR_ACTIVOS, UvTipoProgramaEntity.class)
                        .getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toTipoPrograma)
                        .toList()
        );
    }
}
