package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdmisionEstudianteCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AdmisionEstudianteCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AdmisionEstudianteResultadoProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
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
 * Admision de estudiantes por el administrador (CULM-001 D.4, HU156) sobre {@code usp_admitir_estudiante}: comando canonico
 * de 4 columnas + {@code idUsuario, idEstudiante, esUsuarioNuevo, esEstudianteNuevo}. Los rechazos ADS_002..ADS_006 y SEC_001
 * los traduce {@code DbExceptionTranslator} (via {@link JpaProcedureExecutor}) a su HTTP. La clave que viaja es el hash.
 */
@Repository
public class AdmisionEstudianteJpaRepository implements AdmisionEstudianteCommandPort {

    static final String OP_ADMITIR = "admitirEstudiante";
    static final String SQL_ADMITIR = """
            EXEC dbo.usp_admitir_estudiante
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @password = :password,
                 @idPrograma = :idPrograma,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    private static final int COLUMNAS_EXTRA = 4;

    private final JpaProcedureExecutor procedureExecutor;

    public AdmisionEstudianteJpaRepository(final JpaProcedureExecutor procedureExecutor) {
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public AdmisionEstudianteResultadoProjection admitir(final AdmisionEstudianteCommand orden) {
        if (orden == null || orden.usuarioEjecutor() == null) {
            throw new CrosscuttingException("La orden de admision y el usuario ejecutor son obligatorios.");
        }
        final UUID correlacion = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idTipoIdIdentificacion", orden.tipoIdentificacionId());
        parametros.put("numeroIdentificacion", orden.numeroIdentificacion());
        parametros.put("primerNombre", orden.primerNombre());
        parametros.put("segundoNombre", orden.segundoNombre());
        parametros.put("primerApellido", orden.primerApellido());
        parametros.put("segundoApellido", orden.segundoApellido());
        parametros.put("correo", orden.correo());
        parametros.put("password", orden.passwordHash());
        parametros.put("idPrograma", orden.idPrograma());
        parametros.put("idCorrelacion", correlacion);
        parametros.put("idUsuarioEjecutor", orden.usuarioEjecutor());

        final ProcedureRow fila = procedureExecutor.executeWithExtras(OP_ADMITIR, SQL_ADMITIR, parametros, correlacion, COLUMNAS_EXTRA);
        final List<Object> extras = fila.extras();
        final UUID idUsuario = NativeRowValues.uuid(extras.get(0));
        final UUID idEstudiante = NativeRowValues.uuid(extras.get(1));
        if (idUsuario == null || idEstudiante == null) {
            throw new DatabaseOperationException(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "usp_admitir_estudiante confirmo la admision sin idUsuario o idEstudiante.");
        }
        return new AdmisionEstudianteResultadoProjection(idUsuario, idEstudiante, NativeRowValues.bit(extras.get(2)),
                NativeRowValues.bit(extras.get(3)));
    }
}
