package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.docente.exception.DocenteErrorCode;
import co.edu.uco.asistenciasuco.application.features.estudiante.exception.EstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.admin.exception.CatalogoErrorCode;
import co.edu.uco.asistenciasuco.application.features.decano.exception.CoordinadorErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.exception.TipoIdentificacionErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.VerificacionCorreoErrorCode;
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

    private static final String OPERATION_ACTUALIZAR_COORDINADOR = "actualizarCoordinador";
    private static final String OPERATION_CAMBIAR_ESTADO_COORDINADOR = "cambiarEstadoCoordinador";
    private static final String OPERATION_CONSULTAR_HISTORIAL_GRUPOS_DOCENTE = "consultarHistorialGruposDocente";
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
            // COO_001 (HU122): el coordinador coordina un programa activo; hay que reasignarlo o desactivar el programa.
            case "COO_001" -> CoordinadorErrorCode.COO_001;
            // GEN_001 es generico y por defecto queda tecnico; en la gestion del coordinador significa que el
            // coordinador no existe o es de otra facultad (la DB no distingue los casos para no revelar su existencia).
            case "GEN_001" -> classifyGen001(operation);
            // HU076-HU078: programa academico. Duplicados y reglas 1 a 1 / grupos activos son conflictos de negocio.
            case "PROG_002", "PROG_003", "PROG_005", "PROG_007", "PROG_008" -> CommonErrorCode.CONFLICT;
            case "PROG_004", "PROG_006", "PAG_001" -> CommonErrorCode.VALIDATION_ERROR;
            // GEN_003: "Ya existe un {} registrado con este {}"; lo emite usp_registrar_o_actualizar_plan_estudio
            // ante un plan duplicado (programa + inp). Conflicto de negocio, no fallo tecnico.
            case "GEN_003" -> CommonErrorCode.CONFLICT;
            // HU081-HU083: estado y semestres del plan. PLA_002/PLA_004 no encontrado; maximo de semestres, quitar un
            // semestre que no es el ultimo o con asignaturas son conflictos de negocio.
            case "PLA_002", "PLA_004" -> CommonErrorCode.RESOURCE_NOT_FOUND;
            case "PLA_003", "PLA_005", "PLA_006" -> CommonErrorCode.CONFLICT;
            // HU096-HU098: periodos academicos. PER_001 (no encontrado) se clasifica abajo; fechas invalidas, nombre
            // duplicado o periodo con grupos son errores de negocio; PER_005 es ejecutor sin ambito para gestionarlos.
            case "PER_002" -> CommonErrorCode.VALIDATION_ERROR;
            case "PER_003", "PER_004" -> CommonErrorCode.CONFLICT;
            case "PER_005" -> SecurityErrorCode.FORBIDDEN;
            // HU051: retiro de un estudiante por el docente titular. RET_001 (grupo inexistente o ajeno) y RET_002
            // (sin matricula activa) son no encontrado; periodo terminado es conflicto; motivo invalido es validacion.
            case "RET_001", "RET_002" -> CommonErrorCode.RESOURCE_NOT_FOUND;
            case "RET_003" -> CommonErrorCode.CONFLICT;
            case "RET_004" -> CommonErrorCode.VALIDATION_ERROR;
            // IDENT-001: correo institucional y verificacion. VCE_001/VCE_002 son entradas invalidas (400, VCE_002 identico para
            // cualquier token invalido); VCE_005 ya verificado es conflicto; VCE_007 usuario inexistente o inactivo es no encontrado.
            case "VCE_001" -> VerificacionCorreoErrorCode.VCE_001;
            case "VCE_002" -> VerificacionCorreoErrorCode.VCE_002;
            case "VCE_005" -> VerificacionCorreoErrorCode.VCE_005;
            case "VCE_007" -> VerificacionCorreoErrorCode.VCE_007;
            // HU053-HU055: solicitudes de ingreso a grupo. SIG_001 no es estudiante; SIG_002/SIG_007 no encontrado (las
            // solicitudes ajenas son indistinguibles); periodo, duplicados, ya resuelta o sin cupo son conflicto.
            case "SIG_001" -> SecurityErrorCode.FORBIDDEN;
            case "SIG_002", "SIG_007" -> CommonErrorCode.RESOURCE_NOT_FOUND;
            case "SIG_003", "SIG_004", "SIG_005", "SIG_008", "SIG_013" -> CommonErrorCode.CONFLICT;
            case "SIG_009", "SIG_010" -> CommonErrorCode.VALIDATION_ERROR;
            // FAC_002: el decano ya es titular de otra facultad activa (HU117/HU111).
            case "FAC_002" -> CommonErrorCode.CONFLICT;
            case "VAL_007" -> classifyVal007(operation);
            case "ERR_UNICIDAD_DOCUMENTO" -> UsuarioErrorCode.ERR_UNICIDAD_DOCUMENTO;
            case "USU_002" -> UsuarioErrorCode.ERR_USUARIO_INACTIVO;
            case "HOR_001" -> GrupoErrorCode.ERR_CRUCE_HORARIO_ESTUDIANTE;
            // HOR_002: el docente ya tiene otro grupo en la misma franja del periodo (usp_actualizar_grupo, usp_guardar_horarios_grupo).
            case "HOR_002" -> GrupoErrorCode.ERR_CRUCE_HORARIO_DOCENTE;
            // F1 (usp_guardar_horarios_grupo): franjas del grupo que se cruzan (HOR_003) o horario mal formado (HOR_004).
            case "HOR_003", "HOR_004" -> CommonErrorCode.VALIDATION_ERROR;
            // F3: el docente titular ya definio el horario inicial; los cambios los hace el coordinador (usp_guardar_horarios_grupo).
            case "HOR_005" -> CommonErrorCode.CONFLICT;
            case "SES_001", "PROG_001", "PER_001", "FAC_001", "DEC_001" -> CommonErrorCode.RESOURCE_NOT_FOUND;
            // Catalogos administrativos (HU154/HU151): codigos propios con mensaje institucional.
            case "TPR_001" -> CatalogoErrorCode.TPR_001;
            case "TPR_002" -> CatalogoErrorCode.TPR_002;
            case "ARE_001" -> CatalogoErrorCode.ARE_001;
            case "ARE_002" -> CatalogoErrorCode.ARE_002;
            // HU091-HU093: vertical de docentes del programa. DOC_001/DOC_008 no encontrado; DOC_002/003/006/009 conflicto.
            case "DOC_001" -> DocenteErrorCode.ERR_DOCENTE_NO_EXISTE;
            case "DOC_002" -> DocenteErrorCode.ERR_DOCENTE_YA_REGISTRADO;
            case "DOC_003" -> DocenteErrorCode.ERR_PERSONA_YA_REGISTRADA;
            case "DOC_004" -> DocenteErrorCode.ERR_TIPO_VINCULACION_INVALIDO;
            case "DOC_005" -> DocenteErrorCode.ERR_MODO_VINCULACION_INVALIDO;
            case "DOC_006" -> DocenteErrorCode.ERR_DOCENTE_YA_VINCULADO_PROGRAMA;
            case "DOC_007" -> DocenteErrorCode.ERR_BUSQUEDA_CANDIDATOS_CORTA;
            case "DOC_008" -> DocenteErrorCode.ERR_DOCENTE_NO_VINCULADO_PROGRAMA;
            case "DOC_009" -> DocenteErrorCode.ERR_DOCENTE_SIN_VINCULO_ACTIVO;
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
            // SES-04: resolucion del reclamo. Accion fuera del contrato 400; ya resuelto con otra decision, asistencia
            // presente o sin detalle 409; solicitud inexistente 404.
            case "REV_001" -> AsistenciaErrorCode.ERR_ACCION_REVISION_INVALIDA;
            case "REV_002" -> AsistenciaErrorCode.ERR_RECLAMO_YA_RESUELTO;
            case "REV_003" -> AsistenciaErrorCode.ERR_RECLAMO_ASISTENCIA_NO_JUSTIFICABLE;
            case "REV_004" -> AsistenciaErrorCode.ERR_RECLAMO_SIN_DETALLE_ASISTENCIA;
            case "REV_005" -> AsistenciaErrorCode.ERR_RECLAMO_NO_EXISTE;
            // LB-004D (soporte de revision): ARC_001 es identico para soporte inexistente, ajeno, ya vinculado u
            // huerfano (404 sin distinguir casos); ARC_002 registro en conflicto; ARC_003 metadata invalida;
            // ARC_005 el estudiante no corresponde al ejecutor autenticado.
            case "ARC_001" -> CommonErrorCode.RESOURCE_NOT_FOUND;
            case "ARC_002" -> CommonErrorCode.CONFLICT;
            case "ARC_003" -> CommonErrorCode.VALIDATION_ERROR;
            case "ARC_005" -> SecurityErrorCode.FORBIDDEN;
            // CAT_001: inconsistencia interna de catalogo DB (p. ej. dbo.Estado sin un codigo esperado).
            // Fallo tecnico conocido, no se reclasifica como 4xx.
            case "CAT_001" -> DatabaseErrorCode.ERR_DB_CATALOG_INCONSISTENT;
            // SYS_001: catch-all tecnico de los SP publicos. Caso explicito para que quede clasificado
            // (no caido por el default) aunque su tratamiento sea el mismo que lo no clasificado.
            case "SYS_001" -> DatabaseErrorCode.ERR_DB_UNCLASSIFIED;
            default -> DatabaseErrorCode.ERR_DB_UNCLASSIFIED;
        };
    }

    private static ErrorDefinition classifyGen001(final String operation) {
        if (OPERATION_ACTUALIZAR_COORDINADOR.equals(operation) || OPERATION_CAMBIAR_ESTADO_COORDINADOR.equals(operation)
                || OPERATION_CONSULTAR_HISTORIAL_GRUPOS_DOCENTE.equals(operation)) {
            return CommonErrorCode.RESOURCE_NOT_FOUND;
        }
        return DatabaseErrorCode.ERR_DB_UNCLASSIFIED;
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
