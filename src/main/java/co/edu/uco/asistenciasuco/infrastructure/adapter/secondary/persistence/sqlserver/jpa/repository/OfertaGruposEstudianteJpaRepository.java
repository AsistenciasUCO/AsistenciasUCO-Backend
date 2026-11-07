package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.OfertaGruposEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.GrupoOfertaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.OfertaGruposEstudiantePagina;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Adaptador JPA de la oferta de grupos del estudiante autenticado (HU008). */
@Repository
public class OfertaGruposEstudianteJpaRepository implements OfertaGruposEstudianteQueryPort {

    static final String SQL_OFERTA = """
            EXEC dbo.usp_consultar_oferta_grupos_estudiante
                 @busqueda = :busqueda,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String OP_OFERTA = "consultarOfertaGruposEstudiante";

    /**
     * Columnas del resultset posicional de usp_consultar_oferta_grupos_estudiante (CULM-001 A.1: 19, sin las columnas de
     * solicitud de ingreso). Un desfase entre BD y backend falla de forma visible en vez de desordenar campos.
     */
    static final int COLUMNAS_OFERTA = 19;
    private static final int COLUMNA_COMPONENTE = 17;
    private static final int COLUMNA_TOTAL = 18;

    private final EntityManager entityManager;

    public OfertaGruposEstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public OfertaGruposEstudiantePagina consultarOferta(final UUID usuarioEjecutor, final String busqueda, final int pagina, final int tamano) {
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta de la oferta de grupos es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_OFERTA,
                "No fue posible consultar la oferta de grupos del estudiante desde base de datos.",
                () -> {
                    final List<Object[]> filas = ((List<?>) entityManager.createNativeQuery(SQL_OFERTA)
                            .setParameter("busqueda", busqueda)
                            .setParameter("numeroPagina", pagina)
                            .setParameter("tamanoPagina", tamano)
                            .setParameter("idCorrelacion", correlationId)
                            .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                            .getResultList()).stream()
                            .map(Object[].class::cast)
                            .map(OfertaGruposEstudianteJpaRepository::validarColumnas)
                            .toList();
                    final long total = filas.isEmpty() ? 0 : valor(filas.getFirst()[COLUMNA_TOTAL]);
                    return new OfertaGruposEstudiantePagina(
                            filas.stream().map(OfertaGruposEstudianteJpaRepository::toProjection).toList(), total);
                }
        );
    }

    private static GrupoOfertaEstudianteProjection toProjection(final Object[] f) {
        return new GrupoOfertaEstudianteProjection(
                NativeRowValues.uuid(f[0]), entero(f[1]), texto(f[2]), NativeRowValues.uuid(f[3]), texto(f[4]), texto(f[5]), texto(f[6]), NativeRowValues.uuid(f[7]),
                texto(f[8]), fecha(f[9]), fecha(f[10]), texto(f[11]), texto(f[12]), valor(f[13]), valor(f[14]), valor(f[15]),
                Boolean.TRUE.equals(f[16]), texto(f[COLUMNA_COMPONENTE]));
    }

    private static Object[] validarColumnas(final Object[] fila) {
        if (fila.length != COLUMNAS_OFERTA) {
            throw new IllegalStateException("La oferta de grupos devolvio " + fila.length + " columnas; se esperaban "
                    + COLUMNAS_OFERTA + ".");
        }
        return fila;
    }

    /**
     * Columna DATE del SP. El driver puede entregarla como LocalDate, LocalDateTime o java.util.Date (subclase de
     * java.sql, construida con la zona de la JVM): se recupera la fecha de pared sin depender de java.sql.
     */
    static LocalDate fecha(final Object valor) {
        if (valor instanceof LocalDate fecha) {
            return fecha;
        }
        if (valor instanceof LocalDateTime fecha) {
            return fecha.toLocalDate();
        }
        if (valor instanceof Date fecha) {
            return LocalDate.ofInstant(Instant.ofEpochMilli(fecha.getTime()), ZoneId.systemDefault());
        }
        return null;
    }

    private static Integer entero(final Object valor) {
        return valor == null ? null : ((Number) valor).intValue();
    }

    private static int valor(final Object valor) {
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    private static String texto(final Object valor) {
        return valor == null ? null : valor.toString().strip();
    }
}
