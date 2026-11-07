package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
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
    static final String OP_SOLICITAR_REVISION_ASISTENCIA = "solicitarRevisionAsistencia";
    static final String OP_RESOLVER_SOLICITUD_REVISION = "resolverSolicitudRevisionAsistencia";
    static final String OP_RETIRAR_SOLICITUD_REVISION = "retirarSolicitudRevisionAsistencia";

    // CULM-001 A.2-7: retiro del reclamo propio pendiente (P -> X). Comando con el resultset canonico de 4 columnas.
    static final String SQL_RETIRAR_SOLICITUD_REVISION = """
            EXEC dbo.usp_retirar_solicitud_revision_asistencia
                 @idSolicitudRevision = :idSolicitudRevision,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    private static final String PARAM_GRUPO = "idGrupo";
    private static final String PARAM_SESION = "idSesion";

    // CULM-001 A.2-2: asistencias del grupo con ambito validado por el SP ("ausencia = sin fila", §E), lista completa
    // (@tamanoPagina = NULL, A2-C3). Grupo inexistente o ajeno: THROW DBCODE=SEC_002 -> 403.
    static final String SQL_CONSULTAR_ASISTENCIAS = """
            EXEC dbo.usp_consultar_asistencias_grupo_ambito_paginado
                 @idGrupo = :idGrupo,
                 @idSesion = :idSesion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = 1,
                 @tamanoPagina = NULL
            """;
    static final int COLUMNAS_ASISTENCIAS = 8;
    private static final String OBSERVACION_VACIA = "";

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
    public String retirarSolicitudRevision(final UUID idSolicitudRevision, final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(idSolicitudRevision) || ObjectHelper.isNull(usuarioEjecutor)) {
            throw new CrosscuttingException("La solicitud de revision y el usuario ejecutor son obligatorios para retirarla.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSolicitudRevision", idSolicitudRevision);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);
        return procedureExecutor.execute(OP_RETIRAR_SOLICITUD_REVISION, SQL_RETIRAR_SOLICITUD_REVISION, parametros, correlationId)
                .getMensajeUsuarioResultado();
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

        if (ObjectHelper.isNull(dto.getUsuarioEjecutor())) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta de asistencias por grupo es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarAsistenciasPorGrupo",
                "No fue posible consultar las asistencias de base de datos.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_CONSULTAR_ASISTENCIAS)
                        .setParameter(PARAM_GRUPO, dto.getGrupo())
                        // Un UUID nulo sin tipo no tiene JdbcType en Hibernate: la sesion viaja como texto o NULL.
                        .setParameter(PARAM_SESION, dto.getSesion() == null ? null : dto.getSesion().toString())
                        .setParameter("idUsuarioEjecutor", dto.getUsuarioEjecutor())
                        .setParameter("idCorrelacion", correlationId)
                        .getResultList())
                        .stream()
                        .map(Object[].class::cast)
                        .map(AsistenciaJpaRepository::toAsistencia)
                        .collect(java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toList(), AsistenciaJpaRepository::consolidarPorSesion)),
                DbCodeTranslator::traducir
        );
    }

    // PRIVATE HELPERS

    /**
     * CULM-001 B.2 ([SUPUESTO-B-15], contrato 3.2.9): con varias tomas el SP del ambito devuelve un detalle por toma; el
     * golden path de {@code GET /grupos/{id}/asistencias} conserva su esquema con UNA fila por estudiante y sesion y el
     * estado consolidado: {@code SJC} si alguna toma es SJC; si no {@code EX} si alguna es EX; si no {@code AN}. La fila
     * conserva el id del primer detalle (orden del SP) y {@code presente} = estado consolidado AN. Con una toma por sesion
     * el resultado es identico al actual. La lista es completa ({@code @tamanoPagina = NULL}), asi que agrupar aqui no
     * altera la paginacion.
     */
    static List<AsistenciaRepositoryProjection> consolidarPorSesion(final List<AsistenciaRepositoryProjection> filas) {
        final Map<List<UUID>, AsistenciaRepositoryProjection> porSesion = new LinkedHashMap<>();
        for (final AsistenciaRepositoryProjection fila : filas) {
            porSesion.merge(java.util.Arrays.asList(fila.getEstudiante(), fila.getSesion()), fila, AsistenciaJpaRepository::consolidar);
        }
        return List.copyOf(porSesion.values());
    }

    private static AsistenciaRepositoryProjection consolidar(final AsistenciaRepositoryProjection primera,
                                                             final AsistenciaRepositoryProjection otra) {
        final String estado = prioridad(otra.getEstado()) > prioridad(primera.getEstado()) ? otra.getEstado() : primera.getEstado();
        return new AsistenciaRepositoryProjection(primera.getAsistencia(), primera.getEstudiante(), primera.getGrupo(),
                primera.getSesion(), "AN".equals(estado), estado, primera.getObservacion());
    }

    private static int prioridad(final String estado) {
        return switch (estado == null ? "" : estado) {
            case "SJC" -> 3;
            case "EX" -> 2;
            case "AN" -> 1;
            default -> 0;
        };
    }

    private static AsistenciaRepositoryProjection toAsistencia(final Object[] f) {
        if (f.length != COLUMNAS_ASISTENCIAS) {
            throw new IllegalStateException("La consulta de asistencias del grupo devolvio " + f.length
                    + " columnas; se esperaban " + COLUMNAS_ASISTENCIAS + ".");
        }
        final boolean presente = f[4] instanceof Boolean bit ? bit : f[4] instanceof Number numero && numero.intValue() == 1;
        return new AsistenciaRepositoryProjection(NativeRowValues.uuid(f[0]), NativeRowValues.uuid(f[1]),
                NativeRowValues.uuid(f[2]), NativeRowValues.uuid(f[3]), presente,
                f[5] == null ? null : f[5].toString().strip(),
                f[6] == null ? OBSERVACION_VACIA : f[6].toString());
    }

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
