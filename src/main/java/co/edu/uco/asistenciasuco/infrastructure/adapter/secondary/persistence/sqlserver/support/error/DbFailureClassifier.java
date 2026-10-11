package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.docente.exception.DocenteErrorCode;
import co.edu.uco.asistenciasuco.application.features.estudiante.exception.EstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.exception.TipoIdentificacionErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Clasifica mensajes devueltos por SQL Server a codigos internos estables.
 */
final class DbFailureClassifier {

    private static final String OPERATION_ASIGNAR_DOCENTE = "asignarDocenteAGrupo";
    private static final String OPERATION_REGISTRAR_ESTUDIANTE = "registrarEstudianteEnGrupo";
    // Operaciones reales de los repositories JPA (OP_* en UsuarioJpaRepository / AsistenciaJpaRepository).
    private static final String OPERATION_CREAR_USUARIO = "crearUsuario";
    private static final String OPERATION_SINCRONIZAR_USUARIO = "sincronizarUsuario";
    private static final String OPERATION_REGISTRAR_ASISTENCIA_AUTONOMA = "registrarAsistenciaAutonoma";
    private static final String OPERATION_RESOLVER_SOLICITUD_REVISION = "resolverSolicitudRevisionAsistencia";

    private DbFailureClassifier() {
    }

    static ErrorDefinition classify(final String userMessage, final String technicalMessage, final String operation) {
        final var formalError = DbTechnicalError.parse(technicalMessage);
        if (formalError.isPresent()) {
            return classifyDbCode(formalError.get().codigo(), operation);
        }
        if (DbTechnicalError.hasDbCodeMarker(technicalMessage)) {
            return DatabaseErrorCode.ERR_DB_UNCLASSIFIED;
        }
        return classifyLegacy(userMessage, technicalMessage, operation);
    }

    private static ErrorDefinition classifyDbCode(final String code, final String operation) {
        return switch (code) {
            case "SEC_001", "SEC_002", "EST_004" -> SecurityErrorCode.FORBIDDEN;
            case "ATT_001", "ATT_002", "ATT_003", "GEN_002", "RC_001", "SES_004", "PLA_001",
                 "VAL_001", "VAL_002", "VAL_003", "VAL_004", "VAL_005" -> CommonErrorCode.VALIDATION_ERROR;
            case "VAL_006" -> CommonErrorCode.CONFLICT;
            // GEN_003: "Ya existe un {} registrado con este {}"; lo emite usp_registrar_o_actualizar_plan_estudio
            // ante un plan duplicado (programa + inp). Conflicto de negocio, no fallo tecnico.
            case "GEN_003" -> CommonErrorCode.CONFLICT;
            case "VAL_007" -> classifyVal007(operation);
            case "ERR_UNICIDAD_DOCUMENTO" -> UsuarioErrorCode.ERR_UNICIDAD_DOCUMENTO;
            case "USU_002" -> UsuarioErrorCode.ERR_USUARIO_INACTIVO;
            case "HOR_001" -> GrupoErrorCode.ERR_CRUCE_HORARIO_ESTUDIANTE;
            case "SES_001", "PROG_001", "PER_001" -> CommonErrorCode.RESOURCE_NOT_FOUND;
            // USU_001: validacion de usuario inexistente por id, emitida por los SP publicos que la exigen.
            case "USU_001" -> UsuarioErrorCode.ERR_USUARIO_NO_EXISTE;
            // IDN_001: usp_registrar_estudiante_en_grupo (conflicto de identidad correo/documento).
            case "IDN_001" -> UsuarioErrorCode.ERR_IDENTIDAD_USUARIO_CONFLICTO;
            // GEN_001: codigo GENERICO de existencia (tipo de identificacion, facultad, perfil, periodo,
            // grupo, parametros...). No se puede inferir la entidad: queda tecnico/no clasificado
            // hasta que la DB publique codigos especificos. Cae por el default de este switch.
            // EST_001: usp_registrar_estudiante_en_grupo (estudiante no resoluble tras sincronizar usuario).
            case "EST_001" -> EstudianteErrorCode.ERR_ESTUDIANTE_NO_EXISTE;
            case "ERR_GRUPO_NO_EXISTE" -> GrupoErrorCode.ERR_GRUPO_NO_EXISTE;
            case "ERR_CUPO_SUPERADO" -> GrupoErrorCode.ERR_CUPO_SUPERADO;
            case "ERR_MATRICULA_DUPLICADA" -> GrupoErrorCode.ERR_MATRICULA_DUPLICADA;
            case "ERR_GRUPO_NO_HABILITADO" -> GrupoErrorCode.ERR_GRUPO_NO_HABILITADO;
            // No puede reducirse la capacidad del grupo por debajo de su ocupacion actual.
            case "ERR_CUPO_INFERIOR_OCUPACION" -> CommonErrorCode.CONFLICT;
            // Combinacion Programa/Facultad invalida enviada a la operacion.
            case "ERR_PROGRAMA_FACULTAD_INCONSISTENTE" -> CommonErrorCode.VALIDATION_ERROR;
            case "SES_003" -> DatabaseErrorCode.FEATURE_UNAVAILABLE;
            // CAT_001: inconsistencia interna de catalogo DB (p. ej. dbo.Estado sin un codigo esperado).
            // Fallo tecnico conocido, no se reclasifica como 4xx.
            case "CAT_001" -> DatabaseErrorCode.ERR_DB_CATALOG_INCONSISTENT;
            // SYS_001: catch-all tecnico de los SP publicos. Caso explicito para que quede clasificado
            // (no caido por el default) aunque su tratamiento sea el mismo que lo no clasificado.
            case "SYS_001" -> DatabaseErrorCode.ERR_DB_UNCLASSIFIED;
            default -> DatabaseErrorCode.ERR_DB_UNCLASSIFIED;
        };
    }

    /**
     * VAL_007 no tiene semantica unica en la DB: depende del SP publico que lo emite.
     */
    private static ErrorDefinition classifyVal007(final String operation) {
        if (OPERATION_CREAR_USUARIO.equals(operation) || OPERATION_SINCRONIZAR_USUARIO.equals(operation)) {
            return UsuarioErrorCode.ERR_PASSWORD_POLITICA_INVALIDA;
        }
        if (OPERATION_REGISTRAR_ASISTENCIA_AUTONOMA.equals(operation)) {
            return CommonErrorCode.VALIDATION_ERROR;
        }
        if (OPERATION_RESOLVER_SOLICITUD_REVISION.equals(operation)) {
            return SecurityErrorCode.FORBIDDEN;
        }
        return DatabaseErrorCode.ERR_DB_UNCLASSIFIED;
    }

    private static ErrorDefinition classifyLegacy(
            final String userMessage,
            final String technicalMessage,
            final String operation
    ) {
        final String message = normalize(userMessage + " " + technicalMessage);
        final String normalizedOperation = normalize(operation);

        if (TextHelper.isNullOrBlank(message)) {
            return DatabaseErrorCode.ERR_DB_UNCLASSIFIED;
        }
        // RC_001: codigo de RazonCausa inexistente en dbo.RazonCausa (catalogo de estados de
        // asistencia). El backend ya valida AN/SJC/EX en Application antes de llamar al SP; esta
        // rama es defensa en profundidad para cualquier codigo que la DB rechace igualmente.
        if (contains(message, "estado de asistencia") && contains(message, "razon de causa")) {
            return AsistenciaErrorCode.ERR_ESTADO_ASISTENCIA_INVALIDO;
        }
        if (contains(message, "razoncausa.codigo")) {
            return AsistenciaErrorCode.ERR_ESTADO_ASISTENCIA_INVALIDO;
        }
        if (containsAny(message, "nombre", "nombres", "apellido", "apellidos")
                && containsAny(message, "caracteres no permitidos", "formato invalido")) {
            return UsuarioErrorCode.ERR_NOMBRE_PERSONA_INVALIDO;
        }
        if (contains(message, "usuario") && contains(message, "registrado como docente")) {
            return DocenteErrorCode.ERR_DOCENTE_YA_REGISTRADO;
        }
        if (contains(message, "estudiante") && contains(message, "inactivo")) {
            return EstudianteErrorCode.ERR_ESTUDIANTE_INACTIVO;
        }
        if (contains(message, "docente") && contains(message, "inactivo")) {
            return DocenteErrorCode.ERR_DOCENTE_INACTIVO;
        }
        if (contains(message, "usuario") && contains(message, "inactivo")) {
            return UsuarioErrorCode.ERR_USUARIO_INACTIVO;
        }
        if (contains(message, "matricula") && containsAny(message, "duplicad", "ya existe", "ya se encuentra", "registrad", "inscrit")) {
            return GrupoErrorCode.ERR_MATRICULA_DUPLICADA;
        }
        if (containsAny(message, "tamaoo moximo", "tamano maximo")) {
            return GrupoErrorCode.ERR_CUPO_SUPERADO;
        }
        if (containsAny(message, "cupo", "cupos")
                && containsAny(message, "superad", "sin", "no hay", "agotad", "maxim", "moximo", "capacidad", "lleno", "llena")) {
            return GrupoErrorCode.ERR_CUPO_SUPERADO;
        }
        if (isScheduleConflict(message)) {
            if (contains(message, "estudiante") || contains(normalizedOperation, OPERATION_REGISTRAR_ESTUDIANTE.toLowerCase(Locale.ROOT))) {
                return GrupoErrorCode.ERR_CRUCE_HORARIO_ESTUDIANTE;
            }
            if (contains(message, "docente") || contains(normalizedOperation, OPERATION_ASIGNAR_DOCENTE.toLowerCase(Locale.ROOT))) {
                return GrupoErrorCode.ERR_CRUCE_HORARIO_DOCENTE;
            }
        }
        if (contains(message, "conflicto de identidad")
                || contains(message, "no corresponden de forma univoca al mismo usuario")) {
            return UsuarioErrorCode.ERR_IDENTIDAD_USUARIO_CONFLICTO;
        }
        if (containsAny(message, "correo", "email", "e-mail") && containsAny(message, "duplicad", "ya existe", "ya se encuentra", "registrad", "unico")) {
            return UsuarioErrorCode.ERR_UNICIDAD_CORREO;
        }
        if (containsAny(message, "documento", "identificacion", "numero de identificacion")
                && containsAny(message, "duplicad", "ya existe", "ya se encuentra", "registrad", "unico")) {
            return UsuarioErrorCode.ERR_UNICIDAD_DOCUMENTO;
        }
        if (contains(message, "grupo") && containsAny(
                message,
                "no habilitado",
                "deshabilitado",
                "inhabilitado",
                "no esta habilitado",
                "no se encuentra habilitado"
        )) {
            return GrupoErrorCode.ERR_GRUPO_NO_HABILITADO;
        }
        if (containsAny(
                message,
                "tipo identificacion",
                "tipo de identificacion",
                "tipo identificaci?n",
                "tipo de identificaci?n",
                "tipoididentificacion",
                "uv_tipo_identificacion"
        )
                && containsAny(message, "no existe", "no encontrado", "no encontrada", "inexistente")) {
            return TipoIdentificacionErrorCode.ERR_TIPO_IDENTIFICACION_NO_EXISTE;
        }
        if (doesNotExist(message, "grupo")) {
            return GrupoErrorCode.ERR_GRUPO_NO_EXISTE;
        }
        if (doesNotExist(message, "estudiante")) {
            return EstudianteErrorCode.ERR_ESTUDIANTE_NO_EXISTE;
        }
        if (doesNotExist(message, "usuario")) {
            return UsuarioErrorCode.ERR_USUARIO_NO_EXISTE;
        }
        if (doesNotExist(message, "docente")) {
            return DocenteErrorCode.ERR_DOCENTE_NO_EXISTE;
        }
        if (doesNotExist(message, "sesion")) {
            return SesionErrorCode.ERR_SESION_NO_EXISTE;
        }
        if (contains(message, "estudiante") && contains(message, "sesion")
                && containsAny(message, "no pertenece", "no esta asociado", "no se encuentra asociado")) {
            return AsistenciaErrorCode.ERR_ESTUDIANTE_NO_PERTENECE_SESION;
        }

        return DatabaseErrorCode.ERR_DB_UNCLASSIFIED;
    }

    private static boolean doesNotExist(final String message, final String resource) {
        return contains(message, resource)
                && containsAny(message, "no existe", "no encontrado", "no encontrada", "inexistente");
    }

    private static boolean isScheduleConflict(final String message) {
        return containsAny(message, "cruce", "conflicto", "solap")
                && containsAny(message, "horario", "hora", "sesion");
    }

    private static boolean containsAny(final String value, final String... expectedValues) {
        for (final String expectedValue : expectedValues) {
            if (contains(value, expectedValue)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(final String value, final String expectedValue) {
        return value.contains(normalize(expectedValue));
    }

    private static String normalize(final String value) {
        if (TextHelper.isNullOrBlank(value)) {
            return "";
        }
        final String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim();
    }
}
