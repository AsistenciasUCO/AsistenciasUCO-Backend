package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.admin.exception.AdmisionEstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.admin.exception.CatalogoErrorCode;
import co.edu.uco.asistenciasuco.application.features.admin.exception.DecanoErrorCode;
import co.edu.uco.asistenciasuco.application.features.decano.exception.CoordinadorErrorCode;
import co.edu.uco.asistenciasuco.application.features.docente.exception.DocenteErrorCode;
import co.edu.uco.asistenciasuco.application.features.estudiante.exception.EstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.festivos.exception.FestivoErrorCode;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.exception.AsistenciaQrErrorCode;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.exception.TomaAsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.exception.TipoIdentificacionErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.VerificacionCorreoErrorCode;
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
import java.util.Optional;
import java.util.Set;

/**
 * Traduce el contrato tecnico de SQL Server a excepciones semanticas de aplicacion.
 */
public final class DbExceptionTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbExceptionTranslator.class);
    private static final Set<ErrorDefinition> FORBIDDEN_CODES = Set.of(
            AsistenciaErrorCode.ERR_ESTUDIANTE_NO_PERTENECE_SESION,
            SecurityErrorCode.FORBIDDEN,
            AsistenciaQrErrorCode.AQR_002,
            AsistenciaQrErrorCode.AQR_004,
            AdmisionEstudianteErrorCode.ADS_003
    );
    private static final Set<ErrorDefinition> NOT_FOUND_CODES = Set.of(
            UsuarioErrorCode.ERR_USUARIO_NO_EXISTE,
            VerificacionCorreoErrorCode.VCE_007,
            EstudianteErrorCode.ERR_ESTUDIANTE_NO_EXISTE,
            DocenteErrorCode.ERR_DOCENTE_NO_EXISTE,
            DocenteErrorCode.ERR_DOCENTE_NO_VINCULADO_PROGRAMA,
            GrupoErrorCode.ERR_GRUPO_NO_EXISTE,
            TipoIdentificacionErrorCode.ERR_TIPO_IDENTIFICACION_NO_EXISTE,
            SesionErrorCode.ERR_SESION_NO_EXISTE,
            AsistenciaErrorCode.ERR_RECLAMO_NO_EXISTE,
            CatalogoErrorCode.TPR_001,
            CatalogoErrorCode.ARE_001,
            FestivoErrorCode.FES_007,
            TomaAsistenciaErrorCode.TOM_001,
            AsistenciaQrErrorCode.AQR_001,
            AsistenciaQrErrorCode.AQR_010,
            CommonErrorCode.RESOURCE_NOT_FOUND
    );
    private static final Set<ErrorDefinition> CONFLICT_CODES = Set.of(
            UsuarioErrorCode.ERR_UNICIDAD_CORREO,
            UsuarioErrorCode.ERR_UNICIDAD_DOCUMENTO,
            UsuarioErrorCode.ERR_IDENTIDAD_USUARIO_CONFLICTO,
            VerificacionCorreoErrorCode.VCE_005,
            GrupoErrorCode.ERR_CUPO_SUPERADO,
            GrupoErrorCode.ERR_CRUCE_HORARIO_ESTUDIANTE,
            GrupoErrorCode.ERR_CRUCE_HORARIO_DOCENTE,
            GrupoErrorCode.ERR_MATRICULA_DUPLICADA,
            GrupoErrorCode.ERR_GRUPO_NO_HABILITADO,
            AsistenciaErrorCode.ERR_RECLAMO_YA_RESUELTO,
            AsistenciaErrorCode.ERR_RECLAMO_ASISTENCIA_NO_JUSTIFICABLE,
            AsistenciaErrorCode.ERR_RECLAMO_SIN_DETALLE_ASISTENCIA,
            AdmisionEstudianteErrorCode.ADS_002,
            AdmisionEstudianteErrorCode.ADS_004,
            AsistenciaErrorCode.ERR_PLAZO_JUSTIFICACION_VENCIDO,
            UsuarioErrorCode.ERR_USUARIO_INACTIVO,
            EstudianteErrorCode.ERR_ESTUDIANTE_INACTIVO,
            DocenteErrorCode.ERR_DOCENTE_INACTIVO,
            DocenteErrorCode.ERR_DOCENTE_YA_REGISTRADO,
            CoordinadorErrorCode.COO_001,
            DecanoErrorCode.DEC_002,
            CatalogoErrorCode.TPR_002,
            CatalogoErrorCode.ARE_002,
            DocenteErrorCode.ERR_PERSONA_YA_REGISTRADA,
            DocenteErrorCode.ERR_DOCENTE_YA_VINCULADO_PROGRAMA,
            DocenteErrorCode.ERR_DOCENTE_SIN_VINCULO_ACTIVO,
            AsistenciaErrorCode.ERR_SESION_NO_INICIADA,
            AsistenciaErrorCode.ERR_SESION_CERRADA,
            AsistenciaErrorCode.ERR_SESION_HORARIO_INDETERMINADO,
            // CULM-001 B.1: sesion cancelada (lote) y rechazos de la cancelacion HU047.
            AsistenciaErrorCode.ERR_SESION_CANCELADA,
            SesionErrorCode.SES_011,
            SesionErrorCode.SES_012,
            SesionErrorCode.SES_016,
            TomaAsistenciaErrorCode.TOM_003,
            TomaAsistenciaErrorCode.TOM_004,
            AsistenciaQrErrorCode.AQR_003,
            AsistenciaQrErrorCode.AQR_005,
            AsistenciaQrErrorCode.AQR_006,
            CommonErrorCode.CONFLICT
    );
    private static final Set<ErrorDefinition> VALIDATION_CODES = Set.of(
            UsuarioErrorCode.ERR_NOMBRE_PERSONA_INVALIDO,
            UsuarioErrorCode.ERR_PASSWORD_POLITICA_INVALIDA,
            VerificacionCorreoErrorCode.VCE_001,
            VerificacionCorreoErrorCode.VCE_002,
            AsistenciaErrorCode.ERR_ESTADO_ASISTENCIA_INVALIDO,
            AsistenciaErrorCode.ERR_ACCION_REVISION_INVALIDA,
            DocenteErrorCode.ERR_TIPO_VINCULACION_INVALIDO,
            DocenteErrorCode.ERR_MODO_VINCULACION_INVALIDO,
            DocenteErrorCode.ERR_BUSQUEDA_CANDIDATOS_CORTA,
            SesionErrorCode.SES_017,
            // CULM-001 B.1: motivo de cancelacion y datos de festivos.
            SesionErrorCode.SES_013,
            FestivoErrorCode.FES_001,
            FestivoErrorCode.FES_002,
            FestivoErrorCode.FES_004,
            FestivoErrorCode.FES_006,
            TomaAsistenciaErrorCode.TOM_002,
            AsistenciaQrErrorCode.AQR_007,
            AdmisionEstudianteErrorCode.ADS_005,
            AdmisionEstudianteErrorCode.ADS_006,
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

    /**
     * CULM-001 A.2-9: traduce un codigo DBCODE de un SP de consulta (lanzado con {@code THROW}) con la misma tabla de
     * {@link DbFailureClassifier} que los comandos. Vacio si el codigo no esta clasificado (sigue siendo fallo tecnico).
     */
    public static Optional<RuntimeException> translateDbCode(final String dbCode, final String operation) {
        if (TextHelper.isNullOrBlank(dbCode)) {
            return Optional.empty();
        }
        final ErrorDefinition code = DbFailureClassifier.classify(null, "DBCODE=" + dbCode.strip() + "|", operation);
        if (DatabaseErrorCode.FEATURE_UNAVAILABLE.equals(code)) {
            return Optional.of(new FeatureUnavailableException(code.defaultMessage()));
        }
        for (final Map.Entry<Set<ErrorDefinition>, ExceptionFactory> entry : EXCEPTION_BY_CODES.entrySet()) {
            if (entry.getKey().contains(code)) {
                return Optional.of(entry.getValue().create(code));
            }
        }
        return Optional.empty();
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

        // CULM-001 D.1: el mensaje de usuario de REV_020 lleva la ultima fecha local admitida (parametro del catalogo); se
        // conserva el texto institucional que armo la DB en lugar del texto fijo del codigo.
        final ErrorDefinition efectivo = conMensajeDeUsuario(code, userMessage);
        for (final Map.Entry<Set<ErrorDefinition>, ExceptionFactory> entry : EXCEPTION_BY_CODES.entrySet()) {
            if (entry.getKey().contains(code)) {
                throw entry.getValue().create(efectivo);
            }
        }

        throw new DatabaseOperationException(code);
    }

    private static ErrorDefinition conMensajeDeUsuario(final ErrorDefinition code, final String userMessage) {
        if (!AsistenciaErrorCode.ERR_PLAZO_JUSTIFICACION_VENCIDO.equals(code) || TextHelper.isNullOrBlank(userMessage)) {
            return code;
        }
        return new MensajeDeUsuarioDefinition(code, userMessage.strip());
    }

    /** Mismo codigo y tipo que {@code base}, con el mensaje institucional que resolvio la DB para esta solicitud. */
    private record MensajeDeUsuarioDefinition(ErrorDefinition base, String mensaje) implements ErrorDefinition {

        @Override
        public String code() {
            return base.code();
        }

        @Override
        public String defaultMessage() {
            return mensaje;
        }

        @Override
        public co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind kind() {
            return base.kind();
        }
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
