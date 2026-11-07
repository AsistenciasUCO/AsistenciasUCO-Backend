package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdminIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ComparativaFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisInstitucionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PuntoTendenciaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpComparativaFacultadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpKpisInstitucionEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpPuntoTendenciaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** KPIs, comparativa de facultades y tendencia de la institucion del administrador: ejecuta los SP de consulta y mapea fielmente sus columnas. */
@Repository
public class AdminIndicadoresJpaRepository implements AdminIndicadoresQueryPort {

    static final String SQL_KPIS = """
            EXEC dbo.usp_consultar_kpis_institucion
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idPeriodoAcademico = :idPeriodoAcademico
            """;

    static final String SQL_COMPARATIVA = """
            EXEC dbo.usp_consultar_comparativa_facultades
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idPeriodoAcademico = :idPeriodoAcademico
            """;

    static final String SQL_TENDENCIA = """
            EXEC dbo.usp_consultar_tendencia_ausentismo
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idPeriodoAcademico = :idPeriodoAcademico,
                 @granularidad = :granularidad
            """;

    private final EntityManager entityManager;

    public AdminIndicadoresJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de indicadores es obligatorio.");
    }

    @Override
    public KpisInstitucionProjection consultarKpis(final UUID actorUsuarioId, final UUID idPeriodoAcademico) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarKpisInstitucion", "No fue posible consultar los indicadores de la institucion desde base de datos.",
                () -> {
                    final List<SpKpisInstitucionEntity> filas = ejecutar(SQL_KPIS, SpKpisInstitucionEntity.class, correlationId, actorUsuarioId,
                            Map.of(), idPeriodoAcademico);
                    if (filas.isEmpty()) {
                        throw new IllegalStateException("usp_consultar_kpis_institucion no devolvio fila.");
                    }
                    final SpKpisInstitucionEntity e = filas.getFirst();
                    return new KpisInstitucionProjection(e.idInstitucion(), e.nombreInstitucion(), e.idPeriodoAcademico(), e.nombrePeriodoAcademico(),
                            cero(e.totalFacultades()), cero(e.totalProgramas()), cero(e.totalEstudiantes()), cero(e.estudiantesRegulares()),
                            cero(e.estudiantesRiesgo()), cero(e.estudiantesPerdida()), e.tasaRetencion(), cero(e.sesionesEjecutadas()),
                            cero(e.faltasSinJustaCausa()), e.porcentajeAusentismo(), e.porcentajeAsistencia(), cero(e.sesionesVencidas()),
                            cero(e.sesionesConRegistro()), e.porcentajeCumplimiento(), cero(e.estudiantesConMatriculaActiva()),
                            cero(e.estudiantesRegistrados()), cero(e.docentesConGrupoActivo()), cero(e.docentesRegistrados()),
                            e.umbralAdvertencia(), e.umbralPerdida(),
                            cero(e.tomasDictadas()), e.horasDictadas(), e.horasFalta());
                });
    }

    @Override
    public List<ComparativaFacultadProjection> consultarComparativa(final UUID actorUsuarioId, final UUID idPeriodoAcademico) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarComparativaFacultades", "No fue posible consultar la comparativa de facultades desde base de datos.",
                () -> ejecutar(SQL_COMPARATIVA, SpComparativaFacultadEntity.class, correlationId, actorUsuarioId, Map.of(), idPeriodoAcademico).stream()
                        .map(e -> new ComparativaFacultadProjection(e.idFacultad(), e.nombreFacultad(), verdadero(e.estaActivaFacultad()),
                                cero(e.totalProgramas()), cero(e.totalEstudiantes()), cero(e.estudiantesRiesgo()), cero(e.estudiantesPerdida()),
                                e.tasaRetencion(), cero(e.sesionesEjecutadas()), cero(e.faltasSinJustaCausa()), e.porcentajeAusentismo()))
                        .toList());
    }

    @Override
    public List<PuntoTendenciaProjection> consultarTendencia(final UUID actorUsuarioId, final UUID idPeriodoAcademico, final String granularidad) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarTendenciaAusentismo", "No fue posible consultar la tendencia de ausentismo desde base de datos.",
                () -> ejecutar(SQL_TENDENCIA, SpPuntoTendenciaEntity.class, correlationId, actorUsuarioId, Map.of("granularidad", granularidad), idPeriodoAcademico).stream()
                        .map(e -> new PuntoTendenciaProjection(e.inicioIntervalo(), e.finIntervalo(), cero(e.sesionesEjecutadas()),
                                cero(e.faltasSinJustaCausa()), e.porcentajeAusentismo()))
                        .toList());
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
            throw DbCodeTranslator.traducir(exception);
        }
    }

    private static int cero(final Integer valor) {
        return valor == null ? 0 : valor;
    }

    private static boolean verdadero(final Boolean valor) {
        return Boolean.TRUE.equals(valor);
    }
}
