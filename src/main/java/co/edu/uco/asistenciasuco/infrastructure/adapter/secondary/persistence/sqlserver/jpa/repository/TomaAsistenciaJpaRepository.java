package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.AsistenciaTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.FaltanteTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomasSesionResumen;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvTomaAsistenciaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * CULM-001 B.2 (DEC-013): tomas de asistencia via procedimientos almacenados (JPA, {@code EXEC} nativo). Los comandos
 * validan la cardinalidad exacta de la fila canonica mas sus columnas adicionales; las consultas leen cada columna por su
 * NOMBRE ({@link Tuple}), de modo que una columna nueva al final del SP no rompe el mapeo. El ambito (RBAC DOCENTE y
 * titularidad) lo fuerza cada SP; un rechazo de consulta llega como {@code THROW} con {@code DBCODE} y se traduce con
 * {@link DbCodeTranslator}.
 */
@Repository
public class TomaAsistenciaJpaRepository implements TomaAsistenciaRepositoryPort {

    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final String P_CORRELACION = "idCorrelacion";
    private static final String P_USUARIO = "idUsuarioEjecutor";
    private static final String P_TOMA = "idToma";
    private static final String C_TOTAL = "totalRegistros";

    static final String SQL_ABRIR = """
            EXEC dbo.usp_abrir_toma_asistencia
                 @idSesion = :idSesion, @codigoModo = :codigoModo,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_CERRAR = """
            EXEC dbo.usp_cerrar_toma_asistencia
                 @idToma = :idToma, @marcarAusentes = :marcarAusentes,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_CERRAR_VENCIDAS = "EXEC dbo.usp_cerrar_tomas_vencidas @idCorrelacion = :idCorrelacion";
    static final String SQL_REGISTRAR_LOTE = """
            EXEC dbo.usp_registrar_asistencias_toma
                 @idToma = :idToma, @asistenciaJSON = :asistenciaJSON,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_TOMAS_SESION = """
            EXEC dbo.usp_consultar_tomas_sesion
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @idSesion = :idSesion
            """;
    static final String SQL_ASISTENCIAS = """
            EXEC dbo.usp_consultar_asistencias_toma_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @idToma = :idToma,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina
            """;
    static final String SQL_FALTANTES = """
            EXEC dbo.usp_consultar_faltantes_toma_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @idToma = :idToma,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina
            """;
    static final String HQL_TOMA = "select t from UvTomaAsistenciaEntity t where t.idToma = :idToma";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public TomaAsistenciaJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de tomas es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public TomaAbierta abrir(final UUID sesionId, final String codigoModo, final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSesion", sesionId);
        parametros.put("codigoModo", codigoModo);
        parametros.put(P_CORRELACION, correlacion);
        parametros.put(P_USUARIO, usuarioEjecutor);
        // idToma, numero, codigoModo, usaCodigoQr, fechaAperturaUtc, ventanaCierreUtc, totalTomasSesion, horasToma
        final List<Object> c = procedureExecutor.executeWithExtras("abrirTomaAsistencia", SQL_ABRIR, parametros, correlacion, 8).extras();
        return new TomaAbierta(NativeRowValues.uuid(c.get(0)), NativeRowValues.entero(c.get(1)), NativeRowValues.texto(c.get(2)),
                NativeRowValues.instanteUtc(c.get(4)));
    }

    @Override
    public CierreToma cerrar(final UUID tomaId, final Boolean marcarAusentes, final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(P_TOMA, tomaId);
        // Un BIT nulo sin tipo no tiene JdbcType resoluble: viaja como texto o NULL y SQL Server lo convierte.
        parametros.put("marcarAusentes", marcarAusentes == null ? null : (marcarAusentes ? "1" : "0"));
        parametros.put(P_CORRELACION, correlacion);
        parametros.put(P_USUARIO, usuarioEjecutor);
        // idToma, idSesion, idGrupo, fechaCierreUtc, totalAusentesMarcados, totalFaltantes, yaEstabaCerrada
        final List<Object> c = procedureExecutor.executeWithExtras("cerrarTomaAsistencia", SQL_CERRAR, parametros, correlacion, 7).extras();
        return new CierreToma(NativeRowValues.uuid(c.get(0)), NativeRowValues.uuid(c.get(1)), NativeRowValues.uuid(c.get(2)),
                NativeRowValues.entero(c.get(4)), NativeRowValues.entero(c.get(5)), NativeRowValues.bit(c.get(6)));
    }

    @Override
    public List<TomaCerradaAutomaticamente> cerrarVencidas() {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("cerrarTomasVencidas", "No fue posible cerrar las tomas vencidas.", () -> {
            final List<Tuple> filas = tuplas(SQL_CERRAR_VENCIDAS, Map.of(P_CORRELACION, correlacion));
            final List<TomaCerradaAutomaticamente> cerradas = new ArrayList<>(filas.size());
            for (final Tuple f : filas) {
                cerradas.add(new TomaCerradaAutomaticamente(NativeRowValues.uuid(f.get(P_TOMA)), NativeRowValues.uuid(f.get("idSesion")),
                        NativeRowValues.uuid(f.get("idGrupo")), NativeRowValues.entero(f.get("totalAusentesMarcados"))));
            }
            return cerradas;
        });
    }

    @Override
    public LoteRegistrado registrarLote(final UUID tomaId, final List<RegistroToma> registros, final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(P_TOMA, tomaId);
        parametros.put("asistenciaJSON", serializar(registros));
        parametros.put(P_CORRELACION, correlacion);
        parametros.put(P_USUARIO, usuarioEjecutor);
        // idToma, idSesion, idGrupo, totalRegistros
        final ProcedureRow fila = procedureExecutor.executeWithExtras("registrarAsistenciasToma", SQL_REGISTRAR_LOTE, parametros, correlacion, 4);
        final List<Object> c = fila.extras();
        return new LoteRegistrado(NativeRowValues.uuid(c.get(0)), NativeRowValues.uuid(c.get(1)), NativeRowValues.uuid(c.get(2)),
                NativeRowValues.entero(c.get(3)));
    }

    @Override
    public Optional<TomaAsistenciaResumen> consultarToma(final UUID tomaId) {
        return JpaQueryExecutor.execute("consultarTomaAsistencia", "No fue posible consultar la toma de asistencia.",
                () -> entityManager.createQuery(HQL_TOMA, UvTomaAsistenciaEntity.class)
                        .setParameter(P_TOMA, tomaId)
                        .getResultList().stream()
                        .findFirst()
                        .map(TomaAsistenciaJpaRepository::aResumen));
    }

    @Override
    public TomasSesionResumen consultarTomasSesion(final UUID sesionId, final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarTomasSesion", "No fue posible consultar las tomas de la sesion.", () -> {
            final List<Tuple> filas = tuplas(SQL_TOMAS_SESION, Map.of(P_USUARIO, usuarioEjecutor, P_CORRELACION, correlacion,
                    "idSesion", sesionId));
            if (filas.isEmpty()) {
                throw new IllegalStateException("usp_consultar_tomas_sesion no devolvio la fila de la sesion.");
            }
            final Tuple s = filas.getFirst();
            final List<TomaAsistenciaResumen> tomas = new ArrayList<>();
            for (final Tuple f : filas) {
                if (f.get(P_TOMA) != null) {
                    tomas.add(new TomaAsistenciaResumen(NativeRowValues.uuid(f.get(P_TOMA)), NativeRowValues.uuid(f.get("idSesion")),
                            NativeRowValues.uuid(f.get("idGrupo")), NativeRowValues.entero(f.get("numero")),
                            NativeRowValues.texto(f.get("codigoModo")), NativeRowValues.texto(f.get("codigoEstado")),
                            NativeRowValues.bit(f.get("estaAbierta")), NativeRowValues.bit(f.get("esImplicita")),
                            NativeRowValues.instanteUtc(f.get("fechaAperturaUtc")), NativeRowValues.instanteUtc(f.get("fechaCierreUtc")),
                            NativeRowValues.decimal2(f.get("horasToma")), NativeRowValues.entero(f.get(C_TOTAL)),
                            NativeRowValues.entero(f.get("totalPresentes")), NativeRowValues.entero(f.get("totalAusentes")),
                            NativeRowValues.entero(f.get("totalExcusas")), NativeRowValues.bit(f.get("tieneQrActivo"))));
                }
            }
            return new TomasSesionResumen(NativeRowValues.uuid(s.get("idSesion")), NativeRowValues.uuid(s.get("idGrupo")),
                    NativeRowValues.decimal2(s.get("horasSesion")), NativeRowValues.bit(s.get("estaSesionCancelada")),
                    NativeRowValues.instanteUtc(s.get("ventanaAperturaUtc")), NativeRowValues.instanteUtc(s.get("ventanaCierreUtc")),
                    NativeRowValues.bit(s.get("ventanaAbierta")), NativeRowValues.entero(s.get("maximoTomas")), tomas);
        }, excepcion -> DbCodeTranslator.traducir(excepcion, "sesion"));
    }

    @Override
    public PaginaResultado<AsistenciaTomaFila> consultarAsistencias(final UUID tomaId, final int numeroPagina, final int tamanoPagina,
                                                                   final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarAsistenciasToma", "No fue posible consultar las asistencias de la toma.", () -> {
            final List<Tuple> filas = tuplas(SQL_ASISTENCIAS, paginacion(tomaId, numeroPagina, tamanoPagina, usuarioEjecutor, correlacion));
            final List<AsistenciaTomaFila> elementos = new ArrayList<>(filas.size());
            for (final Tuple f : filas) {
                elementos.add(new AsistenciaTomaFila(NativeRowValues.uuid(f.get("idEstudiante")),
                        NativeRowValues.texto(f.get("nombreCompletoEstudiante")), NativeRowValues.texto(f.get("codigoRazonCausa")),
                        NativeRowValues.bit(f.get("registradoPorEstudiante")), NativeRowValues.instanteUtc(f.get("fechaRegistroUtc"))));
            }
            return new PaginaResultado<>(elementos, numeroPagina, tamanoPagina, total(filas));
        }, excepcion -> DbCodeTranslator.traducir(excepcion, "toma"));
    }

    @Override
    public PaginaResultado<FaltanteTomaFila> consultarFaltantes(final UUID tomaId, final int numeroPagina, final int tamanoPagina,
                                                               final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarFaltantesToma", "No fue posible consultar los faltantes de la toma.", () -> {
            final List<Tuple> filas = tuplas(SQL_FALTANTES, paginacion(tomaId, numeroPagina, tamanoPagina, usuarioEjecutor, correlacion));
            final List<FaltanteTomaFila> elementos = new ArrayList<>(filas.size());
            for (final Tuple f : filas) {
                elementos.add(new FaltanteTomaFila(NativeRowValues.uuid(f.get("idEstudiante")),
                        NativeRowValues.texto(f.get("nombreCompletoEstudiante")), NativeRowValues.bit(f.get("tieneRegistro")),
                        NativeRowValues.texto(f.get("codigoRazonCausa")), NativeRowValues.bit(f.get("fueMarcadoAlCerrar"))));
            }
            return new PaginaResultado<>(elementos, numeroPagina, tamanoPagina, total(filas));
        }, excepcion -> DbCodeTranslator.traducir(excepcion, "toma"));
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> tuplas(final String sql, final Map<String, Object> parametros) {
        final var consulta = entityManager.createNativeQuery(sql, Tuple.class);
        parametros.forEach(consulta::setParameter);
        return (List<Tuple>) consulta.getResultList();
    }

    private static Map<String, Object> paginacion(final UUID tomaId, final int numeroPagina, final int tamanoPagina,
                                                  final UUID usuarioEjecutor, final UUID correlacion) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(P_USUARIO, usuarioEjecutor);
        parametros.put(P_CORRELACION, correlacion);
        parametros.put(P_TOMA, tomaId);
        parametros.put("numeroPagina", numeroPagina);
        parametros.put("tamanoPagina", tamanoPagina);
        return parametros;
    }

    private static long total(final List<Tuple> filas) {
        return filas.isEmpty() ? 0 : NativeRowValues.entero(filas.getFirst().get(C_TOTAL));
    }

    static TomaAsistenciaResumen aResumen(final UvTomaAsistenciaEntity e) {
        return new TomaAsistenciaResumen(e.idToma(), e.idSesion(), e.idGrupo(), NativeRowValues.entero(e.numero()),
                e.codigoModo(), e.codigoEstado(), Boolean.TRUE.equals(e.estaAbierta()), Boolean.TRUE.equals(e.esImplicita()),
                e.fechaAperturaUtc() == null ? null : e.fechaAperturaUtc().toInstant(ZoneOffset.UTC),
                e.fechaCierreUtc() == null ? null : e.fechaCierreUtc().toInstant(ZoneOffset.UTC),
                NativeRowValues.decimal2(e.horasToma()), NativeRowValues.entero(e.totalRegistros()),
                NativeRowValues.entero(e.totalPresentes()), NativeRowValues.entero(e.totalAusentes()),
                NativeRowValues.entero(e.totalExcusas()), Boolean.TRUE.equals(e.tieneQrActivo()));
    }

    /** Exactamente {@code [{"idEstudiante":"<UUID>","estado":"<valor>"}]}, en el orden recibido (contrato 2.8.2). */
    static String serializar(final List<RegistroToma> registros) {
        final List<Map<String, String>> items = new ArrayList<>(registros.size());
        for (final RegistroToma registro : registros) {
            final Map<String, String> item = new LinkedHashMap<>();
            item.put("idEstudiante", registro.idEstudiante().toString());
            item.put("estado", registro.estado());
            items.add(item);
        }
        return JSON.writeValueAsString(items);
    }
}
