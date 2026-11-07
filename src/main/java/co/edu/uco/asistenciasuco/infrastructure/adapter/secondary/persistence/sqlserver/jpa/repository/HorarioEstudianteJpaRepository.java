package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioEstudianteProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Horario del estudiante autenticado (LB-008 JPA-05) sobre {@code usp_consultar_horario_estudiante} (CULM-001 A.2-11): las
 * columnas de {@code uv_horario_estudiante}, solo de matriculas inscritas (la vista no filtra por estado; el filtro va en
 * el SP). Horas academicas LOCALES: no aplica el mapper UTC de Sesion.
 */
@Repository
public class HorarioEstudianteJpaRepository implements HorarioEstudianteQueryPort {

    static final String SQL_HORARIO = """
            EXEC dbo.usp_consultar_horario_estudiante
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;
    /** id, idEstudiante, idGrupo, codigoMateria, nombreMateria, grupo, dia, horaInicio, horaFin, docente. */
    static final int COLUMNAS_HORARIO = 10;

    private final EntityManager entityManager;

    public HorarioEstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de horario estudiante es obligatorio.");
    }

    @Override
    public List<HorarioEstudianteProjection> consultarHorarioEstudiante(final UUID usuarioEjecutor) {
        if (usuarioEjecutor == null) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta del horario es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarHorarioEstudiante",
                "No fue posible consultar el horario del estudiante desde base de datos.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_HORARIO)
                        .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                        .setParameter("idCorrelacion", correlationId)
                        .getResultList()).stream()
                        .map(Object[].class::cast)
                        .map(HorarioEstudianteJpaRepository::toProjection)
                        .toList(),
                DbCodeTranslator::traducir
        );
    }

    private static HorarioEstudianteProjection toProjection(final Object[] f) {
        if (f.length != COLUMNAS_HORARIO) {
            throw new IllegalStateException("El horario del estudiante devolvio " + f.length + " columnas; se esperaban "
                    + COLUMNAS_HORARIO + ".");
        }
        return new HorarioEstudianteProjection(NativeRowValues.uuid(f[0]), NativeRowValues.uuid(f[1]), NativeRowValues.uuid(f[2]),
                texto(f[3]), texto(f[4]), texto(f[5]), texto(f[6]), hora(f[7]), hora(f[8]), texto(f[9]));
    }

    static LocalTime hora(final Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof LocalTime hora) {
            return hora;
        }
        return LocalTime.parse(valor.toString().strip());
    }

    private static String texto(final Object valor) {
        return valor == null ? null : valor.toString().strip();
    }
}
