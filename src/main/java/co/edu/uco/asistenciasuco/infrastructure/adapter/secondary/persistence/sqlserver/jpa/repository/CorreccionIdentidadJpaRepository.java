package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * HU159: {@code usp_corregir_identidad_usuario} (comando canonico + idUsuario, correo y numeroIdentificacion). El SP
 * fuerza la jerarquia (coordinador del programa del estudiante, administrador del decano), el motivo y la auditoria.
 */
@Repository
public class CorreccionIdentidadJpaRepository implements CorreccionIdentidadCommandPort {

    static final String SQL_CORREGIR_IDENTIDAD = """
            EXEC dbo.usp_corregir_identidad_usuario
                 @tipoUsuario = :tipoUsuario,
                 @idRegistro = :idRegistro,
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @motivo = :motivo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String OP_CORREGIR_IDENTIDAD = "corregirIdentidadUsuario";
    private static final int COLUMNAS_EXTRA = 3;

    private final JpaProcedureExecutor procedureExecutor;

    public CorreccionIdentidadJpaRepository(final JpaProcedureExecutor procedureExecutor) {
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    @Override
    public ResultadoCorreccionIdentidad corregir(final TipoUsuarioCorregible tipo, final UUID idRegistro,
                                                 final NombresCorregidos nombres, final CorreccionDocumentoDTO documento,
                                                 final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("tipoUsuario", tipo.name());
        parametros.put("idRegistro", idRegistro);
        parametros.put("primerNombre", nombres.primerNombre());
        parametros.put("segundoNombre", nombres.segundoNombre());
        parametros.put("primerApellido", nombres.primerApellido());
        parametros.put("segundoApellido", nombres.segundoApellido());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);
        CorreccionDocumentoParametros.agregar(parametros, documento);

        final ProcedureRow fila = procedureExecutor.executeWithExtras(
                OP_CORREGIR_IDENTIDAD, SQL_CORREGIR_IDENTIDAD, parametros, correlationId, COLUMNAS_EXTRA);
        final List<Object> extras = fila.extras();
        final UUID idUsuario = NativeRowValues.uuid(extras.get(0));
        if (idUsuario == null) {
            throw new DatabaseOperationException(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "El procedimiento " + OP_CORREGIR_IDENTIDAD + " no retorno el usuario corregido.");
        }
        final Object numero = extras.get(2);
        return new ResultadoCorreccionIdentidad(idUsuario, extras.get(1) == null ? null : String.valueOf(extras.get(1)),
                numero instanceof Number n ? Integer.valueOf(n.intValue()) : null);
    }
}
