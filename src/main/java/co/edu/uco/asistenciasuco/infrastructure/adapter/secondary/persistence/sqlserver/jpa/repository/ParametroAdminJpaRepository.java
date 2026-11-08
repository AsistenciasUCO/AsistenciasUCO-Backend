package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 D.3 (HU168): {@code usp_consultar_parametros_paginado}, {@code usp_actualizar_parametro} y
 * {@code usp_consultar_cambios_parametro_paginado} (JPA, {@code EXEC} nativo). La DB bloquea la fila del parametro
 * (UPDLOCK, HOLDLOCK) y compara {@code fechaModificacion} exacta en DATETIME: la fecha UTC con milisegundos viaja sin
 * perdida entre JSON, {@link LocalDateTime} y la columna.
 */
@Repository
public class ParametroAdminJpaRepository implements ParametroAdminRepositoryPort {

    static final String OP_ACTUALIZAR = "actualizarParametro";
    static final String SQL_ACTUALIZAR = """
            EXEC dbo.usp_actualizar_parametro
                 @idParametro = :idParametro, @valor = :valor, @fechaModificacionEsperada = :fechaModificacionEsperada,
                 @motivo = :motivo, @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_CONSULTAR = """
            EXEC dbo.usp_consultar_parametros_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina,
                 @grupo = :grupo, @texto = :texto, @soloEditables = :soloEditables
            """;
    static final String SQL_CAMBIOS = """
            EXEC dbo.usp_consultar_cambios_parametro_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @idParametro = :idParametro,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina
            """;

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public ParametroAdminJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de parametros es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public PaginaParametros consultar(final UUID usuarioEjecutor, final int pagina, final int tamano, final String grupo,
                                      final String texto, final boolean soloEditables) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametrosConsulta = new LinkedHashMap<>();
        parametrosConsulta.put("idUsuarioEjecutor", usuarioEjecutor);
        parametrosConsulta.put("idCorrelacion", correlationId);
        parametrosConsulta.put("numeroPagina", pagina);
        parametrosConsulta.put("tamanoPagina", tamano);
        parametrosConsulta.put("grupo", grupo);
        parametrosConsulta.put("texto", texto);
        parametrosConsulta.put("soloEditables", soloEditables);
        final List<?> filas = consultarFilas("consultarParametrosAdmin", "No fue posible consultar los parametros.", SQL_CONSULTAR,
                parametrosConsulta);
        final List<ParametroAdmin> parametros = new ArrayList<>(filas.size());
        long total = 0;
        for (final Object fila : filas) {
            final Object[] c = (Object[]) fila;
            parametros.add(new ParametroAdmin(NativeRowValues.uuid(c[0]), NativeRowValues.texto(c[1]), NativeRowValues.texto(c[2]),
                    NativeRowValues.texto(c[3]), NativeRowValues.texto(c[4]), NativeRowValues.texto(c[5]), NativeRowValues.bit(c[6]),
                    decimal(c[7]), decimal(c[8]), permitidos(c[9]), NativeRowValues.texto(c[10]), NativeRowValues.texto(c[11]),
                    NativeRowValues.texto(c[12]), NativeRowValues.texto(c[13]), NativeRowValues.entero(c[14]), utc(c[15]), utc(c[16]),
                    NativeRowValues.texto(c[17])));
            total = ((Number) c[19]).longValue();
        }
        return new PaginaParametros(parametros, total);
    }

    @Override
    public ParametroActualizado actualizar(final UUID idParametro, final String valor, final LocalDateTime fechaModificacionEsperadaUtc,
                                           final String motivo, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idParametro", idParametro);
        parametros.put("valor", valor);
        parametros.put("fechaModificacionEsperada", fechaModificacionEsperadaUtc);
        parametros.put("motivo", motivo);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(OP_ACTUALIZAR, SQL_ACTUALIZAR, parametros, correlationId, 6);
        final List<Object> c = fila.extras();
        return new ParametroActualizado(NativeRowValues.uuid(c.get(0)), NativeRowValues.texto(c.get(1)), NativeRowValues.texto(c.get(2)),
                NativeRowValues.texto(c.get(3)), NativeRowValues.texto(c.get(4)), utc(c.get(5)));
    }

    @Override
    public PaginaCambios consultarCambios(final UUID usuarioEjecutor, final UUID idParametro, final int pagina, final int tamano) {
        final UUID correlationId = CorrelationIdContext.require();
        final List<?> filas = consultarFilas("consultarCambiosParametro", "No fue posible consultar el historial del parametro.",
                SQL_CAMBIOS, Map.of("idUsuarioEjecutor", usuarioEjecutor, "idCorrelacion", correlationId, "idParametro", idParametro,
                        "numeroPagina", pagina, "tamanoPagina", tamano));
        final List<CambioParametro> cambios = new ArrayList<>(filas.size());
        long total = 0;
        for (final Object fila : filas) {
            final Object[] c = (Object[]) fila;
            cambios.add(new CambioParametro(NativeRowValues.uuid(c[0]), NativeRowValues.texto(c[4]), NativeRowValues.texto(c[5]),
                    NativeRowValues.texto(c[6]), NativeRowValues.uuid(c[7]), NativeRowValues.texto(c[8]), NativeRowValues.uuid(c[9]),
                    utc(c[10])));
            total = ((Number) c[11]).longValue();
        }
        return new PaginaCambios(cambios, total);
    }

    private List<?> consultarFilas(final String operacion, final String mensaje, final String sql, final Map<String, Object> valores) {
        return JpaQueryExecutor.execute(operacion, mensaje, () -> {
            try {
                final var query = entityManager.createNativeQuery(sql);
                valores.forEach(query::setParameter);
                return query.getResultList();
            } catch (final PersistenceException exception) {
                throw DbCodeTranslator.traducir(exception, "parametro");
            }
        });
    }

    private static BigDecimal decimal(final Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof BigDecimal decimal ? decimal : new BigDecimal(valor.toString().strip());
    }

    private static List<String> permitidos(final Object valor) {
        if (valor == null || valor.toString().isBlank()) {
            return List.of();
        }
        return Arrays.stream(valor.toString().split(",")).map(String::strip).filter(v -> !v.isEmpty()).toList();
    }

    /** DATETIME/DATETIME2 guardado en UTC sin zona. */
    private static LocalDateTime utc(final Object valor) {
        final var instante = NativeRowValues.instanteUtc(valor);
        return instante == null ? null : LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
