package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CrearUsuarioRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvUsuarioEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioJpaRepository implements UsuarioRepositoryPort {

    static final String SQL_SINCRONIZAR_USUARIO = """
            EXEC dbo.usp_sincronizar_usuario
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @correo = :correo,
                 @password = :password,
                 @idCorrelacion = :idCorrelacion
            """;

    private static final String SELECT_ROW = """
            select u
            from UvUsuarioEntity u
            """;
    static final String HQL_POR_CORREO = SELECT_ROW
            + " where lower(trim(u.correo)) = lower(trim(:correo)) order by u.id";
    static final String HQL_POR_ID = SELECT_ROW + " where u.id = :idUsuario order by u.id";
    static final String HQL_POR_IDENTIFICACION = SELECT_ROW
            + " where u.idTipoIdentificacion = :tipoIdentificacionId"
            + " and u.numeroIdentificacion = :numeroIdentificacion order by u.id";
    static final String HQL_POR_NUMERO_IDENTIFICACION = SELECT_ROW
            + " where u.numeroIdentificacion = :numeroIdentificacion order by u.id";

    // CULM-001 A.2-15: correo real del estudiante ya resuelto por usp_buscar_estudiante_por_documento_grupo (uso interno).
    static final String HQL_CORREO_POR_ESTUDIANTE = """
            select u.correo
            from UvEstudianteIdentidadEntity e join UvUsuarioEntity u on e.idUsuario = u.id
            where e.id = :idEstudiante
            """;

    static final String OP_SINCRONIZAR_USUARIO = "crearUsuario";
    private static final String PARAM_NUMERO_IDENTIFICACION = "numeroIdentificacion";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public UsuarioJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    // COMMANDS

    @Override
    public CrearUsuarioRepositoryProjection crearUsuario(final CrearUsuarioRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) throw new CrosscuttingException("El dominio para crear usuario es obligatorio.");
        final ProcedureResult result = sincronizarUsuario(dto);
        final UUID usuarioId = consultarPorIdentificacion(dto.getTipoIdIdentificacion(), dto.getNumeroIdentificacion())
                .map(UsuarioIdentidadRepositoryProjection::id).orElse(null);
        return new CrearUsuarioRepositoryProjection(usuarioId, result.getMensajeUsuarioResultado());
    }

    // QUERIES

    @Override
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorCorreo(final String correo) {
        return consultarPorCorreo(correo);
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorId(final UUID idUsuario) {
        return consultarPorId(idUsuario);
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorIdentificacion(
            final UUID tipoIdentificacionId, final Integer numeroIdentificacion) {
        return consultarPorIdentificacion(tipoIdentificacionId, numeroIdentificacion);
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorNumeroIdentificacion(
            final Integer numeroIdentificacion) {
        return first(HQL_POR_NUMERO_IDENTIFICACION,
                query -> query.setParameter(PARAM_NUMERO_IDENTIFICACION, numeroIdentificacion),
                "consultarUsuarioPorNumeroIdentificacion",
                "No fue posible consultar el usuario por numero de identificacion.");
    }

    private Optional<UsuarioIdentidadRepositoryProjection> consultarPorCorreo(final String correo) {
        return first(HQL_POR_CORREO, query -> query.setParameter("correo", correo), "consultarUsuarioPorCorreo",
                "No fue posible consultar el usuario por correo.");
    }

    private Optional<UsuarioIdentidadRepositoryProjection> consultarPorId(final UUID idUsuario) {
        return first(HQL_POR_ID, query -> query.setParameter("idUsuario", idUsuario), "consultarUsuarioPorId",
                "No fue posible consultar el usuario por id.");
    }

    private Optional<UsuarioIdentidadRepositoryProjection> consultarPorIdentificacion(
            final UUID tipoIdentificacionId, final Integer numeroIdentificacion) {
        return first(HQL_POR_IDENTIFICACION, query -> query.setParameter("tipoIdentificacionId", tipoIdentificacionId)
                        .setParameter(PARAM_NUMERO_IDENTIFICACION, numeroIdentificacion),
                "consultarUsuarioPorIdentificacion", "No fue posible consultar el usuario por identificacion.");
    }

    @Override
    public Optional<String> consultarCorreoPorEstudiante(final UUID idEstudiante) {
        if (idEstudiante == null) {
            return Optional.empty();
        }
        return JpaQueryExecutor.execute(
                "consultarCorreoPorEstudiante",
                "No fue posible consultar el correo del estudiante.",
                () -> entityManager.createQuery(HQL_CORREO_POR_ESTUDIANTE, String.class)
                        .setParameter("idEstudiante", idEstudiante)
                        .setMaxResults(1)
                        .getResultList().stream()
                        .filter(correo -> correo != null && !correo.isBlank())
                        .findFirst()
        );
    }

    // PRIVATE HELPERS

    private ProcedureResult sincronizarUsuario(final CrearUsuarioRepositoryDTO dto) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idTipoIdIdentificacion", dto.getTipoIdIdentificacion());
        parametros.put(PARAM_NUMERO_IDENTIFICACION, dto.getNumeroIdentificacion());
        parametros.put("primerApellido", dto.getPrimerApellido());
        parametros.put("segundoApellido", dto.getSegundoApellido());
        parametros.put("primerNombre", dto.getPrimerNombre());
        parametros.put("segundoNombre", dto.getSegundoNombre());
        parametros.put("correo", dto.getCorreo());
        parametros.put("password", dto.getPassword());
        parametros.put("idCorrelacion", correlationId);

        return procedureExecutor.execute(OP_SINCRONIZAR_USUARIO, SQL_SINCRONIZAR_USUARIO, parametros, correlationId);
    }

    private Optional<UsuarioIdentidadRepositoryProjection> first(final String hql,
            final java.util.function.UnaryOperator<jakarta.persistence.TypedQuery<UvUsuarioEntity>> binder,
            final String operation, final String message) {
        return JpaQueryExecutor.execute(
                operation,
                message,
                () -> binder.apply(entityManager.createQuery(hql, UvUsuarioEntity.class)).setMaxResults(1)
                        .getResultList().stream().findFirst().map(CoreViewJpaProjectionMapper::toUsuario)
        );
    }
}
