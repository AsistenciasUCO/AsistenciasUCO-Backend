package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AsistenciaJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ObjectMapper;

@Repository
public class AsistenciaJpaRepository implements AsistenciaRepositoryPort {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private static final String SQL_REGISTRAR_ASISTENCIAS_SESION = """
            EXEC dbo.usp_registrar_asistencias_sesion
                 @idSesion = :idSesion,
                 @asistenciaJSON = :asistenciaJSON,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    private static final String SQL_REGISTRAR_ASISTENCIA_AUTONOMA = """
            EXEC dbo.usp_registrar_asistencia_estudiante_autonomo
                 @idEstudiante = :idEstudiante,
                 @idSesion = :idSesion,
                 @codigoVerificacion = :codigoVerificacion,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    private static final String SQL_RADICAR_SOLICITUD_REVISION = """
            EXEC dbo.usp_radicar_solicitud_revision_asistencia_v2
                 @idEstudiante = :idEstudiante,
                 @idSesion = :idSesion,
                 @categoria = :categoria,
                 @justificacion = :justificacion,
                 @idArchivoSoporte = :idArchivoSoporte,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    private static final String SQL_RESOLVER_SOLICITUD_REVISION = """
            EXEC dbo.usp_resolver_solicitud_revision_asistencia
                 @idSolicitud = :idSolicitud,
                 @idDocente = :idDocente,
                 @accion = :accion,
                 @respuestaDocente = :respuestaDocente,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String OP_REGISTRAR_ASISTENCIAS_SESION = "registrarAsistenciasSesion";
    static final String OP_REGISTRAR_ASISTENCIA_AUTONOMA = "registrarAsistenciaAutonoma";
    static final String OP_SOLICITAR_REVISION_ASISTENCIA = "solicitarRevisionAsistencia";
    static final String OP_RESOLVER_SOLICITUD_REVISION = "resolverSolicitudRevisionAsistencia";

    private static final String PARAM_GRUPO = "grupo";
    private static final String PARAM_SESION = "sesion";

    static final String HQL_CONSULTAR_ASISTENCIAS = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaQueryRow(
                da.id, eg.idEstudiante, eg.idGrupo, a.idSesion, da.asistio, da.codigoRazonCausa)
            from UvDetalleAsistenciaEntity da
            join UvAsistenciaEntity a on a.id = da.idAsistencia
            join UvEstudianteGrupoEntity eg on eg.id = a.idEstudianteGrupo
            where eg.idGrupo = :grupo
              and (:sesion is null or a.idSesion = :sesion)
            """;

    /**
     * {@code EntityManager} administrado por Spring (compartido). No se cierra por invocacion: el proxy
     * abre y cierra su contexto de persistencia por operacion cuando no hay transaccion activa.
     */
    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public AsistenciaJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    // COMMANDS

    @Override
    public void registrarAsistenciasSesion(final RegistrarAsistenciasSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para registrar asistencias por sesion es obligatorio.");
        }

        final String asistenciaJson = serializarRegistros(dto);
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSesion", dto.sesion());
        parametros.put("asistenciaJSON", asistenciaJson);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.usuarioEjecutor());

        procedureExecutor.execute(OP_REGISTRAR_ASISTENCIAS_SESION, SQL_REGISTRAR_ASISTENCIAS_SESION, parametros, correlationId);
    }

    @Override
    public void registrarAsistenciaAutonoma(final RegistrarAsistenciaAutonomaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para registrar asistencia autonoma es obligatorio.");
        }

        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idEstudiante", dto.estudiante());
        parametros.put("idSesion", dto.sesion());
        parametros.put("codigoVerificacion", dto.codigoVerificacion());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.usuarioEjecutor());

        procedureExecutor.execute(OP_REGISTRAR_ASISTENCIA_AUTONOMA, SQL_REGISTRAR_ASISTENCIA_AUTONOMA, parametros, correlationId);
    }

    @Override
    public void solicitarRevisionAsistencia(final SolicitarRevisionAsistenciaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para solicitar revision de asistencia es obligatorio.");
        }

        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idEstudiante", dto.estudiante());
        parametros.put("idSesion", dto.sesion());
        parametros.put("categoria", dto.categoria());
        parametros.put("justificacion", dto.justificacion());
        // El soporte es opcional. Un UUID nulo sin tipo no tiene JdbcType resoluble en Hibernate; la cadena
        // (o NULL) se convierte a uniqueidentifier en SQL Server de forma implicita y sin ambiguedad.
        parametros.put("idArchivoSoporte", dto.soporteArchivoId() == null ? null : dto.soporteArchivoId().toString());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.usuarioEjecutor());

        procedureExecutor.execute(OP_SOLICITAR_REVISION_ASISTENCIA, SQL_RADICAR_SOLICITUD_REVISION, parametros, correlationId);
    }

    @Override
    public void resolverSolicitudRevisionAsistencia(final ResolverSolicitudRevisionAsistenciaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para resolver revision de asistencia es obligatorio.");
        }

        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSolicitud", dto.solicitud());
        parametros.put("idDocente", dto.docente());
        parametros.put("accion", dto.accion());
        parametros.put("respuestaDocente", dto.respuestaDocente());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.usuarioEjecutor());

        procedureExecutor.execute(OP_RESOLVER_SOLICITUD_REVISION, SQL_RESOLVER_SOLICITUD_REVISION, parametros, correlationId);
    }

    @Override
    public void registrarAsistencia(final RegistrarAsistenciaRepositoryDTO dto) {
        throw new FeatureUnavailableException("El registro individual docente requiere contrato DB inequívoco para mapear idEstadoAsistencia.");
    }

    // QUERIES

    @Override
    public List<AsistenciaRepositoryProjection> consultarAsistenciasPorGrupo(
            final ConsultarAsistenciasPorGrupoRepositoryDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para consultar asistencias por grupo es obligatorio.");
        }

        return JpaQueryExecutor.execute(
                "consultarAsistenciasPorGrupo",
                "No fue posible consultar las asistencias de base de datos.",
                () -> entityManager.createQuery(HQL_CONSULTAR_ASISTENCIAS, AsistenciaQueryRow.class)
                        .setParameter(PARAM_GRUPO, dto.getGrupo())
                        .setParameter(PARAM_SESION, dto.getSesion())
                        .getResultList()
                        .stream()
                        .map(AsistenciaJpaProjectionMapper::toProjection)
                        .toList()
        );
    }

    // PRIVATE HELPERS

    /**
     * Serializa exactamente {@code [{"idEstudiante":"<UUID>","estado":"<valor exacto>"}]}, en el orden del
     * DTO y con orden de claves fijo. Sin normalizar ni validar el estado; un valor nulo falla igual que en
     * el baseline (serializacion), no se sustituye.
     */
    private static String serializarRegistros(final RegistrarAsistenciasSesionRepositoryDTO dto) {
        try {
            final List<Map<String, String>> registros = new ArrayList<>();
            for (final var registro : dto.registros()) {
                final Map<String, String> item = new LinkedHashMap<>();
                item.put("idEstudiante", registro.estudiante().toString());
                item.put("estado", Objects.requireNonNull(registro.estado()));
                registros.add(item);
            }
            return OBJECT_MAPPER.writeValueAsString(registros);
        } catch (RuntimeException exception) {
            throw new CrosscuttingException("No fue posible serializar los registros de asistencia.", exception);
        }
    }
}
