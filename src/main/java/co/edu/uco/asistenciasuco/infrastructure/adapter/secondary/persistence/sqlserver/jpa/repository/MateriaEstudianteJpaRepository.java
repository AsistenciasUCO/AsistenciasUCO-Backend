package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Materias del estudiante autenticado (LB-008 JPA-05) sobre {@code usp_consultar_materias_estudiante} (CULM-001 A.2-11):
 * el SP resuelve el estudiante desde el usuario ejecutor y solo cuenta la matricula inscrita
 * ({@code MATRICULA/ESTADO_INSCRITA}); {@code I}, {@code CI}, {@code CVP} y {@code F} no aparecen.
 */
@Repository
public class MateriaEstudianteJpaRepository implements MateriaEstudianteQueryPort {

    static final String SQL_MATERIAS = """
            EXEC dbo.usp_consultar_materias_estudiante
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;
    static final int COLUMNAS_MATERIAS = 4;

    private final EntityManager entityManager;

    public MateriaEstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de materias de estudiante es obligatorio.");
    }

    @Override
    public List<MateriaEstudianteProjection> consultarMateriasEstudiante(final UUID usuarioEjecutor) {
        if (usuarioEjecutor == null) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta de materias es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarMateriasEstudiante",
                "No fue posible consultar las materias del estudiante desde base de datos.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_MATERIAS)
                        .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                        .setParameter("idCorrelacion", correlationId)
                        .getResultList()).stream()
                        .map(Object[].class::cast)
                        .map(MateriaEstudianteJpaRepository::toProjection)
                        .toList(),
                DbCodeTranslator::traducir
        );
    }

    private static MateriaEstudianteProjection toProjection(final Object[] f) {
        if (f.length != COLUMNAS_MATERIAS) {
            throw new IllegalStateException("Las materias del estudiante devolvieron " + f.length + " columnas; se esperaban "
                    + COLUMNAS_MATERIAS + ".");
        }
        return new MateriaEstudianteProjection(NativeRowValues.uuid(f[0]), texto(f[1]), NativeRowValues.uuid(f[2]), texto(f[3]));
    }

    private static String texto(final Object valor) {
        return valor == null ? null : valor.toString().strip();
    }
}
