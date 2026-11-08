package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.docente.exception.DocenteErrorCode;
import co.edu.uco.asistenciasuco.application.features.estudiante.exception.EstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.festivos.exception.FestivoErrorCode;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.exception.AsistenciaQrErrorCode;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.exception.TomaAsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.admin.exception.AdmisionEstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.admin.exception.CatalogoErrorCode;
import co.edu.uco.asistenciasuco.application.features.admin.exception.DecanoErrorCode;
import co.edu.uco.asistenciasuco.application.features.decano.exception.CoordinadorErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.exception.TipoIdentificacionErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.exception.MatriculaQrErrorCode;
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
    // P5 (2026-11-04): OP_* de AsignaturaJpaRepository.
    private static final String OPERATION_CREAR_ASIGNATURA = "crearAsignatura";
    private static final String OPERATION_CORREGIR_IDENTIDAD = "corregirIdentidadUsuario";
    private static final String OPERATION_ACTUALIZAR_ASIGNATURA = "actualizarAsignatura";

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
            // Asignaturas (decision de John 2026-11-04): creditos obligatorios y mayores que cero.
            case "ASI_001" -> CommonErrorCode.VALIDATION_ERROR;
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
            // HU159: correccion de identidad. Motivo invalido es validacion; la correccion de documento apagada por
            // parametro (IDENTIDAD.PERMITIR_CORREGIR_DOCUMENTO) es una regla de negocio en conflicto.
            // NTF-002B: administracion de plantillas y cola outbox (marcadores invalidos, version inexistente,
            // notificacion fuera de un estado reintentable).
            case "NTF_004" -> CommonErrorCode.VALIDATION_ERROR;
            // CULM-001 A.2-6: NTF_002 notificacion inexistente al reintentar desde la cola (antes ERR_DB_UNCLASSIFIED, 500).
            case "NTF_002", "NTF_005" -> CommonErrorCode.RESOURCE_NOT_FOUND;
            case "NTF_008" -> CommonErrorCode.CONFLICT;
            case "IDE_001" -> CommonErrorCode.VALIDATION_ERROR;
            case "IDE_003" -> CommonErrorCode.CONFLICT;
            // IDENT-001: correo institucional y verificacion. VCE_001/VCE_002 son entradas invalidas (400, VCE_002 identico para
            // cualquier token invalido); VCE_005 ya verificado es conflicto; VCE_007 usuario inexistente o inactivo es no encontrado.
            // HU176B: matricula por QR/PIN. MQR_001 es generico para cualquier token invalido (anti-enumeracion).
            case "MQR_001" -> MatriculaQrErrorCode.MQR_001;
            case "MQR_004" -> MatriculaQrErrorCode.MQR_004;
            case "MQR_005" -> MatriculaQrErrorCode.MQR_005;
            case "MQR_006" -> MatriculaQrErrorCode.MQR_006;
            case "MQR_007" -> MatriculaQrErrorCode.MQR_007;
            case "VCE_001" -> VerificacionCorreoErrorCode.VCE_001;
            case "VCE_002" -> VerificacionCorreoErrorCode.VCE_002;
            case "VCE_005" -> VerificacionCorreoErrorCode.VCE_005;
            case "VCE_007" -> VerificacionCorreoErrorCode.VCE_007;
            // FAC_002: el decano ya es titular de otra facultad activa (HU117/HU111).
            case "FAC_002" -> CommonErrorCode.CONFLICT;
            case "VAL_007" -> classifyVal007(operation);
            // SES-04: resolucion del reclamo. Accion fuera del contrato 400; ya resuelto con otra decision, asistencia
            // presente o sin detalle 409; solicitud inexistente 404.
            case "REV_001" -> AsistenciaErrorCode.ERR_ACCION_REVISION_INVALIDA;
            case "REV_002" -> AsistenciaErrorCode.ERR_RECLAMO_YA_RESUELTO;
            case "REV_003" -> AsistenciaErrorCode.ERR_RECLAMO_ASISTENCIA_NO_JUSTIFICABLE;
            case "REV_004" -> AsistenciaErrorCode.ERR_RECLAMO_SIN_DETALLE_ASISTENCIA;
            case "REV_005" -> AsistenciaErrorCode.ERR_RECLAMO_NO_EXISTE;
            // CULM-001 A.2-7: retiro del reclamo propio. REV_006 inexistente o ajeno (indistinguibles); REV_007 no pendiente.
            case "REV_006" -> CommonErrorCode.RESOURCE_NOT_FOUND;
            case "REV_007" -> CommonErrorCode.CONFLICT;
            // CULM-001 D.4 (HU156): admision de estudiantes. Codigos propios para que el usuario reciba el texto institucional.
            case "ADS_002" -> AdmisionEstudianteErrorCode.ADS_002;
            case "ADS_003" -> AdmisionEstudianteErrorCode.ADS_003;
            case "ADS_004" -> AdmisionEstudianteErrorCode.ADS_004;
            case "ADS_005" -> AdmisionEstudianteErrorCode.ADS_005;
            case "ADS_006" -> AdmisionEstudianteErrorCode.ADS_006;
            // CULM-001 A.2-8: no se desactiva al decano de una facultad con programas activos. 409 con su codigo de
            // catalogo (como COO_001) para que el usuario reciba el texto institucional (F-A-S02).
            case "DEC_002" -> DecanoErrorCode.DEC_002;
            // CULM-001 A.2-10: sesion solapada en el grupo o con otro grupo del docente. 400 como SES_004: el OpenAPI de
            // POST/PATCH/PUT /api/v1/sesiones y POST /api/v2/sesiones declara 400 y no 409 (§8.1 del contrato). Con su
            // codigo de catalogo para que el formulario muestre el texto del solape (A2-F-09, N11, F-A-S04).
            case "SES_017" -> SesionErrorCode.SES_017;
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
            // SES-01: ventana de registro de asistencia (lote y autonomo). La DB la repite bajo bloqueo con su reloj UTC:
            // sesion no iniciada, cerrada (sin tolerancia) o con procedencia temporal indeterminada son conflictos de estado.
            case "SES_007" -> AsistenciaErrorCode.ERR_SESION_NO_INICIADA;
            case "SES_008" -> AsistenciaErrorCode.ERR_SESION_CERRADA;
            case "SES_009" -> AsistenciaErrorCode.ERR_SESION_HORARIO_INDETERMINADO;
            // CULM-001 B.1: sesion cancelada (NB-1: SES_010 se publica como ERR_SESION_CANCELADA, igual que SES_007..009) y
            // cancelacion por el docente HU047 (codigos de la DB expuestos tal cual).
            case "SES_010" -> AsistenciaErrorCode.ERR_SESION_CANCELADA;
            case "SES_011" -> SesionErrorCode.SES_011;
            case "SES_012" -> SesionErrorCode.SES_012;
            case "SES_013" -> SesionErrorCode.SES_013;
            case "SES_016" -> SesionErrorCode.SES_016;
            // CULM-001 B.1: festivos (carga, aplicacion y consultas del administrador).
            // CULM-001 B.2: tomas de asistencia y QR de asistencia (el codigo de la DB se expone tal cual).
            case "TOM_001" -> TomaAsistenciaErrorCode.TOM_001;
            case "TOM_002" -> TomaAsistenciaErrorCode.TOM_002;
            case "TOM_003" -> TomaAsistenciaErrorCode.TOM_003;
            case "TOM_004" -> TomaAsistenciaErrorCode.TOM_004;
            case "AQR_001" -> AsistenciaQrErrorCode.AQR_001;
            case "AQR_002" -> AsistenciaQrErrorCode.AQR_002;
            case "AQR_003" -> AsistenciaQrErrorCode.AQR_003;
            case "AQR_004" -> AsistenciaQrErrorCode.AQR_004;
            case "AQR_005" -> AsistenciaQrErrorCode.AQR_005;
            case "AQR_006" -> AsistenciaQrErrorCode.AQR_006;
            case "AQR_007" -> AsistenciaQrErrorCode.AQR_007;
            case "AQR_010" -> AsistenciaQrErrorCode.AQR_010;
            case "FES_001" -> FestivoErrorCode.FES_001;
            case "FES_002" -> FestivoErrorCode.FES_002;
            case "FES_003" -> FestivoErrorCode.FES_003;
            case "FES_004" -> FestivoErrorCode.FES_004;
            case "FES_006" -> FestivoErrorCode.FES_006;
            case "FES_007" -> FestivoErrorCode.FES_007;
            case "FES_009" -> FestivoErrorCode.FES_009;
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
                || OPERATION_CONSULTAR_HISTORIAL_GRUPOS_DOCENTE.equals(operation)
                // P5: area o componente inexistente al guardar una asignatura.
                || OPERATION_CREAR_ASIGNATURA.equals(operation) || OPERATION_ACTUALIZAR_ASIGNATURA.equals(operation)
                // HU159: el estudiante o el decano no existe o esta fuera de la jerarquia del ejecutor.
                || OPERATION_CORREGIR_IDENTIDAD.equals(operation)) {
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
