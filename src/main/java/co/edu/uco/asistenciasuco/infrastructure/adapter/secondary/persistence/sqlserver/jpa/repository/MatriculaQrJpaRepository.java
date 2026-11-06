package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * HU176B: matricula por QR/PIN rotativo via procedimientos almacenados (JPA). El token llega en claro como valor opaco;
 * aqui se calcula su SHA-256 y la base de datos solo conoce el hash. Ni el token, ni el PIN, ni el hash se registran en logs.
 *
 * <p>Los SP de token y de PIN se separan en dos sentencias porque el parametro que no se usa debe omitirse (un {@code NULL}
 * sin tipo en un parametro {@code VARBINARY} falla en SQL Server).</p>
 */
@Repository
public class MatriculaQrJpaRepository implements MatriculaQrRepositoryPort {

    private static final String P_ID_CORRELACION = "idCorrelacion";
    private static final String P_ID_USUARIO_EJECUTOR = "idUsuarioEjecutor";
    private static final String CODIGO_TOKEN_INVALIDO = "MQR_001";

    static final String SQL_ABRIR = """
            EXEC dbo.usp_abrir_ventana_matricula_grupo
                 @idGrupo = :idGrupo, @tokenHash = :tokenHash, @pin = :pin,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_CERRAR = """
            EXEC dbo.usp_cerrar_ventana_matricula_grupo
                 @idGrupo = :idGrupo, @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_ROTAR_MANUAL = """
            EXEC dbo.usp_rotar_token_matricula_grupo
                 @idGrupo = :idGrupo, @tokenHash = :tokenHash, @pin = :pin,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_ROTAR_AUTOMATICO = """
            EXEC dbo.usp_rotar_token_matricula_automatico
                 @idGrupo = :idGrupo, @tokenHash = :tokenHash, @pin = :pin, @idCorrelacion = :idCorrelacion
            """;
    static final String SQL_POR_ROTAR = "EXEC dbo.usp_consultar_ventanas_matricula_por_rotar";
    static final String SQL_VALIDAR_TOKEN = """
            EXEC dbo.usp_validar_token_matricula_publico @tokenHash = :tokenHash, @idCorrelacion = :idCorrelacion
            """;
    static final String SQL_VALIDAR_PIN = """
            EXEC dbo.usp_validar_token_matricula_publico @pin = :pin, @idCorrelacion = :idCorrelacion
            """;
    static final String SQL_MATRICULAR_TOKEN = """
            EXEC dbo.usp_matricular_estudiante_por_token
                 @tokenHash = :tokenHash, @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_MATRICULAR_PIN = """
            EXEC dbo.usp_matricular_estudiante_por_token
                 @pin = :pin, @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    private static final String SQL_REGISTRAR_BASE = """
            EXEC dbo.usp_registrar_estudiante_por_token_publico
                 %s,
                 @idTipoIdentificacion = :idTipoIdentificacion, @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre, @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido, @segundoApellido = :segundoApellido,
                 @correo = :correo, @password = :password, @idCorrelacion = :idCorrelacion
            """;
    static final String SQL_REGISTRAR_TOKEN = SQL_REGISTRAR_BASE.formatted("@tokenHash = :tokenHash");
    static final String SQL_REGISTRAR_PIN = SQL_REGISTRAR_BASE.formatted("@pin = :pin");

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public MatriculaQrJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de matricula QR es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public VentanaAbierta abrirVentana(final UUID grupoId, final String token, final String pin, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", grupoId);
        parametros.put("tokenHash", TokenHashHelper.sha256(token));
        parametros.put("pin", pin);
        parametros.put(P_ID_CORRELACION, correlationId);
        parametros.put(P_ID_USUARIO_EJECUTOR, usuarioEjecutor);
        final ProcedureRow fila = procedureExecutor.executeWithExtras("abrirVentanaMatriculaGrupo", SQL_ABRIR, parametros, correlationId, 2);
        return new VentanaAbierta(aUuid(fila.extras().get(0)), aInstante(fila.extras().get(1)));
    }

    @Override
    public void cerrarVentana(final UUID grupoId, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", grupoId);
        parametros.put(P_ID_CORRELACION, correlationId);
        parametros.put(P_ID_USUARIO_EJECUTOR, usuarioEjecutor);
        procedureExecutor.execute("cerrarVentanaMatriculaGrupo", SQL_CERRAR, parametros, correlationId);
    }

    @Override
    public VentanaRotada rotarManual(final UUID grupoId, final String token, final String pin, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", grupoId);
        parametros.put("tokenHash", TokenHashHelper.sha256(token));
        parametros.put("pin", pin);
        parametros.put(P_ID_CORRELACION, correlationId);
        parametros.put(P_ID_USUARIO_EJECUTOR, usuarioEjecutor);
        return aVentanaRotada(procedureExecutor.executeWithExtras(
                "rotarTokenMatriculaGrupo", SQL_ROTAR_MANUAL, parametros, correlationId, 4));
    }

    /**
     * El SP automatico es idempotente: si otra instancia ya roto la ventana responde exito sin cambios. Para no publicar
     * un QR que no quedo guardado se confirma, tras el SP, que el token emitido es el vigente; si no lo es, no hubo rotacion
     * y se devuelve {@code null}. Una ventana que ya no existe o vencio tambien es una rotacion que no aplica.
     */
    @Override
    public VentanaRotada rotarAutomatico(final UUID grupoId, final String token, final String pin) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", grupoId);
        parametros.put("tokenHash", TokenHashHelper.sha256(token));
        parametros.put("pin", pin);
        parametros.put(P_ID_CORRELACION, correlationId);
        final VentanaRotada ventana;
        try {
            ventana = aVentanaRotada(procedureExecutor.executeWithExtras(
                    "rotarTokenMatriculaAutomatico", SQL_ROTAR_AUTOMATICO, parametros, correlationId, 4));
        } catch (final ResourceNotFoundException exception) {
            if (CODIGO_TOKEN_INVALIDO.equals(exception.getCode())) {
                return null;
            }
            throw exception;
        }
        return esVigente(token) ? ventana : null;
    }

    @Override
    public List<VentanaPorRotar> consultarVentanasPorRotar() {
        return JpaQueryExecutor.execute(
                "consultarVentanasMatriculaPorRotar",
                "No fue posible consultar las ventanas de matricula por rotar.",
                () -> {
                    final List<?> filas = entityManager.createNativeQuery(SQL_POR_ROTAR).getResultList();
                    final List<VentanaPorRotar> resultado = new ArrayList<>(filas.size());
                    for (final Object fila : filas) {
                        final Object[] columnas = (Object[]) fila;
                        resultado.add(new VentanaPorRotar(aUuid(columnas[0]), aUuid(columnas[1])));
                    }
                    return resultado;
                }
        );
    }

    @Override
    public CursoMatricula validar(final CredencialMatricula credencial) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "validarTokenMatriculaPublico",
                "No fue posible validar el token de matricula desde base de datos.",
                () -> {
                    final List<?> filas;
                    try {
                        final Query consulta = credencial.token() != null
                                ? entityManager.createNativeQuery(SQL_VALIDAR_TOKEN).setParameter("tokenHash", TokenHashHelper.sha256(credencial.token()))
                                : entityManager.createNativeQuery(SQL_VALIDAR_PIN).setParameter("pin", credencial.pin());
                        filas = consulta.setParameter(P_ID_CORRELACION, correlationId).getResultList();
                    } catch (final PersistenceException exception) {
                        throw DbCodeTranslator.traducir(exception);
                    }
                    if (filas.isEmpty()) {
                        throw new PersistenceException("El SP de validacion no devolvio el curso de la ventana.");
                    }
                    final Object[] c = (Object[]) filas.getFirst();
                    return new CursoMatricula(
                            aUuid(c[0]), String.valueOf(c[1]), String.valueOf(c[2]), String.valueOf(c[3]),
                            aInstante(c[4]), ((Number) c[5]).intValue());
                }
        );
    }

    @Override
    public MatriculaRealizada registrarPublico(final RegistroPublico datos) {
        final UUID correlationId = CorrelationIdContext.require();
        final boolean porToken = datos.credencial().token() != null;
        final Map<String, Object> parametros = new LinkedHashMap<>();
        if (porToken) {
            parametros.put("tokenHash", TokenHashHelper.sha256(datos.credencial().token()));
        } else {
            parametros.put("pin", datos.credencial().pin());
        }
        parametros.put("idTipoIdentificacion", datos.tipoIdentificacionId());
        parametros.put("numeroIdentificacion", datos.numeroIdentificacion());
        parametros.put("primerNombre", datos.primerNombre());
        parametros.put("segundoNombre", datos.segundoNombre());
        parametros.put("primerApellido", datos.primerApellido());
        parametros.put("segundoApellido", datos.segundoApellido());
        parametros.put("correo", datos.correo());
        parametros.put("password", datos.passwordCodificada());
        parametros.put(P_ID_CORRELACION, correlationId);
        return aMatricula(procedureExecutor.executeWithExtras(
                "registrarEstudiantePorTokenPublico",
                porToken ? SQL_REGISTRAR_TOKEN : SQL_REGISTRAR_PIN,
                parametros, correlationId, 3));
    }

    @Override
    public MatriculaRealizada matricularAutenticado(final CredencialMatricula credencial, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final boolean porToken = credencial.token() != null;
        final Map<String, Object> parametros = new LinkedHashMap<>();
        if (porToken) {
            parametros.put("tokenHash", TokenHashHelper.sha256(credencial.token()));
        } else {
            parametros.put("pin", credencial.pin());
        }
        parametros.put(P_ID_CORRELACION, correlationId);
        parametros.put(P_ID_USUARIO_EJECUTOR, usuarioEjecutor);
        return aMatricula(procedureExecutor.executeWithExtras(
                "matricularEstudiantePorToken",
                porToken ? SQL_MATRICULAR_TOKEN : SQL_MATRICULAR_PIN,
                parametros, correlationId, 3));
    }

    private boolean esVigente(final String token) {
        try {
            validar(new CredencialMatricula(token, null));
            return true;
        } catch (final ApplicationException exception) {
            if (CODIGO_TOKEN_INVALIDO.equals(exception.getCode())) {
                return false;
            }
            throw exception;
        }
    }

    private static VentanaRotada aVentanaRotada(final ProcedureRow fila) {
        return new VentanaRotada(
                aUuid(fila.extras().get(0)), aUuid(fila.extras().get(1)),
                aInstante(fila.extras().get(2)), aInstante(fila.extras().get(3)));
    }

    private static MatriculaRealizada aMatricula(final ProcedureRow fila) {
        return new MatriculaRealizada(aUuid(fila.extras().get(0)), aUuid(fila.extras().get(1)), aUuid(fila.extras().get(2)));
    }

    private static UUID aUuid(final Object valor) {
        return valor instanceof UUID uuid ? uuid : UUID.fromString(String.valueOf(valor));
    }

    /** Las fechas de la DB son DATETIME2 en UTC sin zona: se interpretan siempre como UTC. */
    private static Instant aInstante(final Object valor) {
        return switch (valor) {
            case Instant instante -> instante;
            case LocalDateTime local -> local.toInstant(ZoneOffset.UTC);
            case OffsetDateTime offset -> offset.toInstant();
            // java.sql.Timestamp (subclase de Date): sus campos locales son el valor DATETIME2 sin zona.
            case java.util.Date fecha -> LocalDateTime.ofInstant(fecha.toInstant(), ZoneId.systemDefault()).toInstant(ZoneOffset.UTC);
            default -> LocalDateTime.parse(String.valueOf(valor).replace(' ', 'T')).toInstant(ZoneOffset.UTC);
        };
    }
}
