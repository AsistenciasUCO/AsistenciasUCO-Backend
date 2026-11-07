package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Sesiones de una asignatura para el estudiante (CULM-001 D.1, HU026B; antes LB-008 JPA-05 sobre {@code uv_sesion}) sobre
 * {@code usp_consultar_sesiones_materia_estudiante}: el SP resuelve el estudiante desde el usuario ejecutor, solo cuenta la
 * matricula inscrita ({@code MATRICULA/ESTADO_INSCRITA}), pagina con {@code COUNT(1) OVER()} y calcula la fecha limite de
 * justificacion ({@code fechaLimiteJustificacionUtc}, {@code plazoJustificacionVigente}). Las fechas DATETIME2 se leen
 * siempre como UTC literal (MAINT-003K): jamas con la zona de la JVM.
 */
@Repository
public class SesionMateriaEstudianteJpaRepository implements SesionMateriaEstudianteQueryPort {

    static final String SQL_SESIONES_MATERIA = """
            EXEC dbo.usp_consultar_sesiones_materia_estudiante
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idMateria = :idMateria,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;
    static final int COLUMNAS_SESIONES_MATERIA = 13;
    private static final int COLUMNA_TOTAL = 12;

    private final EntityManager entityManager;

    public SesionMateriaEstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de sesiones de materia es obligatorio.");
    }

    @Override
    public PaginaResultado<SesionMateriaEstudianteProjection> consultarSesionesMateria(final UUID usuarioEjecutor,
                                                                                       final UUID idAsignatura,
                                                                                       final int numeroPagina,
                                                                                       final int tamanoPagina) {
        if (usuarioEjecutor == null || idAsignatura == null) {
            throw new CrosscuttingException("El usuario ejecutor y la materia de la consulta de sesiones son obligatorios.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarSesionesMateria",
                "No fue posible consultar las sesiones de la materia desde base de datos.",
                () -> {
                    final List<?> filas = entityManager.createNativeQuery(SQL_SESIONES_MATERIA)
                            .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                            .setParameter("idCorrelacion", correlationId)
                            .setParameter("idMateria", idAsignatura)
                            .setParameter("numeroPagina", numeroPagina)
                            .setParameter("tamanoPagina", tamanoPagina)
                            .getResultList();
                    final List<SesionMateriaEstudianteProjection> sesiones = filas.stream()
                            .map(Object[].class::cast)
                            .map(SesionMateriaEstudianteJpaRepository::toProjection)
                            .toList();
                    final long total = filas.isEmpty() ? 0 : NativeRowValues.entero(((Object[]) filas.getFirst())[COLUMNA_TOTAL]);
                    return new PaginaResultado<>(sesiones, numeroPagina, tamanoPagina, total);
                },
                DbCodeTranslator::traducir
        );
    }

    private static SesionMateriaEstudianteProjection toProjection(final Object[] f) {
        if (f.length != COLUMNAS_SESIONES_MATERIA) {
            throw new IllegalStateException("Las sesiones de la materia devolvieron " + f.length + " columnas; se esperaban "
                    + COLUMNAS_SESIONES_MATERIA + ".");
        }
        return new SesionMateriaEstudianteProjection(NativeRowValues.uuid(f[0]), NativeRowValues.texto(f[1]),
                entero(f[2]), NativeRowValues.texto(f[3]), entero(f[4]), NativeRowValues.uuid(f[5]),
                f[6] == null ? null : f[6].toString().strip(), NativeRowValues.texto(f[7]), utc(f[8]), utc(f[9]), utc(f[10]),
                NativeRowValues.bit(f[11]));
    }

    private static Integer entero(final Object valor) {
        return valor == null ? null : NativeRowValues.entero(valor);
    }

    private static LocalDateTime utc(final Object valor) {
        final Instant instante = NativeRowValues.instanteUtc(valor);
        return instante == null ? null : LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
