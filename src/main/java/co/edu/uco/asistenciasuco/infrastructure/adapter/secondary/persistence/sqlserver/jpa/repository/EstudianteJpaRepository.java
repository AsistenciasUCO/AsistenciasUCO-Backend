package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.*;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvEstudianteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteResumenQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EstudianteJpaRepository implements EstudianteRepositoryPort {
    private static final String BASE_FROM = " from UvEstudianteIdentidadEntity e join UvUsuarioEntity u on e.idUsuario = u.id";
    private static final String SELECT_RESUMEN = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteResumenQueryRow(
                e.id, u.id, u.idTipoIdentificacion, u.numeroIdentificacion, u.primerApellido,
                u.segundoApellido, u.primerNombre, u.segundoNombre, u.nombreCompleto, u.correo,
                u.estaActivoUsuario)
            """;
    private static final String ORDER = " order by u.primerApellido, u.primerNombre, u.numeroIdentificacion, e.id";
    /**
     * Filtros opcionales con texto JPQL constante: un filtro ausente se enlaza como {@code null} y
     * su predicado queda neutro. Ningun valor del usuario se concatena en la consulta (java:S2077).
     */
    private static final String FILTERS = """
             where (:tipoIdentificacionId is null or u.idTipoIdentificacion = :tipoIdentificacionId)
               and (:numeroIdentificacion is null or u.numeroIdentificacion = :numeroIdentificacion)
               and (:nombre is null or upper(u.nombreCompleto) like :nombre)
               and (:correo is null or lower(u.correo) like :correo)
               and (:activo is null or u.estaActivoUsuario = :activo)
               and ((:institucionId is null and :facultadId is null and :programaId is null and :grupoId is null)
                    or exists (select 1 from UvEstudianteEntity a where a.id = e.id
                        and (:institucionId is null or a.idInstitucion = :institucionId)
                        and (:facultadId is null or a.idFacultad = :facultadId)
                        and (:programaId is null or a.idPrograma = :programaId)
                        and (:grupoId is null or a.idGrupo = :grupoId)))
            """;
    static final String HQL_COUNT = "select count(e.id)" + BASE_FROM + FILTERS;
    static final String HQL_PAGE = SELECT_RESUMEN + BASE_FROM + FILTERS + ORDER;
    static final String HQL_CONTEXTOS = """
            select distinct a
            from UvEstudianteEntity a
            where a.id = :estudianteId
            order by a.nombreInstitucion, a.nombreFacultad, a.nombrePrograma,
                a.nombreAsignatura, a.nombreGrupo, a.idGrupo
            """;
    private final EntityManager entityManager;

    public EstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de consultas de estudiante es obligatorio.");
    }

    @Override
    public EstudiantePaginaRepositoryProjection consultarEstudiantes(final ConsultarEstudiantesRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar estudiantes es obligatorio.");
        }
        // The JPA page offset is a non-negative int. Validate before the query
        // executor so invalid paging cannot be misreported as a SQL failure.
        final int offset = validatedOffset(dto);
        return JpaQueryExecutor.execute(
                "consultarEstudiantes",
                "No fue posible consultar los estudiantes.",
                () -> {
                    final Map<String, Object> parameters = filterParameters(dto);
                    final TypedQuery<Long> countQuery = entityManager.createQuery(HQL_COUNT, Long.class);
                    bind(countQuery, parameters);
                    final long total = countQuery.getSingleResult();

                    final TypedQuery<EstudianteResumenQueryRow> rows =
                            entityManager.createQuery(HQL_PAGE, EstudianteResumenQueryRow.class);
                    bind(rows, parameters);
                    final List<EstudianteResumenRepositoryProjection> items = rows
                            .setFirstResult(offset).setMaxResults(dto.size()).getResultList().stream()
                            .map(CoreViewJpaProjectionMapper::toEstudianteResumen).toList();
                    return new EstudiantePaginaRepositoryProjection(
                            items, total, totalPages(total, dto.size()), dto.page(), dto.size());
                }
        );
    }

    @Override
    public Optional<EstudianteDetalleRepositoryProjection> consultarEstudiantePorId(final UUID estudianteId) {
        if (ObjectHelper.isNull(estudianteId)) {
            throw new CrosscuttingException("El identificador del estudiante es obligatorio.");
        }
        return JpaQueryExecutor.execute(
                "consultarEstudiantePorId",
                "No fue posible consultar el estudiante por ID.",
                () -> {
                    final List<EstudianteResumenRepositoryProjection> personal = entityManager.createQuery(
                                    SELECT_RESUMEN + BASE_FROM + " where e.id = :estudianteId",
                                    EstudianteResumenQueryRow.class)
                            .setParameter("estudianteId", estudianteId).setMaxResults(1).getResultList().stream()
                            .map(CoreViewJpaProjectionMapper::toEstudianteResumen).toList();
                    if (personal.isEmpty()) {
                        return Optional.<EstudianteDetalleRepositoryProjection>empty();
                    }
                    final List<EstudianteContextoAcademicoRepositoryProjection> contexts = entityManager
                            .createQuery(HQL_CONTEXTOS, UvEstudianteEntity.class)
                            .setParameter("estudianteId", estudianteId).getResultList().stream()
                            .map(CoreViewJpaProjectionMapper::toEstudianteContexto).toList();
                    return Optional.of(new EstudianteDetalleRepositoryProjection(personal.getFirst(), contexts));
                }
        );
    }

    /** Todos los parametros de {@link #FILTERS} se enlazan siempre; {@code null} significa filtro ausente. */
    private static Map<String, Object> filterParameters(final ConsultarEstudiantesRepositoryDTO dto) {
        final Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("tipoIdentificacionId", dto.tipoIdentificacionId());
        parameters.put("numeroIdentificacion", dto.numeroIdentificacion());
        parameters.put("nombre", TextHelper.isNullOrBlank(dto.nombre())
                ? null : "%" + dto.nombre().toUpperCase(Locale.ROOT) + "%");
        parameters.put("correo", TextHelper.isNullOrBlank(dto.correo())
                ? null : "%" + dto.correo().toLowerCase(Locale.ROOT) + "%");
        parameters.put("activo", dto.activo());
        parameters.put("institucionId", dto.institucionId());
        parameters.put("facultadId", dto.facultadId());
        parameters.put("programaId", dto.programaId());
        parameters.put("grupoId", dto.grupoId());
        return parameters;
    }

    private static void bind(final Query query, final Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
    }

    /**
     * JPA accepts an int offset; do not let untrusted pagination overflow and
     * silently query a different page. Positive, representable offsets preserve
     * the existing query and HTTP contract.
     */
    private static int validatedOffset(final ConsultarEstudiantesRepositoryDTO dto) {
        if (dto.page() < 0 || dto.size() <= 0) {
            throw new CrosscuttingException("La pagina debe ser no negativa y el tamano mayor que cero.");
        }
        final long offset = (long) dto.page() * dto.size();
        if (offset > Integer.MAX_VALUE) {
            throw new CrosscuttingException("El desplazamiento de pagina supera el rango permitido por JPA.");
        }
        return (int) offset;
    }

    private static int totalPages(final long total, final int size) {
        return total == 0 ? 0 : (int) Math.ceil((double) total / size);
    }
}
