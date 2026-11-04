package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Repository;

/**
 * HU112/HU128/HU143/HU157: native EXEC de las consultas del decano y del administrador. Las filas llegan como
 * {@code Object[]} (sin entidades: una fila por programa-plan o por matricula no tiene un identificador unico) y los
 * UNIQUEIDENTIFIER pueden llegar como texto ({@link NativeRowValues#uuid}). El SP exige el perfil y filtra por el
 * ambito del ejecutor; sus rechazos se traducen con {@link DbCodeTranslator} (PAG_001/VAL_001 -> 400, SEC_001 -> 403).
 */
@Repository
public class ConsultasTransversalesJpaRepository implements ConsultasTransversalesQueryPort {

    static final String SQL_PROGRAMAS_CREDITOS = """
            EXEC dbo.usp_consultar_programas_creditos_facultad_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @busqueda = :busqueda,
                 @estaActivo = :estaActivo
            """;
    static final String SQL_DOCENTES = """
            EXEC dbo.usp_consultar_docentes_facultad_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @idPrograma = :idPadre,
                 @busqueda = :busqueda,
                 @estaActivo = :estaActivo
            """;
    static final String SQL_ASIGNATURAS = """
            EXEC dbo.usp_buscar_asignaturas_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @busqueda = :busqueda,
                 @idFacultad = :idPadre,
                 @estaActiva = :estaActivo
            """;
    static final String SQL_ESTUDIANTES = """
            EXEC dbo.usp_buscar_estudiantes_institucion_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @busqueda = :busqueda,
                 @idFacultad = :idPadre,
                 @estaActivo = :estaActivo
            """;
    static final String SQL_HISTORIAL = """
            EXEC dbo.usp_consultar_historial_estudiante
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idEstudiante = :idEstudiante
            """;
    static final String SQL_CONSOLIDADO = """
            EXEC dbo.usp_consultar_consolidado_asistencia_grupos_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @idPeriodoAcademico = :idPeriodoAcademico,
                 @idPrograma = :idPrograma,
                 @idAsignatura = :idAsignatura,
                 @idDocente = :idDocente
            """;
    static final String SQL_AUDITORIA = """
            EXEC dbo.usp_consultar_auditoria_eventos_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @desdeUtc = :desdeUtc,
                 @hastaUtc = :hastaUtc,
                 @accion = :accion,
                 @tipoRecurso = :tipoRecurso,
                 @idRecurso = :idRecurso,
                 @idUsuarioActor = :idUsuarioActor,
                 @resultado = :resultado,
                 @soloAsistencia = :soloAsistencia
            """;
    static final String SQL_COMPONENTES = """
            EXEC dbo.usp_consultar_componentes_formacion
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;
    /** HU155: lectura directa de las vistas de catalogo (sin SP: no hay ambito ni escritura). */
    static final String SQL_ESTADOS = """
            SELECT catalogo = 'SOLICITUD', e.id, e.codigo, e.nombre FROM dbo.uv_estado e
            UNION ALL
            SELECT catalogo = 'ESTUDIANTE_GRUPO', g.id, g.codigo, g.nombre FROM dbo.uv_estado_estudiante_grupo g
            ORDER BY catalogo DESC, nombre
            """;
    private static final DateTimeFormatter FORMATO_DATETIMEOFFSET = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
            .appendLiteral(' ').appendOffset("+HH:MM", "Z").toFormatter();

    private final EntityManager entityManager;

    public ConsultasTransversalesJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager es obligatorio.");
    }

    @Override
    public Pagina<ProgramaCreditosProjection> consultarProgramasCreditos(final UUID idUsuarioEjecutor,
                                                                         final String busqueda,
                                                                         final Boolean estaActivo, final int pagina,
                                                                         final int tamano) {
        return pagina("consultarProgramasCreditosFacultad",
                "No fue posible consultar los programas y creditos de la facultad.", SQL_PROGRAMAS_CREDITOS,
                idUsuarioEjecutor, q -> paginar(q, pagina, tamano).setParameter("busqueda", busqueda)
                        .setParameter("estaActivo", estaActivo), 12,
                f -> new ProgramaCreditosProjection(NativeRowValues.uuid(f[0]), texto(f[1]), texto(f[2]), texto(f[3]),
                        texto(f[4]), verdadero(f[5]), NativeRowValues.uuid(f[6]), entero(f[7]), verdadero(f[8]),
                        cero(f[9]), cero(f[10]), cero(f[11])));
    }

    @Override
    public Pagina<DocenteFacultadProjection> consultarDocentes(final UUID idUsuarioEjecutor,
                                                               final FiltroConsulta filtro, final int pagina,
                                                               final int tamano) {
        return pagina("consultarDocentesFacultad", "No fue posible consultar los docentes de la facultad.",
                SQL_DOCENTES, idUsuarioEjecutor, q -> filtrar(paginar(q, pagina, tamano), filtro), 9,
                f -> new DocenteFacultadProjection(NativeRowValues.uuid(f[0]), texto(f[2]), texto(f[3]), texto(f[4]),
                        verdadero(f[5]), texto(f[6]), cero(f[7]), cero(f[8])));
    }

    @Override
    public Pagina<AsignaturaProjection> buscarAsignaturas(final UUID idUsuarioEjecutor, final FiltroConsulta filtro,
                                                          final int pagina, final int tamano) {
        return pagina("buscarAsignaturas", "No fue posible buscar las asignaturas.", SQL_ASIGNATURAS,
                idUsuarioEjecutor, q -> filtrar(paginar(q, pagina, tamano), filtro), 14,
                f -> new AsignaturaProjection(NativeRowValues.uuid(f[0]), texto(f[1]), texto(f[2]), entero(f[3]),
                        texto(f[4]), texto(f[5]), texto(f[6]), NativeRowValues.uuid(f[7]), entero(f[8]),
                        NativeRowValues.uuid(f[9]), texto(f[10]), NativeRowValues.uuid(f[11]), texto(f[12]),
                        verdadero(f[13])));
    }

    @Override
    public Pagina<EstudianteInstitucionProjection> buscarEstudiantes(final UUID idUsuarioEjecutor,
                                                                     final FiltroConsulta filtro, final int pagina,
                                                                     final int tamano) {
        return pagina("buscarEstudiantesInstitucion", "No fue posible buscar los estudiantes de la institucion.",
                SQL_ESTUDIANTES, idUsuarioEjecutor, q -> filtrar(paginar(q, pagina, tamano), filtro), 9,
                f -> new EstudianteInstitucionProjection(NativeRowValues.uuid(f[0]), texto(f[2]), texto(f[3]),
                        texto(f[4]), verdadero(f[5]), texto(f[6]), texto(f[7]), cero(f[8])));
    }

    @Override
    public List<HistorialEstudianteProjection> consultarHistorial(final UUID idUsuarioEjecutor,
                                                                  final UUID idEstudiante) {
        return filas("consultarHistorialEstudiante", "No fue posible consultar el historial del estudiante.",
                SQL_HISTORIAL, idUsuarioEjecutor, q -> q.setParameter("idEstudiante", idEstudiante))
                .stream().map(ConsultasTransversalesJpaRepository::historial).toList();
    }

    @Override
    public Pagina<ConsolidadoGrupoProjection> consultarConsolidadoAsistencia(final UUID idUsuarioEjecutor,
                                                                             final FiltroConsolidado filtro,
                                                                             final int pagina, final int tamano) {
        return pagina("consultarConsolidadoAsistencia", "No fue posible consultar el consolidado de asistencia.",
                SQL_CONSOLIDADO, idUsuarioEjecutor, q -> paginar(q, pagina, tamano)
                        .setParameter("idPeriodoAcademico", filtro.idPeriodoAcademico())
                        .setParameter("idPrograma", filtro.idPrograma())
                        .setParameter("idAsignatura", filtro.idAsignatura())
                        .setParameter("idDocente", filtro.idDocente()), 20,
                f -> new ConsolidadoGrupoProjection(NativeRowValues.uuid(f[0]), entero(f[1]), texto(f[2]),
                        NativeRowValues.uuid(f[3]), texto(f[4]), NativeRowValues.uuid(f[5]), texto(f[6]),
                        NativeRowValues.uuid(f[7]), texto(f[8]), NativeRowValues.uuid(f[9]), texto(f[10]),
                        cero(f[11]), cero(f[12]), cero(f[13]), cero(f[14]), cero(f[15]), decimal(f[16]),
                        decimal(f[17]), cero(f[18]), cero(f[19])));
    }

    @Override
    public Pagina<EventoAuditoriaProjection> consultarAuditoria(final UUID idUsuarioEjecutor,
                                                                final FiltroAuditoria filtro, final int pagina,
                                                                final int tamano) {
        return pagina("consultarAuditoria", "No fue posible consultar el rastro de auditoria.", SQL_AUDITORIA,
                idUsuarioEjecutor, q -> paginar(q, pagina, tamano)
                        .setParameter("desdeUtc", filtro.desde())
                        .setParameter("hastaUtc", filtro.hasta())
                        .setParameter("accion", filtro.accion())
                        .setParameter("tipoRecurso", filtro.tipoRecurso())
                        .setParameter("idRecurso", filtro.idRecurso())
                        .setParameter("idUsuarioActor", filtro.idUsuarioActor())
                        .setParameter("resultado", filtro.resultado())
                        .setParameter("soloAsistencia", filtro.soloAsistencia()), 17,
                f -> new EventoAuditoriaProjection(NativeRowValues.uuid(f[0]), fechaHora(f[1]), texto(f[2]),
                        texto(f[3]), texto(f[4]), texto(f[5]), texto(f[6]), texto(f[7]), texto(f[8]),
                        NativeRowValues.uuid(f[9]), texto(f[10]), texto(f[11]), entero(f[12]), texto(f[13]),
                        texto(f[14]), texto(f[15]), verdadero(f[16])));
    }

    @Override
    public List<ComponenteFormacionProjection> consultarComponentes(final UUID idUsuarioEjecutor) {
        return filas("consultarComponentesFormacion", "No fue posible consultar los componentes de formacion.",
                SQL_COMPONENTES, idUsuarioEjecutor, UnaryOperator.identity()).stream()
                .map(f -> new ComponenteFormacionProjection(NativeRowValues.uuid(f[0]), texto(f[1]), texto(f[2]),
                        cero(f[3])))
                .toList();
    }

    @Override
    public List<EstadoSistemaProjection> consultarEstadosSistema() {
        return JpaQueryExecutor.execute("consultarEstadosSistema", "No fue posible consultar los estados del sistema.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_ESTADOS).getResultList()).stream()
                        .map(Object[].class::cast)
                        .map(f -> new EstadoSistemaProjection(texto(f[0]), NativeRowValues.uuid(f[1]), texto(f[2]),
                                texto(f[3])))
                        .toList());
    }

    private static HistorialEstudianteProjection historial(final Object[] f) {
        final UUID idGrupo = NativeRowValues.uuid(f[15]);
        final ResumenAsistenciaProjection resumen = idGrupo == null ? null : new ResumenAsistenciaProjection(
                cero(f[21]), cero(f[22]), cero(f[23]), cero(f[24]), decimal(f[25]), texto(f[26]));
        return new HistorialEstudianteProjection(NativeRowValues.uuid(f[0]), texto(f[2]), texto(f[3]), texto(f[4]),
                texto(f[5]), verdadero(f[6]), NativeRowValues.uuid(f[7]), texto(f[8]), NativeRowValues.uuid(f[9]),
                texto(f[10]), entero(f[11]), NativeRowValues.uuid(f[12]), texto(f[13]), texto(f[14]), idGrupo,
                entero(f[16]), NativeRowValues.uuid(f[17]), texto(f[18]), texto(f[19]), texto(f[20]), resumen);
    }

    /** {@code columnaTotal} es la posicion de {@code totalRegistros} en el resultset del SP. */
    private <T> Pagina<T> pagina(final String operacion, final String mensajeFallo, final String sql,
                                 final UUID idUsuarioEjecutor, final UnaryOperator<Query> parametros,
                                 final int columnaTotal, final Function<Object[], T> mapper) {
        final List<Object[]> filas = filas(operacion, mensajeFallo, sql, idUsuarioEjecutor, parametros);
        return new Pagina<>(filas.stream().map(mapper).toList(),
                filas.isEmpty() ? 0 : cero(filas.getFirst()[columnaTotal]));
    }

    private List<Object[]> filas(final String operacion, final String mensajeFallo, final String sql,
                                 final UUID idUsuarioEjecutor, final UnaryOperator<Query> parametros) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(operacion, mensajeFallo, () -> {
            try {
                return ((List<?>) parametros.apply(entityManager.createNativeQuery(sql)
                                .setParameter("idUsuarioEjecutor", idUsuarioEjecutor)
                                .setParameter("idCorrelacion", correlationId))
                        .getResultList()).stream().map(Object[].class::cast).toList();
            } catch (final PersistenceException exception) {
                throw DbCodeTranslator.traducir(exception);
            }
        });
    }

    private static Query paginar(final Query query, final int pagina, final int tamano) {
        return query.setParameter("numeroPagina", pagina).setParameter("tamanoPagina", tamano);
    }

    private static Query filtrar(final Query query, final FiltroConsulta filtro) {
        return query.setParameter("busqueda", filtro.busqueda()).setParameter("idPadre", filtro.idPadre())
                .setParameter("estaActivo", filtro.estaActivo());
    }

    private static String texto(final Object valor) {
        return valor == null ? null : valor.toString().strip();
    }

    private static Integer entero(final Object valor) {
        return valor == null ? null : ((Number) valor).intValue();
    }

    private static int cero(final Object valor) {
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    private static boolean verdadero(final Object valor) {
        return Boolean.TRUE.equals(valor) || (valor instanceof Number numero && numero.intValue() == 1);
    }

    private static BigDecimal decimal(final Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof BigDecimal numero ? numero : new BigDecimal(valor.toString());
    }

    /** DATETIMEOFFSET: Hibernate lo entrega como OffsetDateTime; el driver puede entregar su propio tipo (texto ISO). */
    static OffsetDateTime fechaHora(final Object valor) {
        return switch (valor) {
            case null -> null;
            case OffsetDateTime fecha -> fecha;
            case Instant instante -> instante.atOffset(ZoneOffset.UTC);
            case LocalDateTime fecha -> fecha.atOffset(ZoneOffset.UTC);
            default -> OffsetDateTime.parse(valor.toString().strip(), FORMATO_DATETIMEOFFSET);
        };
    }
}
