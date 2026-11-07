package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 B.2 (F-DOC-004, F-EST-002): resumen por horas de {@code uv_resumen_asistencia_estudiante_grupo} a traves de
 * los SP con ambito. Cada columna se lee por NOMBRE ({@link Tuple}); las columnas de horas que la vista agrega al final no
 * desplazan el mapeo. Horas y porcentajes salen con 2 decimales (HALF_UP).
 */
@Repository
public class ResumenAsistenciaJpaRepository implements ResumenAsistenciaQueryPort {

    static final String SQL_GRUPO_DOCENTE = """
            EXEC dbo.usp_consultar_resumen_asistencia_grupo_docente_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @idGrupo = :idGrupo,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina
            """;
    static final String SQL_ESTUDIANTE = """
            EXEC dbo.usp_consultar_resumen_asistencia_estudiante
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion
            """;

    private final EntityManager entityManager;

    public ResumenAsistenciaJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager del resumen de asistencia es obligatorio.");
    }

    @Override
    public PaginaResultado<FilaResumen> consultarResumenGrupoDocente(final UUID grupoId, final int numeroPagina, final int tamanoPagina,
                                                                    final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarResumenAsistenciaGrupoDocente",
                "No fue posible consultar el resumen de asistencia del grupo.", () -> {
                    final List<Tuple> filas = tuplas(SQL_GRUPO_DOCENTE, Map.of("idUsuarioEjecutor", usuarioEjecutor,
                            "idCorrelacion", correlacion, "idGrupo", grupoId, "numeroPagina", numeroPagina, "tamanoPagina", tamanoPagina));
                    final long total = filas.isEmpty() ? 0 : NativeRowValues.entero(filas.getFirst().get("totalRegistros"));
                    return new PaginaResultado<>(filas.stream().map(f -> aFila(f, NativeRowValues.texto(f.get("nombreCompletoEstudiante"))))
                            .toList(), numeroPagina, tamanoPagina, total);
                }, excepcion -> DbCodeTranslator.traducir(excepcion, "grupo"));
    }

    @Override
    public List<FilaResumen> consultarResumenEstudiante(final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarResumenAsistenciaEstudiante",
                "No fue posible consultar el resumen de asistencia del estudiante.", () -> {
                    final List<FilaResumen> resultado = new ArrayList<>();
                    for (final Tuple f : tuplas(SQL_ESTUDIANTE, Map.of("idUsuarioEjecutor", usuarioEjecutor, "idCorrelacion", correlacion))) {
                        resultado.add(aFila(f, null));
                    }
                    return resultado;
                }, DbCodeTranslator::traducir);
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> tuplas(final String sql, final Map<String, Object> parametros) {
        final var consulta = entityManager.createNativeQuery(sql, Tuple.class);
        parametros.forEach(consulta::setParameter);
        return (List<Tuple>) consulta.getResultList();
    }

    static FilaResumen aFila(final Tuple f, final String nombreCompleto) {
        return new FilaResumen(NativeRowValues.uuid(f.get("idGrupo")), NativeRowValues.uuid(f.get("idEstudiante")), nombreCompleto,
                NativeRowValues.texto(f.get("codigoEstadoEstudiante")), new IndicadoresAsistenciaDTO(
                NativeRowValues.entero(f.get("sesionesEjecutadas")),
                NativeRowValues.decimal2(f.get("horasDictadas")),
                NativeRowValues.decimal2(f.get("horasAsistidas")),
                NativeRowValues.decimal2(f.get("horasFalta")),
                NativeRowValues.decimal2(f.get("horasExcusa")),
                NativeRowValues.decimal2(f.get("horasSinRegistro")),
                NativeRowValues.decimal2(f.get("porcentajeInasistencia")),
                NativeRowValues.decimal2(f.get("porcentajeAsistenciaHoras")),
                NativeRowValues.texto(f.get("codigoPermanencia")),
                NativeRowValues.decimal2(f.get("umbralAdvertencia")),
                NativeRowValues.decimal2(f.get("umbralPerdida"))));
    }
}
