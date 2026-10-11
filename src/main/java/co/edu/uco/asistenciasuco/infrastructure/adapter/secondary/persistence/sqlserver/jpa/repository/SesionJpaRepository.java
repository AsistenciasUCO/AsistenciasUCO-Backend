package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.features.sesion.common.ContratoHorarioSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvSesionEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class SesionJpaRepository implements SesionRepositoryPort {

    static final String SQL_CREAR_SESION = """
            EXEC dbo.usp_crear_sesion
                 @idGrupo = :idGrupo,
                 @nombre = :nombre,
                 @fechaHoraInicio = :fechaHoraInicio,
                 @fechaHoraFin = :fechaHoraFin,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_ACTUALIZAR_SESION = """
            EXEC dbo.usp_actualizar_sesion
                 @idSesion = :idSesion,
                 @nombre = :nombre,
                 @fechaHoraInicio = :fechaHoraInicio,
                 @fechaHoraFin = :fechaHoraFin,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    // UTC-D06: mismos parametros nombrados que v1; el SP v2 marca procedenciaTemporal = UTC_V2 en la
    // misma sentencia que escribe las horas. Ningun parametro permite elegir la procedencia.
    static final String SQL_CREAR_SESION_V2 = """
            EXEC dbo.usp_crear_sesion_v2
                 @idGrupo = :idGrupo,
                 @nombre = :nombre,
                 @fechaHoraInicio = :fechaHoraInicio,
                 @fechaHoraFin = :fechaHoraFin,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_ACTUALIZAR_SESION_V2 = """
            EXEC dbo.usp_actualizar_sesion_v2
                 @idSesion = :idSesion,
                 @nombre = :nombre,
                 @fechaHoraInicio = :fechaHoraInicio,
                 @fechaHoraFin = :fechaHoraFin,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    /**
     * Literal ISO 8601 con 7 decimales que SQL Server convierte a DATETIME2(7) de forma exacta e
     * independiente del idioma y del timezone de la JVM (un Timestamp JDBC se construye en la zona
     * por defecto del proceso y puede caer en un salto DST local).
     */
    private static final DateTimeFormatter DATETIME2_UTC_LITERAL =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSS");

    static final String SQL_CERRAR_SESION = """
            EXEC dbo.usp_cerrar_sesion
                 @idSesion = :idSesion,
                 @idDocente = :idDocente,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_GENERAR_SESIONES_GRUPO = """
            EXEC dbo.usp_generar_sesiones_grupo
                 @idGrupo = :idGrupo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    private static final String SELECT_ROW = """
            select s
            from UvSesionEntity s
            """;
    static final String HQL_POR_ID = SELECT_ROW + " where s.id = :idSesion order by s.id";
    static final String HQL_POR_GRUPO = SELECT_ROW
            + " where s.idGrupo = :idGrupo order by s.fechaHoraInicio, s.numero, s.id";

    static final String OP_CREAR_SESION = "crearSesion";
    static final String OP_ACTUALIZAR_SESION = "actualizarSesion";
    static final String OP_CREAR_SESION_V2 = "crearSesionV2";
    static final String OP_ACTUALIZAR_SESION_V2 = "actualizarSesionV2";
    static final String OP_CERRAR_SESION = "cerrarSesion";
    static final String OP_GENERAR_SESIONES_GRUPO = "generarSesionesGrupo";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public SesionJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    // COMMANDS

    @Override
    public void crearSesion(final CrearSesionRepositoryDTO dto) {
        require(dto, "El dominio para crear sesion es obligatorio.");
        final UUID correlationId = CorrelationIdContext.require();

        final boolean utcConfirmado = dto.getContratoTemporal() == ContratoHorarioSesion.UTC_CONFIRMADO_V2;

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", dto.getGrupo());
        parametros.put("nombre", dto.getNombre());
        parametros.put("fechaHoraInicio", temporal(dto.getFechaHoraInicio(), utcConfirmado));
        parametros.put("fechaHoraFin", temporal(dto.getFechaHoraFin(), utcConfirmado));
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.getUsuarioEjecutor());

        procedureExecutor.execute(
                utcConfirmado ? OP_CREAR_SESION_V2 : OP_CREAR_SESION,
                utcConfirmado ? SQL_CREAR_SESION_V2 : SQL_CREAR_SESION,
                parametros,
                correlationId
        );
    }

    @Override
    public void actualizarSesion(final ActualizarSesionRepositoryDTO dto) {
        require(dto, "El dominio para actualizar sesion es obligatorio.");
        final UUID correlationId = CorrelationIdContext.require();

        final boolean utcConfirmado = dto.contratoTemporal() == ContratoHorarioSesion.UTC_CONFIRMADO_V2;

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSesion", dto.sesion());
        parametros.put("nombre", dto.nombre());
        parametros.put("fechaHoraInicio", temporal(dto.fechaHoraInicio(), utcConfirmado));
        parametros.put("fechaHoraFin", temporal(dto.fechaHoraFin(), utcConfirmado));
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.usuarioEjecutor());

        procedureExecutor.execute(
                utcConfirmado ? OP_ACTUALIZAR_SESION_V2 : OP_ACTUALIZAR_SESION,
                utcConfirmado ? SQL_ACTUALIZAR_SESION_V2 : SQL_ACTUALIZAR_SESION,
                parametros,
                correlationId
        );
    }

    @Override
    public void cerrarSesion(final CerrarSesionRepositoryDTO dto) {
        require(dto, "El dominio para cerrar sesion es obligatorio.");
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSesion", dto.getSesion());
        parametros.put("idDocente", dto.getDocente());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.getUsuarioEjecutor());

        procedureExecutor.execute(OP_CERRAR_SESION, SQL_CERRAR_SESION, parametros, correlationId);
    }

    @Override
    public void generarSesionesGrupo(final GenerarSesionesGrupoRepositoryDTO dto) {
        require(dto, "El dominio para generar sesiones de grupo es obligatorio.");
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", dto.grupo());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.usuarioEjecutor());

        procedureExecutor.execute(OP_GENERAR_SESIONES_GRUPO, SQL_GENERAR_SESIONES_GRUPO, parametros, correlationId);
    }

    // QUERIES

    @Override
    public SesionRepositoryProjection consultarSesion(final ConsultarSesionRepositoryDTO dto) {
        require(dto, "El dominio para consultar sesion es obligatorio.");
        return consultarSesionPorId(dto.getSesion());
    }

    @Override
    public List<SesionRepositoryProjection> consultarSesionesPorGrupo(final UUID grupoId) {
        require(grupoId, "El identificador del grupo para consultar sesiones es obligatorio.");
        return JpaQueryExecutor.execute(
                "consultarSesionesPorGrupo",
                "No fue posible consultar las sesiones del grupo desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_GRUPO, UvSesionEntity.class)
                        .setParameter("idGrupo", grupoId)
                        .getResultList().stream().map(CoreViewJpaProjectionMapper::toSesion).toList()
        );
    }

    private SesionRepositoryProjection consultarSesionPorId(final UUID sesionId) {
        return JpaQueryExecutor.execute(
                "consultarSesion",
                "No fue posible consultar la sesion desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_ID, UvSesionEntity.class)
                        .setParameter("idSesion", sesionId)
                        .setMaxResults(1)
                        .getResultList().stream()
                        .findFirst()
                        .map(CoreViewJpaProjectionMapper::toSesion)
                        .orElse(null)
        );
    }

    // PRIVATE HELPERS

    private static void require(final Object value, final String message) {
        if (ObjectHelper.isNull(value)) throw new CrosscuttingException(message);
    }

    /** v1 conserva su binding AS-IS; v2 envia el instante UTC como literal DATETIME2(7) exacto. */
    private static Object temporal(final LocalDateTime value, final boolean utcConfirmado) {
        if (!utcConfirmado || value == null) {
            return value;
        }
        return DATETIME2_UTC_LITERAL.format(value);
    }
}
