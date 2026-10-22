package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.docente.exception.DocenteErrorCode;
import co.edu.uco.asistenciasuco.application.features.estudiante.exception.EstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.exception.TipoIdentificacionErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Traduce el contrato tecnico de SQL Server a excepciones semanticas de aplicacion.
 */
public final class DbExceptionTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbExceptionTranslator.class);
    private static final Set<ErrorDefinition> FORBIDDEN_CODES = Set.of(
            AsistenciaErrorCode.ERR_ESTUDIANTE_NO_PERTENECE_SESION,
            SecurityErrorCode.FORBIDDEN
    );
    private static final Set<ErrorDefinition> NOT_FOUND_CODES = Set.of(
            UsuarioErrorCode.ERR_USUARIO_NO_EXISTE,
            EstudianteErrorCode.ERR_ESTUDIANTE_NO_EXISTE,
            DocenteErrorCode.ERR_DOCENTE_NO_EXISTE,
            GrupoErrorCode.ERR_GRUPO_NO_EXISTE,
            TipoIdentificacionErrorCode.ERR_TIPO_IDENTIFICACION_NO_EXISTE,
            SesionErrorCode.ERR_SESION_NO_EXISTE,
            CommonErrorCode.RESOURCE_NOT_FOUND
    );
    private static final Set<ErrorDefinition> CONFLICT_CODES = Set.of(
            UsuarioErrorCode.ERR_UNICIDAD_CORREO,
            UsuarioErrorCode.ERR_UNICIDAD_DOCUMENTO,
            UsuarioErrorCode.ERR_IDENTIDAD_USUARIO_CONFLICTO,
            GrupoErrorCode.ERR_CUPO_SUPERADO,
            GrupoErrorCode.ERR_CRUCE_HORARIO_ESTUDIANTE,
            GrupoErrorCode.ERR_CRUCE_HORARIO_DOCENTE,
            GrupoErrorCode.ERR_MATRICULA_DUPLICADA,
            GrupoErrorCode.ERR_GRUPO_NO_HABILITADO,
            UsuarioErrorCode.ERR_USUARIO_INACTIVO,
            EstudianteErrorCode.ERR_ESTUDIANTE_INACTIVO,
            DocenteErrorCode.ERR_DOCENTE_INACTIVO,
            DocenteErrorCode.ERR_DOCENTE_YA_REGISTRADO,
            CommonErrorCode.CONFLICT
    );
    private static final Set<ErrorDefinition> VALIDATION_CODES = Set.of(
            UsuarioErrorCode.ERR_NOMBRE_PERSONA_INVALIDO,
            UsuarioErrorCode.ERR_PASSWORD_POLITICA_INVALIDA,
            AsistenciaErrorCode.ERR_ESTADO_ASISTENCIA_INVALIDO,
            CommonErrorCode.VALIDATION_ERROR
    );

    private static final Map<Set<ErrorDefinition>, ExceptionFactory> EXCEPTION_BY_CODES = Map.of(
            FORBIDDEN_CODES, ForbiddenException::new,
            NOT_FOUND_CODES, ResourceNotFoundException::new,
            CONFLICT_CODES, ConflictException::new,
            VALIDATION_CODES, ValidationException::new
    );

    private DbExceptionTranslator() {
    }

    public static void throwIfFailed(
            final boolean successful,
            final String userMessage,
            final String technicalMessage,
            final String correlationId,
            final String operation
    ) {
        if (successful) {
            return;
        }

        final ErrorDefinition code = DbFailureClassifier.classify(userMessage, technicalMessage, operation);
        logFailedResult(code, userMessage, technicalMessage, correlationId, operation);

        if (DatabaseErrorCode.FEATURE_UNAVAILABLE.equals(code)) {
            throw new FeatureUnavailableException(code.defaultMessage());
        }

        for (final Map.Entry<Set<ErrorDefinition>, ExceptionFactory> entry : EXCEPTION_BY_CODES.entrySet()) {
            if (entry.getKey().contains(code)) {
                if (entry.getKey() == CONFLICT_CODES && !TextHelper.isNullOrBlank(userMessage) && !userMessage.equals(CommonErrorCode.CONFLICT.defaultMessage())) {
                    throw new ConflictException(userMessage);
                }
                throw entry.getValue().create(code);
            }
        }

        throw new DatabaseOperationException(code);
    }

    private static void logFailedResult(
            final ErrorDefinition internalCode,
            final String userMessage,
            final String technicalMessage,
            final String correlationId,
            final String operation
    ) {
        final String normalizedOperation = TextHelper.isNullOrBlank(operation) ? "unknown" : operation;
        if (DatabaseErrorCode.ERR_DB_UNCLASSIFIED.equals(internalCode)) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}, dbCode={}, userMessage={}, technicalMessage={}",
                    normalizedOperation,
                    SensitiveDataSanitizer.sanitizeForLog(correlationId, 80),
                    internalCode.code(),
                    SensitiveDataSanitizer.sanitizeForLog(userMessage),
                    SensitiveDataSanitizer.sanitizeForLog(technicalMessage)
            );
            return;
        }
        LOGGER.warn(
                "SQL operation rejected. operation={}, correlationId={}, dbCode={}, userMessage={}, technicalMessage={}",
                normalizedOperation,
                SensitiveDataSanitizer.sanitizeForLog(correlationId, 80),
                internalCode.code(),
                SensitiveDataSanitizer.sanitizeForLog(userMessage),
                SensitiveDataSanitizer.sanitizeForLog(technicalMessage)
        );
    }

    @FunctionalInterface
    private interface ExceptionFactory {

        ApplicationException create(ErrorDefinition code);
    }
}
