package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaDocenteQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaDocenteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Consulta JPA-only de la asignacion academica del docente sobre {@code uv_docente} (LB-008 JPA-05). */
@Repository
public class AsignaturaDocenteJpaRepository implements AsignaturaDocenteQueryPort {

    static final String HQL_POR_DOCENTE = """
            select distinct d
            from UvDocenteEntity d
            where d.id = :idDocente
            order by d.nombreAsignatura, d.nombreGrupo
            """;

    /** Conteo de estudiantes activos del grupo desde la vista autorizada {@code uv_grupo} (mismo dato que el coordinador). */
    static final String HQL_ESTUDIANTES_ACTIVOS = """
            select g.id, g.estudiantesActivos
            from UvGrupoEntity g
            where g.id in :idsGrupo
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
                    final Map<UUID, Integer> activos = estudiantesActivos(filas);
                    return filas.stream()
                            .map(fila -> AcademicViewJpaProjectionMapper.toAsignaturaDocente(fila,
                                    fila.idGrupo() == null ? null : activos.get(fila.idGrupo())))
                            .toList();
                }
        );
    }

    private Map<UUID, Integer> estudiantesActivos(final List<UvDocenteEntity> filas) {
        final List<UUID> idsGrupo = filas.stream().map(UvDocenteEntity::idGrupo).filter(Objects::nonNull).distinct().toList();
        final Map<UUID, Integer> activos = new HashMap<>();
        if (idsGrupo.isEmpty()) {
            return activos;
        }
        for (final Object[] fila : entityManager.createQuery(HQL_ESTUDIANTES_ACTIVOS, Object[].class)
                .setParameter("idsGrupo", idsGrupo)
                .getResultList()) {
            activos.put((UUID) fila[0], (Integer) fila[1]);
        }
        return activos;
    }
}
