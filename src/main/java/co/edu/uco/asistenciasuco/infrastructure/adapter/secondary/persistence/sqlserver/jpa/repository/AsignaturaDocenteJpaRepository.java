package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaDocenteQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaDocenteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvProgramaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consulta JPA-only de la asignacion academica del docente sobre {@code uv_docente} (LB-008 JPA-05). HU058: agrega el
 * tipo de programa (nivel formativo) de cada grupo desde {@code uv_programa}, sin cambiar la vista del docente.
 */
@Repository
public class AsignaturaDocenteJpaRepository implements AsignaturaDocenteQueryPort {

    static final String HQL_POR_DOCENTE = """
            select distinct d
            from UvDocenteEntity d
            where d.id = :idDocente
            order by d.nombreAsignatura, d.nombreGrupo
            """;

    /** HU058: tipo de programa (nivel formativo) de los programas de los grupos del docente. */
    static final String HQL_PROGRAMAS = """
            select p
            from UvProgramaEntity p
            where p.id in :ids
            """;

    private final EntityManager entityManager;

    public AsignaturaDocenteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de asignaturas de docente es obligatorio.");
    }

    @Override
    public List<AsignaturaDocenteProjection> consultarAsignaturasDocente(final UUID idDocente) {
        return JpaQueryExecutor.execute(
                "consultarAsignaturasDocente",
                "No fue posible consultar las asignaturas del docente desde base de datos.",
                () -> {
                    final List<UvDocenteEntity> filas = entityManager.createQuery(HQL_POR_DOCENTE, UvDocenteEntity.class)
                            .setParameter("idDocente", idDocente)
                            .getResultList();
                    final Map<UUID, UvProgramaEntity> programas = programas(filas);
                    return filas.stream()
                            .map(fila -> AcademicViewJpaProjectionMapper.toAsignaturaDocente(fila,
                                    fila.idPrograma() == null ? null : programas.get(fila.idPrograma())))
                            .toList();
                }
        );
    }

    private Map<UUID, UvProgramaEntity> programas(final List<UvDocenteEntity> filas) {
        final Set<UUID> ids = filas.stream().map(UvDocenteEntity::idPrograma).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return entityManager.createQuery(HQL_PROGRAMAS, UvProgramaEntity.class)
                .setParameter("ids", ids)
                .getResultList().stream()
                .collect(Collectors.toMap(UvProgramaEntity::id, Function.identity(), (a, b) -> a));
    }
}
