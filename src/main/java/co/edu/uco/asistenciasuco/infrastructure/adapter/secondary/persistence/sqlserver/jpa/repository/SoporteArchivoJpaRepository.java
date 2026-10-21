package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpSoporteArchivoAutorizadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Metadata durable del soporte de revision (LB-004D, ONE_TABLE) sobre SQL Server: registro del borrador
 * ({@code usp_registrar_archivo_soporte}) y lectura autorizada ({@code usp_consultar_archivo_soporte_autorizado}).
 *
 * <p>El propietario es SIEMPRE el usuario autenticado (jamas un dato del cliente). Los instantes
 * {@code DATETIME2(7)} son UTC por contrato de columna y se leen como {@link LocalDateTime} sin zona de la JVM.</p>
 */
@Repository
public class SoporteArchivoJpaRepository implements SoporteArchivoMetadataPort {

    static final String OP_REGISTRAR_SOPORTE = "registrarArchivoSoporte";
    static final String OP_CONSULTAR_SOPORTE = "consultarArchivoSoporteAutorizado";

    static final String SQL_REGISTRAR_SOPORTE = """
            EXEC dbo.usp_registrar_archivo_soporte
                 @idArchivoSoporte = :idArchivoSoporte,
                 @nombreOriginal = :nombreOriginal,
                 @tipoContenido = :tipoContenido,
                 @tamanioBytes = :tamanioBytes,
                 @checksumSha256 = :checksumSha256,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_CONSULTAR_SOPORTE = """
            EXEC dbo.usp_consultar_archivo_soporte_autorizado
                 @idArchivoSoporte = :idArchivoSoporte,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public SoporteArchivoJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    @Override
    public void registrarBorrador(final RegistrarSoporteBorrador comando) {
        Objects.requireNonNull(comando, "El comando para registrar el soporte es obligatorio.");
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idArchivoSoporte", comando.fileId());
        parametros.put("nombreOriginal", comando.nombreOriginal());
        parametros.put("tipoContenido", comando.tipoContenido());
        parametros.put("tamanioBytes", comando.tamanioBytes());
        parametros.put("checksumSha256", comando.checksumSha256());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", comando.usuarioPropietario());

        try {
            procedureExecutor.execute(OP_REGISTRAR_SOPORTE, SQL_REGISTRAR_SOPORTE, parametros, correlationId);
        } catch (final ApplicationException rechazo) {
            // La DB respondio con un rechazo de negocio (ARC_00x/SEC_001...): no se escribio nada.
            throw new SoporteMetadataRechazadaException("La DB rechazo el registro del soporte.", rechazo);
        } catch (final DatabaseOperationException tecnico) {
            throw clasificarFalloTecnico(tecnico);
        }
    }

    @Override
    public Optional<SoporteArchivoAutorizado> consultarAutorizado(final UUID fileId, final UUID usuarioEjecutor) {
        Objects.requireNonNull(fileId, "El fileId es obligatorio.");
        Objects.requireNonNull(usuarioEjecutor, "El usuario ejecutor es obligatorio.");
        final UUID correlationId = CorrelationIdContext.require();

        try {
            return JpaQueryExecutor.execute(
                    OP_CONSULTAR_SOPORTE,
                    "No fue posible consultar el soporte desde base de datos.",
                    () -> entityManager.createNativeQuery(SQL_CONSULTAR_SOPORTE, SpSoporteArchivoAutorizadoEntity.class)
                            .setParameter("idArchivoSoporte", fileId)
                            .setParameter("idCorrelacion", correlationId)
                            .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                            .getResultList().stream()
                            .findFirst()
                            .map(fila -> toAutorizado((SpSoporteArchivoAutorizadoEntity) fila))
            );
        } catch (final DatabaseOperationException tecnico) {
            throw new SoporteMetadataNoDisponibleException("No fue posible consultar el soporte.", tecnico);
        }
    }

    private static RuntimeException clasificarFalloTecnico(final DatabaseOperationException tecnico) {
        // Transporte/timeout/contrato roto: el resultado en SQL es desconocido. Un SYS_001 del propio SP
        // (clasificado ERR_DB_UNCLASSIFIED) ocurre tras su ROLLBACK, asi que es un rechazo definitivo.
        final boolean definitivo = tecnico.getErrorDefinition()
                .map(codigo -> DatabaseErrorCode.ERR_DB_UNCLASSIFIED.equals(codigo)
                        || DatabaseErrorCode.ERR_DB_CATALOG_INCONSISTENT.equals(codigo))
                .orElse(false);
        return definitivo
                ? new SoporteMetadataRechazadaException("La DB no registro el soporte.", tecnico)
                : new SoporteMetadataNoDisponibleException("No se pudo confirmar el registro del soporte.", tecnico);
    }

    private static SoporteArchivoAutorizado toAutorizado(final SpSoporteArchivoAutorizadoEntity fila) {
        return new SoporteArchivoAutorizado(
                fila.idArchivoSoporte(),
                EstadoSoporte.valueOf(fila.estado()),
                fila.nombreOriginal(),
                fila.tipoContenido(),
                fila.tamanioBytes(),
                fila.checksumSha256(),
                toUtcInstant(fila.fechaRegistroUtc()),
                toUtcInstant(fila.fechaAdjuncionUtc()),
                fila.idSolicitudRevisionAsistencia(),
                RelacionAcceso.valueOf(fila.relacionAcceso())
        );
    }

    /** DATETIME2(7) UTC sin zona -> Instant, sin pasar por la zona por defecto de la JVM. */
    static Instant toUtcInstant(final LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }
}
