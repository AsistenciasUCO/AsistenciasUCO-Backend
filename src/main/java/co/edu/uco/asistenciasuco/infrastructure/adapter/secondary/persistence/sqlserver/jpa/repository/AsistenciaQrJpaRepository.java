package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaQrRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * CULM-001 B.2 (HU175, HU176; DEC-015): QR/PIN rotativo de asistencia via procedimientos almacenados (JPA). El token
 * llega en claro como valor opaco: aqui se calcula su SHA-256 y la base de datos solo conoce el hash. Ni el token, ni el
 * PIN, ni el hash se registran en logs. Los SP de token y de PIN del registro se separan en dos sentencias porque un
 * {@code NULL} sin tipo en un parametro {@code VARBINARY} falla en SQL Server.
 *
 * <p>Un interbloqueo (SQL 1205), sea que el SP lo capture y lo devuelva en su tupla o que llegue como excepcion del
 * driver, se publica como {@link ConflictoConcurrenciaException} para que el caso de uso reintente.</p>
 */
@Repository
public class AsistenciaQrJpaRepository implements AsistenciaQrRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsistenciaQrJpaRepository.class);
    private static final String P_TOMA = "idToma";
    private static final String P_TOKEN_HASH = "tokenHash";
    private static final String P_PIN = "pin";
    private static final String P_CORRELACION = "idCorrelacion";
    private static final String P_USUARIO = "idUsuarioEjecutor";
    private static final String CODIGO_TOMA_CERRADA = "AQR_005";
    static final String OP_REGISTRAR = "registrarAsistenciaPorToken";
    static final String MARCA_INTERBLOQUEO = "[NUMBER=1205]";

    static final String SQL_ABRIR = """
            EXEC dbo.usp_abrir_qr_toma
                 @idToma = :idToma, @tokenHash = :tokenHash, @pin = :pin,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_ROTAR_MANUAL = """
            EXEC dbo.usp_rotar_token_asistencia_toma
                 @idToma = :idToma, @tokenHash = :tokenHash, @pin = :pin,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_ROTAR_AUTOMATICO = """
            EXEC dbo.usp_rotar_token_asistencia_automatico
                 @idToma = :idToma, @tokenHash = :tokenHash, @pin = :pin, @idCorrelacion = :idCorrelacion
            """;
    static final String SQL_POR_ROTAR = "EXEC dbo.usp_consultar_tomas_qr_por_rotar @idCorrelacion = :idCorrelacion";
    static final String SQL_CERRAR = """
            EXEC dbo.usp_cerrar_qr_toma @idToma = :idToma, @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_REGISTRAR_TOKEN = """
            EXEC dbo.usp_registrar_asistencia_por_token
                 @tokenHash = :tokenHash, @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_REGISTRAR_PIN = """
            EXEC dbo.usp_registrar_asistencia_por_token
                 @pin = :pin, @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public AsistenciaQrJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager del QR de asistencia es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public QrEmitido abrir(final UUID tomaId, final String token, final String pin, final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        // idTokenAsistencia, idToma, idSesion, idGrupo, fechaRotacionUtc, fechaExpiracionUtc, rotacionSegundos
        final List<Object> c = procedureExecutor.executeWithExtras("abrirQrToma", SQL_ABRIR,
                credenciales(tomaId, token, pin, usuarioEjecutor, correlacion), correlacion, 7).extras();
        return new QrEmitido(NativeRowValues.uuid(c.get(0)), NativeRowValues.uuid(c.get(1)), NativeRowValues.uuid(c.get(2)),
                NativeRowValues.uuid(c.get(3)), NativeRowValues.instanteUtc(c.get(4)), NativeRowValues.instanteUtc(c.get(5)),
                NativeRowValues.entero(c.get(6)));
    }

    @Override
    public QrRotado rotarManual(final UUID tomaId, final String token, final String pin, final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        return aRotado(procedureExecutor.executeWithExtras("rotarTokenAsistenciaToma", SQL_ROTAR_MANUAL,
                credenciales(tomaId, token, pin, usuarioEjecutor, correlacion), correlacion, 7));
    }

    /** {@code rotado = 0} (otra instancia ya roto o aun no tocaba) o una toma/token cerrado ({@code AQR_005}): vacio. */
    @Override
    public Optional<QrRotado> rotarAutomatico(final UUID tomaId, final String token, final String pin) {
        final UUID correlacion = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(P_TOMA, tomaId);
        parametros.put(P_TOKEN_HASH, TokenHashHelper.sha256(token));
        parametros.put(P_PIN, pin);
        parametros.put(P_CORRELACION, correlacion);
        final ProcedureRow fila;
        try {
            fila = procedureExecutor.executeWithExtras("rotarTokenAsistenciaAutomatico", SQL_ROTAR_AUTOMATICO, parametros, correlacion, 7);
        } catch (final ApplicationException exception) {
            if (CODIGO_TOMA_CERRADA.equals(exception.getCode())) {
                return Optional.empty();
            }
            throw exception;
        }
        return NativeRowValues.bit(fila.extras().get(6)) ? Optional.of(aRotado(fila)) : Optional.empty();
    }

    @Override
    public List<TomaPorRotar> consultarTomasPorRotar() {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarTomasQrPorRotar", "No fue posible consultar las tomas QR por rotar.", () -> {
            @SuppressWarnings("unchecked")
            final List<Tuple> filas = (List<Tuple>) entityManager.createNativeQuery(SQL_POR_ROTAR, Tuple.class)
                    .setParameter(P_CORRELACION, correlacion)
                    .getResultList();
            final List<TomaPorRotar> resultado = new ArrayList<>(filas.size());
            for (final Tuple f : filas) {
                resultado.add(new TomaPorRotar(NativeRowValues.uuid(f.get(P_TOMA)), NativeRowValues.uuid(f.get("idGrupo")),
                        NativeRowValues.uuid(f.get("idDocenteEmisor"))));
            }
            return resultado;
        });
    }

    @Override
    public String cerrar(final UUID tomaId, final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(P_TOMA, tomaId);
        parametros.put(P_CORRELACION, correlacion);
        parametros.put(P_USUARIO, usuarioEjecutor);
        // idToma, idSesion, idGrupo
        return procedureExecutor.executeWithExtras("cerrarQrToma", SQL_CERRAR, parametros, correlacion, 3)
                .result().getMensajeUsuarioResultado();
    }

    @Override
    public AsistenciaPorToken registrarPorToken(final String token, final String pin, final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        final boolean porToken = token != null;
        final Map<String, Object> parametros = new LinkedHashMap<>();
        if (porToken) {
            parametros.put(P_TOKEN_HASH, TokenHashHelper.sha256(token));
        } else {
            parametros.put(P_PIN, pin);
        }
        parametros.put(P_CORRELACION, correlacion);
        parametros.put(P_USUARIO, usuarioEjecutor);
        final ProcedureRow fila = ejecutarDetectandoInterbloqueo(porToken ? SQL_REGISTRAR_TOKEN : SQL_REGISTRAR_PIN, parametros);
        if (!fila.result().getEstadoResultado() && contieneInterbloqueo(fila.result().getMensajeTecnicoResultado())) {
            throw new ConflictoConcurrenciaException("La base de datos eligio el registro como victima de un interbloqueo.");
        }
        ProcedureResultValidator.validate(fila.result(), correlacion, OP_REGISTRAR);
        // idToma, idSesion, idGrupo, nombreAsignatura, nombreGrupo, numeroSesion, numeroToma, yaRegistrado, fechaRegistroUtc
        final List<Object> c = fila.extras();
        return new AsistenciaPorToken(NativeRowValues.uuid(c.get(0)), NativeRowValues.uuid(c.get(1)), NativeRowValues.uuid(c.get(2)),
                NativeRowValues.texto(c.get(3)), NativeRowValues.texto(c.get(4)), NativeRowValues.entero(c.get(5)),
                NativeRowValues.entero(c.get(6)), NativeRowValues.bit(c.get(7)), NativeRowValues.instanteUtc(c.get(8)),
                fila.result().getMensajeUsuarioResultado());
    }

    private ProcedureRow ejecutarDetectandoInterbloqueo(final String sql, final Map<String, Object> parametros) {
        try {
            final Query consulta = entityManager.createNativeQuery(sql);
            parametros.forEach(consulta::setParameter);
            return ProcedureResultMapper.mapSingleWithExtras(consulta.getResultList(), 9);
        } catch (PersistenceException | IllegalStateException | IllegalArgumentException exception) {
            if (esInterbloqueo(exception)) {
                throw new ConflictoConcurrenciaException("La base de datos eligio el registro como victima de un interbloqueo.");
            }
            LOGGER.error("SQL operation failed. operation={}, correlationId={}", OP_REGISTRAR, CorrelationIdContext.getAsString());
            throw new DatabaseOperationException(DatabaseErrorCode.DATABASE_OPERATION_ERROR,
                    "No fue posible ejecutar el procedimiento almacenado.", exception);
        }
    }

    static boolean esInterbloqueo(final Throwable exception) {
        for (Throwable actual = exception; actual != null; actual = actual.getCause()) {
            final String nombre = actual.getClass().getSimpleName();
            final String mensaje = actual.getMessage() == null ? "" : actual.getMessage().toLowerCase(Locale.ROOT);
            if (nombre.contains("LockAcquisition") || nombre.contains("DeadlockLoser") || mensaje.contains("deadlock")
                    || contieneInterbloqueo(actual.getMessage())) {
                return true;
            }
        }
        return false;
    }

    static boolean contieneInterbloqueo(final String mensajeTecnico) {
        return mensajeTecnico != null && mensajeTecnico.contains(MARCA_INTERBLOQUEO);
    }

    private static Map<String, Object> credenciales(final UUID tomaId, final String token, final String pin,
                                                    final UUID usuarioEjecutor, final UUID correlacion) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(P_TOMA, tomaId);
        parametros.put(P_TOKEN_HASH, TokenHashHelper.sha256(token));
        parametros.put(P_PIN, pin);
        parametros.put(P_CORRELACION, correlacion);
        parametros.put(P_USUARIO, usuarioEjecutor);
        return parametros;
    }

    private static QrRotado aRotado(final ProcedureRow fila) {
        // idTokenAsistencia, idToma, idGrupo, idDocenteEmisor, fechaRotacionUtc, fechaExpiracionUtc, rotado
        final List<Object> c = fila.extras();
        return new QrRotado(NativeRowValues.uuid(c.get(0)), NativeRowValues.uuid(c.get(1)), NativeRowValues.uuid(c.get(2)),
                NativeRowValues.uuid(c.get(3)), NativeRowValues.instanteUtc(c.get(4)), NativeRowValues.instanteUtc(c.get(5)));
    }
}
