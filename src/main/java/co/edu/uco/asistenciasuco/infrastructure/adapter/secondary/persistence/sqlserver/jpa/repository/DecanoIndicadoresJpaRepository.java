package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AforoGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ComparativaProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisDecanoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAforoGruposProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpAforoGrupoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpComparativaProgramaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpKpisDecanoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** KPIs, comparativa y aforo de la facultad del decano: ejecuta los SP de consulta y mapea fielmente sus columnas. */
@Repository
public class DecanoIndicadoresJpaRepository implements DecanoIndicadoresQueryPort {

    private static final String ENTIDAD_NO_ENCONTRADA = "programa";

    static final String SQL_KPIS = """
            EXEC dbo.usp_consultar_kpis_decano
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idPeriodoAcademico = :idPeriodoAcademico
            """;

    static final String SQL_COMPARATIVA = """
            EXEC dbo.usp_consultar_comparativa_programas_facultad
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idPeriodoAcademico = :idPeriodoAcademico
            """;

    static final String SQL_AFORO = """
            EXEC dbo.usp_consultar_aforo_grupos_facultad
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @idPeriodoAcademico = :idPeriodoAcademico,
                 @idPrograma = :idPrograma,
                 @soloHabilitados = :soloHabilitados
            """;

    private final EntityManager entityManager;

    public DecanoIndicadoresJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de indicadores es obligatorio.");
    }

    @Override
    public KpisDecanoProjection consultarKpis(final UUID actorUsuarioId, final UUID idPeriodoAcademico) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarKpisDecano", "No fue posible consultar los indicadores de la facultad desde base de datos.",
                () -> {
                    final List<SpKpisDecanoEntity> filas = ejecutar(SQL_KPIS, SpKpisDecanoEntity.class, correlationId, actorUsuarioId,
                            Map.of(), idPeriodoAcademico);
                    if (filas.isEmpty()) {
                        throw new IllegalStateException("usp_consultar_kpis_decano no devolvio fila.");
                    }
                    final SpKpisDecanoEntity e = filas.getFirst();
                    return new KpisDecanoProjection(e.idFacultad(), e.nombreFacultad(), e.idPeriodoAcademico(), e.nombrePeriodoAcademico(),
                            cero(e.totalProgramas()), cero(e.totalEstudiantes()), cero(e.estudiantesRegulares()), cero(e.estudiantesRiesgo()),
                            cero(e.estudiantesPerdida()), e.tasaRetencion(), cero(e.sesionesEjecutadas()), cero(e.faltasSinJustaCausa()),
                            e.porcentajeAusentismo(), e.porcentajeAsistencia(), cero(e.gruposActivos()), cero(e.cupoTotal()),
                            cero(e.matriculadosActivos()), e.tasaAforo(), e.umbralAdvertencia(), e.umbralPerdida(),
                            cero(e.tomasDictadas()), e.horasDictadas(), e.horasFalta());
                });
    }

    @Override
    public List<ComparativaProgramaProjection> consultarComparativa(final UUID actorUsuarioId, final UUID idPeriodoAcademico) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarComparativaProgramas", "No fue posible consultar la comparativa de programas desde base de datos.",
                () -> ejecutar(SQL_COMPARATIVA, SpComparativaProgramaEntity.class, correlationId, actorUsuarioId, Map.of(), idPeriodoAcademico).stream()
                        .map(e -> new ComparativaProgramaProjection(e.idPrograma(), e.nombrePrograma(), verdadero(e.estaActivoPrograma()),
                                cero(e.totalEstudiantes()), cero(e.estudiantesRiesgo()), cero(e.estudiantesPerdida()), e.tasaRetencion(),
                                cero(e.sesionesEjecutadas()), cero(e.faltasSinJustaCausa()), e.porcentajeAusentismo()))
                        .toList());
    }

    @Override
    public PaginaAforoGruposProjection consultarAforo(final UUID actorUsuarioId, final UUID idPeriodoAcademico, final UUID idPrograma,
                                                      final boolean soloHabilitados, final int pagina, final int tamano) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarAforoGrupos", "No fue posible consultar el aforo de los grupos desde base de datos.",
                () -> {
                    final Map<String, Object> extra = new LinkedHashMap<>();
                    extra.put("numeroPagina", pagina);
                    extra.put("tamanoPagina", tamano);
                    extra.put("idPrograma", idPrograma);
                    extra.put("soloHabilitados", soloHabilitados);
                    final List<SpAforoGrupoEntity> filas = ejecutar(SQL_AFORO, SpAforoGrupoEntity.class, correlationId, actorUsuarioId, extra, idPeriodoAcademico);
                    final int total = filas.isEmpty() ? 0 : cero(filas.getFirst().totalRegistros());
                    return new PaginaAforoGruposProjection(filas.stream()
                            .map(e -> new AforoGrupoProjection(e.idGrupo(), cero(e.codigoGrupo()), e.idAsignatura(), e.nombreAsignatura(), e.idPrograma(),
                                    e.nombrePrograma(), cero(e.cupoMaximo()), cero(e.matriculadosActivos()), cero(e.cuposDisponibles()), e.tasaAforo(),
                                    verdadero(e.grupoEstaHabilitado())))
                            .toList(), total);
                });
    }

    private <T> List<T> ejecutar(final String sql, final Class<T> tipo, final UUID correlationId, final UUID actor, final Map<String, Object> parametros,
                                 final UUID idPeriodoAcademico) {
        try {
            final Query query = entityManager.createNativeQuery(sql, tipo)
                    .setParameter("idUsuarioEjecutor", actor)
                    .setParameter("idCorrelacion", correlationId);
            for (final Map.Entry<String, Object> parametro : parametros.entrySet()) {
                query.setParameter(parametro.getKey(), parametro.getValue());
            }
            query.setParameter("idPeriodoAcademico", idPeriodoAcademico);
            return query.getResultList().stream().map(tipo::cast).toList();
        } catch (final PersistenceException exception) {
            throw DbCodeTranslator.traducir(exception, ENTIDAD_NO_ENCONTRADA);
        }
    }

    private static int cero(final Integer valor) {
        return valor == null ? 0 : valor;
    }

    private static boolean verdadero(final Boolean valor) {
        return Boolean.TRUE.equals(valor);
    }
}
