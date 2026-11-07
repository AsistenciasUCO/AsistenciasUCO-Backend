package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvGrupoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvPeriodoAcademicoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * CULM-001 B.1: dias festivos via procedimientos almacenados (JPA, {@code EXEC} nativo). La DB valida anio, fuente,
 * cantidad y formato del JSON ({@code FES_001..FES_004}), el RBAC ADMINISTRADOR y el ambito por institucion; este
 * adaptador solo serializa {@code [{"fecha","nombre"}]} y traduce las filas. Sin {@code @Transactional}: cada SP
 * controla su transaccion (los de aplicacion de festivos hacen una por grupo).
 */
@Repository
public class DiaFestivoJpaRepository implements DiaFestivoRepositoryPort {

    static final String OP_CARGAR = "cargarDiasFestivos";
    static final String OP_CARGAR_AUTOMATICO = "cargarDiasFestivosAutomatico";
    static final String OP_APLICAR_PERIODO = "aplicarFestivosPeriodo";
    static final String OP_APLICAR_AUTOMATICO = "aplicarFestivosAutomatico";

    static final String SQL_CARGAR = """
            EXEC dbo.usp_cargar_dias_festivos
                 @anio = :anio, @festivosJSON = :festivosJSON, @fuente = :fuente,
                 @diferenciasCalculoLocal = :diferenciasCalculoLocal,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_CARGAR_AUTOMATICO = """
            EXEC dbo.usp_cargar_dias_festivos_automatico
                 @anio = :anio, @festivosJSON = :festivosJSON, @fuente = :fuente,
                 @diferenciasCalculoLocal = :diferenciasCalculoLocal, @idCorrelacion = :idCorrelacion
            """;
    static final String SQL_APLICAR_PERIODO = """
            EXEC dbo.usp_aplicar_festivos_periodo
                 @anio = :anio, @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_APLICAR_AUTOMATICO = """
            EXEC dbo.usp_aplicar_festivos_automatico @anio = :anio, @idCorrelacion = :idCorrelacion
            """;
    static final String SQL_CARGA_ANIO = """
            EXEC dbo.usp_consultar_carga_festivos_anio @anio = :anio, @idCorrelacion = :idCorrelacion
            """;
    static final String SQL_CONSULTAR_FESTIVOS = """
            EXEC dbo.usp_consultar_dias_festivos
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @anio = :anio,
                 @incluirInactivos = :incluirInactivos, @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina
            """;
    static final String SQL_SESIONES_CON_ASISTENCIA = """
            EXEC dbo.usp_consultar_sesiones_festivo_con_asistencia_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @anio = :anio,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina
            """;
    static final String HQL_RANGO_PERIODO = "select p from UvPeriodoAcademicoEntity p where p.id = :id order by p.id";
    static final String HQL_RANGO_GRUPO = "select g from UvGrupoEntity g where g.id = :id order by g.id";

    private static final String P_ANIO = "anio";
    private static final String P_CORRELACION = "idCorrelacion";
    private static final String P_USUARIO = "idUsuarioEjecutor";
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public DiaFestivoJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de festivos es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public CargaFestivos cargar(final int anio, final List<FestivoColombia> festivos, final String fuente,
                                final int diferenciasCalculoLocal, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = parametrosCarga(anio, festivos, fuente, diferenciasCalculoLocal, correlationId);
        parametros.put(P_USUARIO, usuarioEjecutor);
        return aCarga(procedureExecutor.executeWithExtras(OP_CARGAR, SQL_CARGAR, parametros, correlationId, 5));
    }

    @Override
    public CargaFestivos cargarAutomatico(final int anio, final List<FestivoColombia> festivos, final String fuente,
                                          final int diferenciasCalculoLocal) {
        final UUID correlationId = CorrelationIdContext.require();
        return aCarga(procedureExecutor.executeWithExtras(OP_CARGAR_AUTOMATICO, SQL_CARGAR_AUTOMATICO,
                parametrosCarga(anio, festivos, fuente, diferenciasCalculoLocal, correlationId), correlationId, 5));
    }

    @Override
    public AplicacionFestivos aplicarAnio(final int anio, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(P_ANIO, anio);
        parametros.put(P_CORRELACION, correlationId);
        parametros.put(P_USUARIO, usuarioEjecutor);
        return aAplicacion(procedureExecutor.executeWithExtras(OP_APLICAR_PERIODO, SQL_APLICAR_PERIODO, parametros,
                correlationId, 3));
    }

    @Override
    public AplicacionFestivos aplicarAutomatico(final int anio) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(P_ANIO, anio);
        parametros.put(P_CORRELACION, correlationId);
        return aAplicacion(procedureExecutor.executeWithExtras(OP_APLICAR_AUTOMATICO, SQL_APLICAR_AUTOMATICO, parametros,
                correlationId, 3));
    }

    @Override
    public int contarFestivosActivos(final int anio) {
        final UUID correlationId = CorrelationIdContext.require();
        final List<Object[]> filas = consultarFilas("consultarCargaFestivosAnio",
                "No fue posible consultar la carga de festivos del anio.", SQL_CARGA_ANIO,
                q -> q.setParameter(P_ANIO, anio).setParameter(P_CORRELACION, correlationId));
        return filas.isEmpty() ? 0 : entero(filas.getFirst()[1]);
    }

    @Override
    public PaginaFestivos consultarFestivos(final int anio, final boolean incluirInactivos, final int numeroPagina,
                                            final int tamanoPagina, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final List<Object[]> filas = consultarFilas("consultarDiasFestivos",
                "No fue posible consultar los dias festivos.", SQL_CONSULTAR_FESTIVOS,
                q -> q.setParameter(P_USUARIO, usuarioEjecutor).setParameter(P_CORRELACION, correlationId)
                        .setParameter(P_ANIO, anio).setParameter("incluirInactivos", incluirInactivos)
                        .setParameter("numeroPagina", numeroPagina).setParameter("tamanoPagina", tamanoPagina));
        final List<FestivoRegistrado> festivos = new ArrayList<>(filas.size());
        for (final Object[] c : filas) {
            festivos.add(new FestivoRegistrado(fecha(c[1]), texto(c[3]), texto(c[4]), Boolean.TRUE.equals(c[5])));
        }
        if (filas.isEmpty()) {
            return new PaginaFestivos(List.of(), 0, null, null, null);
        }
        final Object[] primera = filas.getFirst();
        return new PaginaFestivos(festivos, entero(primera[11]), texto(primera[8]), fechaHora(primera[9]),
                primera[10] == null ? null : entero(primera[10]));
    }

    @Override
    public PaginaSesionesFestivo consultarSesionesConAsistencia(final int anio, final int numeroPagina,
                                                                final int tamanoPagina, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final List<Object[]> filas = consultarFilas("consultarSesionesFestivoConAsistencia",
                "No fue posible consultar las sesiones en festivo con asistencia.", SQL_SESIONES_CON_ASISTENCIA,
                q -> q.setParameter(P_USUARIO, usuarioEjecutor).setParameter(P_CORRELACION, correlationId)
                        .setParameter(P_ANIO, anio).setParameter("numeroPagina", numeroPagina)
                        .setParameter("tamanoPagina", tamanoPagina));
        final List<SesionFestivoConAsistencia> sesiones = new ArrayList<>(filas.size());
        for (final Object[] c : filas) {
            sesiones.add(new SesionFestivoConAsistencia(NativeRowValues.uuid(c[0]), NativeRowValues.uuid(c[1]),
                    texto(c[2]), texto(c[3]), texto(c[4]), fechaHora(c[5]), fecha(c[6]), texto(c[7]), entero(c[8]),
                    entero(c[9])));
        }
        return new PaginaSesionesFestivo(sesiones, filas.isEmpty() ? 0 : entero(filas.getFirst()[10]));
    }

    @Override
    public Optional<RangoFechas> consultarRangoPeriodo(final UUID periodoAcademicoId) {
        Objects.requireNonNull(periodoAcademicoId, "El periodo academico es obligatorio.");
        return JpaQueryExecutor.execute("consultarRangoPeriodoFestivos",
                "No fue posible consultar las fechas del periodo academico.",
                () -> entityManager.createQuery(HQL_RANGO_PERIODO, UvPeriodoAcademicoEntity.class)
                        .setParameter("id", periodoAcademicoId)
                        .setMaxResults(1)
                        .getResultList().stream().findFirst()
                        .map(p -> new RangoFechas(p.fechaInicio(), p.fechaFin())));
    }

    @Override
    public Optional<RangoFechas> consultarRangoPeriodoDeGrupo(final UUID grupoId) {
        Objects.requireNonNull(grupoId, "El grupo es obligatorio.");
        return JpaQueryExecutor.execute("consultarRangoPeriodoGrupoFestivos",
                "No fue posible consultar las fechas del periodo del grupo.",
                () -> entityManager.createQuery(HQL_RANGO_GRUPO, UvGrupoEntity.class)
                        .setParameter("id", grupoId)
                        .setMaxResults(1)
                        .getResultList().stream().findFirst()
                        .map(g -> new RangoFechas(g.fechaInicioPeriodoAcademico(), g.fechaFinPeriodoAcademico())));
    }

    private Map<String, Object> parametrosCarga(final int anio, final List<FestivoColombia> festivos, final String fuente,
                                                final int diferencias, final UUID correlationId) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(P_ANIO, anio);
        parametros.put("festivosJSON", serializar(festivos));
        parametros.put("fuente", fuente);
        parametros.put("diferenciasCalculoLocal", diferencias);
        parametros.put(P_CORRELACION, correlationId);
        return parametros;
    }

    /** Exactamente las dos claves que admite el SP: {@code [{"fecha":"YYYY-MM-DD","nombre":"..."}]}. */
    static String serializar(final List<FestivoColombia> festivos) {
        final List<Map<String, String>> items = new ArrayList<>();
        for (final FestivoColombia festivo : festivos == null ? List.<FestivoColombia>of() : festivos) {
            final Map<String, String> item = new LinkedHashMap<>();
            item.put("fecha", festivo.fecha().toString());
            item.put("nombre", festivo.nombre());
            items.add(item);
        }
        return JSON.writeValueAsString(items);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> consultarFilas(final String operacion, final String mensaje, final String sql,
                                          final Function<Query, Query> parametros) {
        return JpaQueryExecutor.execute(operacion, mensaje, () -> {
            try {
                final List<?> filas = parametros.apply(entityManager.createNativeQuery(sql)).getResultList();
                final List<Object[]> resultado = new ArrayList<>(filas.size());
                for (final Object fila : filas) {
                    resultado.add((Object[]) fila);
                }
                return resultado;
            } catch (final PersistenceException exception) {
                throw DbCodeTranslator.traducir(exception, "periodo academico");
            }
        });
    }

    private static CargaFestivos aCarga(final ProcedureRow fila) {
        final List<Object> c = fila.extras();
        return new CargaFestivos(entero(c.get(0)), entero(c.get(1)), entero(c.get(2)), entero(c.get(3)), entero(c.get(4)));
    }

    private static AplicacionFestivos aAplicacion(final ProcedureRow fila) {
        final List<Object> c = fila.extras();
        return new AplicacionFestivos(entero(c.get(0)), entero(c.get(1)), entero(c.get(2)));
    }

    private static int entero(final Object valor) {
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    private static String texto(final Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }

    private static LocalDate fecha(final Object valor) {
        return switch (valor) {
            case null -> null;
            case LocalDate local -> local;
            case java.util.Date dia -> LocalDateTime.ofInstant(Instant.ofEpochMilli(dia.getTime()), ZoneId.systemDefault()).toLocalDate();
            case LocalDateTime local -> local.toLocalDate();
            default -> LocalDate.parse(String.valueOf(valor).substring(0, 10));
        };
    }

    /** DATETIME2 en UTC sin zona: se conserva el valor almacenado tal cual. */
    private static LocalDateTime fechaHora(final Object valor) {
        return switch (valor) {
            case null -> null;
            case LocalDateTime local -> local;
            case java.util.Date fecha -> LocalDateTime.ofInstant(Instant.ofEpochMilli(fecha.getTime()), ZoneId.systemDefault());
            default -> LocalDateTime.parse(String.valueOf(valor).replace(' ', 'T'));
        };
    }
}
