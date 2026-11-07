package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 B.1 (HU047): cancelacion de sesiones via {@code usp_cancelar_sesion} y destinatarios del aviso via
 * {@code usp_consultar_destinatarios_sesion_cancelada} (JPA, {@code EXEC} nativo). El SP toma
 * {@code Sesion WITH (UPDLOCK, HOLDLOCK)}: la cancelacion y el registro de asistencia se serializan en la DB.
 */
@Repository
public class SesionCancelacionJpaRepository implements SesionCancelacionRepositoryPort {

    static final String OP_CANCELAR = "cancelarSesion";
    static final String SQL_CANCELAR = """
            EXEC dbo.usp_cancelar_sesion
                 @idSesion = :idSesion, @motivo = :motivo,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_DESTINATARIOS = """
            EXEC dbo.usp_consultar_destinatarios_sesion_cancelada
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion, @idSesion = :idSesion
            """;

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public SesionCancelacionJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de cancelacion es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public SesionCancelada cancelar(final UUID sesionId, final String motivo, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSesion", sesionId);
        parametros.put("motivo", motivo);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(OP_CANCELAR, SQL_CANCELAR, parametros, correlationId, 4);
        final List<Object> c = fila.extras();
        return new SesionCancelada(NativeRowValues.uuid(c.get(0)), NativeRowValues.uuid(c.get(1)), fechaHora(c.get(2)),
                c.get(3) == null ? 0 : ((Number) c.get(3)).intValue());
    }

    @Override
    public List<DestinatarioSesionCancelada> consultarDestinatarios(final UUID sesionId, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarDestinatariosSesionCancelada",
                "No fue posible consultar los destinatarios del aviso de la sesion cancelada.",
                () -> {
                    final List<?> filas;
                    try {
                        filas = entityManager.createNativeQuery(SQL_DESTINATARIOS)
                                .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                                .setParameter("idCorrelacion", correlationId)
                                .setParameter("idSesion", sesionId)
                                .getResultList();
                    } catch (final PersistenceException exception) {
                        throw DbCodeTranslator.traducir(exception, "sesion");
                    }
                    final List<DestinatarioSesionCancelada> destinatarios = new ArrayList<>(filas.size());
                    for (final Object fila : filas) {
                        final Object[] c = (Object[]) fila;
                        destinatarios.add(new DestinatarioSesionCancelada(
                                NativeRowValues.uuid(c[0]), NativeRowValues.uuid(c[1]), texto(c[2]), texto(c[3]),
                                texto(c[4]), texto(c[5]), texto(c[6]), fechaHora(c[7]), fechaHora(c[8]), texto(c[9])));
                    }
                    return destinatarios;
                });
    }

    private static String texto(final Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }

    /** DATETIME2 en UTC sin zona: se conserva el valor almacenado tal cual. */
    static LocalDateTime fechaHora(final Object valor) {
        return switch (valor) {
            case null -> null;
            case LocalDateTime local -> local;
            case java.util.Date fecha -> LocalDateTime.ofInstant(Instant.ofEpochMilli(fecha.getTime()), ZoneId.systemDefault());
            default -> LocalDateTime.parse(String.valueOf(valor).replace(' ', 'T'));
        };
    }
}
