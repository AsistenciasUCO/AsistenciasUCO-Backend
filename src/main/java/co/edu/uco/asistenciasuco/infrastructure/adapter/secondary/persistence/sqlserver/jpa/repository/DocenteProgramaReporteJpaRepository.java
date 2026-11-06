package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.HistorialGruposDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ResumenDocentesProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.GrupoHistorialDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HistorialGruposDocentePagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenTipoVinculacionProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Adaptador JPA de los reportes del directorio docente del coordinador: historial de grupos (HU094) y consolidado (HU095). */
@Repository
public class DocenteProgramaReporteJpaRepository implements HistorialGruposDocenteQueryPort, ResumenDocentesProgramaQueryPort {

    static final String SQL_HISTORIAL = """
            EXEC dbo.usp_consultar_historial_grupos_docente_programa
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idDocente = :idDocente,
                 @idPeriodoAcademico = :idPeriodoAcademico,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;

    static final String SQL_RESUMEN = """
            EXEC dbo.usp_consultar_resumen_docentes_programa
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String OP_HISTORIAL = "consultarHistorialGruposDocente";
    static final String OP_RESUMEN = "consultarResumenDocentesPrograma";

    private final EntityManager entityManager;

    public DocenteProgramaReporteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public HistorialGruposDocentePagina consultarHistorial(
            final UUID usuarioEjecutor, final UUID idDocente, final UUID idPeriodoAcademico, final int pagina, final int tamano
    ) {
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta del historial del docente es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_HISTORIAL,
                "No fue posible consultar el historial de grupos del docente desde base de datos.",
                () -> {
                    final List<Object[]> filas = historial(usuarioEjecutor, correlationId, idDocente, idPeriodoAcademico, pagina, tamano);
                    if (filas.isEmpty()) {
                        return new HistorialGruposDocentePagina(List.of(), 0, 0, 0, 0, 0);
                    }
                    final Object[] primera = filas.getFirst();
                    return new HistorialGruposDocentePagina(
                            filas.stream().map(DocenteProgramaReporteJpaRepository::toProjection).toList(),
                            valor(primera[20]), valor(primera[16]), valor(primera[17]), valor(primera[18]), valor(primera[19]));
                }
        );
    }

    /**
     * Certificación Fase 3 (HU094): el SP rechaza con GEN_001 un docente que no está en el programa del coordinador.
     * Antes llegaba como 500; {@link DbCodeTranslator} lo traduce a 404, como las demás consultas por SP.
     */
    private List<Object[]> historial(final UUID usuarioEjecutor, final UUID correlationId, final UUID idDocente,
                                     final UUID idPeriodoAcademico, final int pagina, final int tamano) {
        try {
            return ((List<?>) entityManager.createNativeQuery(SQL_HISTORIAL)
                            .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                            .setParameter("idCorrelacion", correlationId)
                            .setParameter("idDocente", idDocente)
                            .setParameter("idPeriodoAcademico", idPeriodoAcademico)
                            .setParameter("numeroPagina", pagina)
                            .setParameter("tamanoPagina", tamano)
                            .getResultList()).stream()
                            .map(Object[].class::cast)
                            .toList();
        } catch (final PersistenceException exception) {
            throw DbCodeTranslator.traducir(exception, "docente");
        }
    }

    @Override
    public List<ResumenTipoVinculacionProjection> consultarResumen(final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta del consolidado docente es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_RESUMEN,
                "No fue posible consultar el consolidado de docentes del programa desde base de datos.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_RESUMEN)
                        .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                        .setParameter("idCorrelacion", correlationId)
                        .getResultList()).stream()
                        .map(Object[].class::cast)
                        .map(f -> new ResumenTipoVinculacionProjection(NativeRowValues.uuid(f[0]), texto(f[1]), texto(f[2]), valor(f[3]), valor(f[4])))
                        .toList()
        );
    }

    private static GrupoHistorialDocenteProjection toProjection(final Object[] f) {
        return new GrupoHistorialDocenteProjection(
                NativeRowValues.uuid(f[0]), entero(f[1]), texto(f[2]), NativeRowValues.uuid(f[3]), texto(f[4]), NativeRowValues.uuid(f[5]), entero(f[6]), texto(f[7]),
                fecha(f[8]), fecha(f[9]), texto(f[10]), valor(f[11]), valor(f[12]), valor(f[13]), valor(f[14]), valor(f[15]));
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
