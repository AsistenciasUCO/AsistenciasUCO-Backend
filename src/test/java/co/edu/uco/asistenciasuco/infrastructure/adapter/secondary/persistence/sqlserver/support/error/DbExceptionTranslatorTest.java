package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;



import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.docente.exception.DocenteErrorCode;
import co.edu.uco.asistenciasuco.application.features.estudiante.exception.EstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DbExceptionTranslatorTest {

    private static final String USER_MESSAGE = "Mensaje publico.";
    private static final String TECHNICAL_MESSAGE = "SQLException password token stackTrace";
    private static final String CORRELATION_ID = "93641bab-e3cd-485c-b275-47e7b731e18c";
    private static final String OPERATION = "operacionPrueba";
    private static final String SAFE_INTERNAL_MESSAGE =
            "Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.";

    @ParameterizedTest
    @CsvSource({"SEC_001", "SEC_002", "EST_004"})
    void dbcode_de_autorizacion_lanza_forbidden_semantico(final String dbCode) {
        final ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> translateFailure(
                        "mensaje humano que no clasifica",
                        "DBCODE=" + dbCode + "|detalle arbitrario y mutable",
                        "registrarAsistenciasSesion"
                )
        );

        assertEquals("FORBIDDEN", exception.getCode());
    }

    @ParameterizedTest
    @CsvSource({"ATT_001", "ATT_002", "ATT_003", "GEN_002", "RC_001", "SES_004"})
    void dbcode_de_validacion_lanza_validation_semantico(final String dbCode) {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> translateFailure(
                        "mensaje humano que no clasifica",
                        "DBCODE=" + dbCode + "|detalle arbitrario y mutable",
                        "registrarAsistenciasSesion"
                )
        );

        assertEquals("VALIDATION_ERROR", exception.getCode());
    }

    @Test
    void dbcode_ses_001_lanza_not_found_semantico() {
        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> translateFailure("irrelevante", "DBCODE=SES_001|detalle", "consultarSesion")
        );

        assertEquals("RESOURCE_NOT_FOUND", exception.getCode());
    }

    @Test
    void dbcode_ses_003_lanza_feature_unavailable_y_nunca_simula_exito() {
        final FeatureUnavailableException exception = assertThrows(
                FeatureUnavailableException.class,
                () -> translateFailure("irrelevante", "DBCODE=SES_003|detalle", "cerrarSesion")
        );

        assertEquals("FEATURE_UNAVAILABLE", exception.getCode());
    }

    @ParameterizedTest
    @CsvSource({
            "ERR_CUPO_SUPERADO, ERR_CUPO_SUPERADO",
            "ERR_MATRICULA_DUPLICADA, ERR_MATRICULA_DUPLICADA",
            "ERR_GRUPO_NO_HABILITADO, ERR_GRUPO_NO_HABILITADO"
    })
    void dbcode_de_conflicto_de_grupo_lanza_conflict_semantico(final String dbCode, final String expectedCode) {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure(
                        "mensaje humano",
                        "DBCODE=" + dbCode + "|detalle grupo",
                        "registrarEstudianteEnGrupo"
                )
        );

        assertEquals(expectedCode, exception.getCode());
    }

    @Test
    void dbcode_cupo_inferior_ocupacion_lanza_conflict_semantico() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("irrelevante", "DBCODE=ERR_CUPO_INFERIOR_OCUPACION|detalle", OPERATION)
        );

        assertEquals("CONFLICT", exception.getCode());
    }

    @Test
    void dbcode_programa_facultad_inconsistente_lanza_validation_semantico() {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> translateFailure("irrelevante", "DBCODE=ERR_PROGRAMA_FACULTAD_INCONSISTENTE|detalle", OPERATION)
        );

        assertEquals("VALIDATION_ERROR", exception.getCode());
    }

    @Test
    void dbcode_de_grupo_no_existe_lanza_resource_not_found() {
        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> translateFailure(
                        "mensaje humano",
                        "DBCODE=ERR_GRUPO_NO_EXISTE|detalle grupo inexistente",
                        "consultarGrupo"
                )
        );

        assertEquals("ERR_GRUPO_NO_EXISTE", exception.getCode());
    }

    @Test
    void cambiar_completamente_detalle_de_dbcode_no_cambia_clasificacion() {
        final ForbiddenException first = assertThrows(
                ForbiddenException.class,
                () -> translateFailure("usuario", "DBCODE=SEC_001|texto A", OPERATION)
        );
        final ForbiddenException second = assertThrows(
                ForbiddenException.class,
                () -> translateFailure(
                        "sesion no existe y correo duplicado",
                        "DBCODE=SEC_001|cualquier texto completamente distinto",
                        OPERATION
                )
        );

        assertEquals(first.getClass(), second.getClass());
        assertEquals(first.getCode(), second.getCode());
        assertEquals("FORBIDDEN", second.getCode());
    }

    @ParameterizedTest
    @CsvSource({
            "USU_001, ERR_USUARIO_NO_EXISTE",
            "EST_001, ERR_ESTUDIANTE_NO_EXISTE"
    })
    void dbcode_formal_de_inexistencia_lanza_not_found_con_codigo_especifico(final String dbCode, final String expectedCode) {
        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> translateFailure("irrelevante", "DBCODE=" + dbCode + "|detalle", OPERATION)
        );

        assertEquals(expectedCode, exception.getCode());
    }

    @ParameterizedTest
    @CsvSource({"PROG_001", "PER_001"})
    void dbcode_formal_de_programa_y_periodo_lanza_resource_not_found(final String dbCode) {
        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> translateFailure("irrelevante", "DBCODE=" + dbCode + "|detalle", OPERATION)
        );

        assertEquals("RESOURCE_NOT_FOUND", exception.getCode());
    }

    @ParameterizedTest
    @CsvSource({
            "IDN_001, ERR_IDENTIDAD_USUARIO_CONFLICTO",
            "ERR_UNICIDAD_DOCUMENTO, ERR_UNICIDAD_DOCUMENTO",
            "USU_002, ERR_USUARIO_INACTIVO",
            "HOR_001, ERR_CRUCE_HORARIO_ESTUDIANTE",
            "VAL_006, CONFLICT",
            "GEN_003, CONFLICT"
    })
    void dbcode_formal_de_conflicto_lanza_conflict_con_codigo_especifico(final String dbCode, final String expectedCode) {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("irrelevante", "DBCODE=" + dbCode + "|detalle", OPERATION)
        );

        assertEquals(expectedCode, exception.getCode());
    }

    @ParameterizedTest
    @CsvSource({"PLA_001", "VAL_001", "VAL_002", "VAL_003", "VAL_004", "VAL_005"})
    void dbcode_formal_de_validacion_ampliado_lanza_validation_exception(final String dbCode) {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> translateFailure("irrelevante", "DBCODE=" + dbCode + "|detalle", OPERATION)
        );

        assertEquals("VALIDATION_ERROR", exception.getCode());
    }

    @ParameterizedTest
    @CsvSource({
            "CAT_001, ERR_DB_CATALOG_INCONSISTENT",
            "SYS_001, ERR_DB_UNCLASSIFIED",
            "GEN_001, ERR_DB_UNCLASSIFIED"
    })
    void dbcode_formal_tecnico_o_ambiguo_lanza_database_operation_exception(final String dbCode, final String expectedCode) {
        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> translateFailure("irrelevante", "DBCODE=" + dbCode + "|detalle", OPERATION)
        );

        assertEquals(expectedCode, exception.getCode());
    }

    @Test
    void gen_001_no_se_convierte_en_404_falso_de_tipo_identificacion_aunque_el_texto_lo_sugiera() {
        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> translateFailure(
                        "El tipo de identificacion seleccionado no existe en el sistema.",
                        "DBCODE=GEN_001|tipo de identificacion no existe en uv_tipo_identificacion",
                        "registrarEstudianteEnGrupo"
                )
        );

        assertEquals("ERR_DB_UNCLASSIFIED", exception.getCode());
    }

    @Test
    void val_007_en_crear_usuario_es_politica_de_password_invalida() {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> translateFailure("irrelevante", "DBCODE=VAL_007|detalle", "crearUsuario")
        );

        assertEquals("ERR_PASSWORD_POLITICA_INVALIDA", exception.getCode());
    }

    @Test
    void val_007_en_asistencia_autonoma_es_validation_error() {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> translateFailure("irrelevante", "DBCODE=VAL_007|detalle", "registrarAsistenciaAutonoma")
        );

        assertEquals("VALIDATION_ERROR", exception.getCode());
    }

    @Test
    void val_007_en_resolver_solicitud_revision_es_forbidden() {
        final ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> translateFailure("irrelevante", "DBCODE=VAL_007|detalle", "resolverSolicitudRevisionAsistencia")
        );

        assertEquals("FORBIDDEN", exception.getCode());
    }

    @Test
    void val_007_en_operacion_desconocida_falla_cerrado() {
        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> translateFailure("irrelevante", "DBCODE=VAL_007|detalle", OPERATION)
        );

        assertEquals("ERR_DB_UNCLASSIFIED", exception.getCode());
    }

    @Test
    void dbcode_desconocido_falla_cerrado_y_no_se_convierte_en_validacion_por_el_detalle() {
        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> translateFailure(
                        "estado de asistencia invalido",
                        "DBCODE=NUEVO_999|estado de asistencia y razon de causa",
                        OPERATION
                )
        );

        assertEquals("ERR_DB_UNCLASSIFIED", exception.getCode());
    }

    @Test
    void dbcode_malformado_falla_cerrado_y_no_activa_el_classifier_legacy() {
        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> translateFailure(
                        "El correo ya existe.",
                        "DBCODE=SEC-001|detalle malformado",
                        OPERATION
                )
        );

        assertEquals("ERR_DB_UNCLASSIFIED", exception.getCode());
    }

    @Test
    void mensaje_legacy_sin_dbcode_conserva_compatibilidad() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("El correo ya existe.", "detalle tecnico antiguo", OPERATION)
        );

        assertEquals("ERR_UNICIDAD_CORREO", exception.getCode());
    }

    @ParameterizedTest
    @CsvSource({
            "La matricula ya se encuentra registrada para este grupo., ERR_MATRICULA_DUPLICADA",
            "No hay cupos disponibles para el grupo., ERR_CUPO_SUPERADO",
            "Existe cruce de horario para el estudiante., ERR_CRUCE_HORARIO_ESTUDIANTE",
            "Existe cruce de horario para el docente., ERR_CRUCE_HORARIO_DOCENTE",
            "El correo ya se encuentra registrado., ERR_UNICIDAD_CORREO",
            "El numero de identificacion ya existe., ERR_UNICIDAD_DOCUMENTO"
    })
    void mensajes_conocidos_de_conflicto_lanzan_conflictException_con_codigo_especifico(
            final String userMessage,
            final String expectedCode
    ) {
        final ConflictException exception = assertThrows(ConflictException.class, () -> translateFailure(userMessage, OPERATION));

        assertEquals(expectedCode, exception.getCode());
        assertCatalogMessage(expectedCode, exception.getMessage());
    }

    @Test
    void conflicto_identidad_de_db_se_traduce_a_conflictException_con_codigo_neutral() {
        final ConflictException exception = assertThrows(ConflictException.class, () -> translateFailure(
                "Los datos de identificación suministrados no corresponden de forma unívoca al mismo usuario.",
                "Conflicto de identidad: correo y documento compuesto no resuelven de forma unívoca al mismo usuario en uv_usuario.",
                "crearDecano"
        ));

        assertEquals("ERR_IDENTIDAD_USUARIO_CONFLICTO", exception.getCode());
        assertEquals(UsuarioErrorCode.ERR_IDENTIDAD_USUARIO_CONFLICTO.defaultMessage(), exception.getMessage());
    }

    @Test
    void clasificacion_de_identidad_no_captura_conflicto_generico() {
        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> translateFailure("Conflicto de horario nuevo.", "crearDecano"));

        assertEquals("ERR_DB_UNCLASSIFIED", exception.getCode());
    }

    @Test
    void cruce_horario_sin_actor_en_operacion_asignar_docente_lanza_codigo_docente() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("Existe cruce de horario.", "asignarDocenteAGrupo")
        );

        assertEquals("ERR_CRUCE_HORARIO_DOCENTE", exception.getCode());
    }

    @Test
    void cruce_horario_sin_actor_en_registro_estudiante_lanza_codigo_estudiante() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("Existe cruce de horario.", "registrarEstudianteEnGrupo")
        );

        assertEquals("ERR_CRUCE_HORARIO_ESTUDIANTE", exception.getCode());
    }

    @Test
    void cupo_lleno_en_mensaje_tecnico_lanza_cupo_superado_sin_exponer_mensaje_tecnico() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("No fue posible registrar el estudiante.", "Cupo lleno", "registrarEstudianteEnGrupo")
        );

        assertEquals("ERR_CUPO_SUPERADO", exception.getCode());
        assertEquals(GrupoErrorCode.ERR_CUPO_SUPERADO.defaultMessage(), exception.getMessage());
    }

    @Test
    void mensaje_real_tamaoo_moximo_lanza_cupo_superado() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("El grupo supero su tamaoo moximo.", "registrarEstudianteEnGrupo")
        );

        assertEquals("ERR_CUPO_SUPERADO", exception.getCode());
    }

    @Test
    void grupo_no_habilitado_lanza_conflictException_con_codigo_especifico() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("El grupo no se encuentra habilitado.", "registrarEstudianteEnGrupo")
        );

        assertEquals("ERR_GRUPO_NO_HABILITADO", exception.getCode());
    }

    @ParameterizedTest
    @CsvSource({
            "El usuario se encuentra inactivo., ERR_USUARIO_INACTIVO",
            "El estudiante se encuentra inactivo., ERR_ESTUDIANTE_INACTIVO",
            "El docente se encuentra inactivo., ERR_DOCENTE_INACTIVO",
            "El usuario ya se encuentra registrado como docente., ERR_DOCENTE_YA_REGISTRADO"
    })
    void mensajes_reales_inactividad_y_docente_duplicado_lanzan_conflictException_con_codigo_especifico(
            final String userMessage,
            final String expectedCode
    ) {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure(userMessage, "asignarDocenteAGrupo")
        );

        assertEquals(expectedCode, exception.getCode());
        assertCatalogMessage(expectedCode, exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "El primer nombre contiene caracteres no permitidos.",
            "El primer apellido tiene formato invalido."
    })
    void mensajes_reales_nombre_persona_invalido_lanzan_validationException(final String userMessage) {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> translateFailure(userMessage, "registrarDocenteDesdeUsuario")
        );

        assertEquals("ERR_NOMBRE_PERSONA_INVALIDO", exception.getCode());
        assertEquals(UsuarioErrorCode.ERR_NOMBRE_PERSONA_INVALIDO.defaultMessage(), exception.getMessage());
    }

    @Test
    void mensaje_real_tipo_identificacion_con_interrogacion_lanza_notFound_con_codigo_especifico() {
        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> translateFailure(
                        "El tipo de identificaci?n seleccionado no existe en el sistema.",
                        "Validacion en uv_tipo_identificacion",
                        "registrarEstudianteEnGrupo"
                )
        );

        assertEquals("ERR_TIPO_IDENTIFICACION_NO_EXISTE", exception.getCode());
    }

    @ParameterizedTest
    @CsvSource({
            "El usuario no existe., ERR_USUARIO_NO_EXISTE",
            "El estudiante no existe., ERR_ESTUDIANTE_NO_EXISTE",
            "El docente no existe., ERR_DOCENTE_NO_EXISTE",
            "El grupo no existe., ERR_GRUPO_NO_EXISTE",
            "El tipo de identificacion no existe., ERR_TIPO_IDENTIFICACION_NO_EXISTE",
            "La sesion no existe., ERR_SESION_NO_EXISTE"
    })
    void mensajes_conocidos_no_encontrado_lanzan_resourceNotFoundException_con_codigo_especifico(
            final String userMessage,
            final String expectedCode
    ) {
        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> translateFailure(userMessage, OPERATION)
        );

        assertEquals(expectedCode, exception.getCode());
    }

    @Test
    void mensaje_rc_001_estado_de_asistencia_invalido_lanza_validationException_no_500() {
        // Texto verificado en vivo contra dbo.uv_mensaje_usuario / dbo.uv_mensaje_tecnico para el
        // codigo de catalogo RC_001 (RazonCausa.codigo inexistente).
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> translateFailure(
                        "El código de estado de asistencia \"ABC\" no corresponde a una razón de causa registrada en el catálogo.",
                        "Error de catálogo: RazonCausa.codigo = [ABC] no existe. No se permite creación dinámica de estados en dbo.RazonCausa.",
                        "registrarAsistenciasSesion"
                )
        );

        assertEquals(AsistenciaErrorCode.ERR_ESTADO_ASISTENCIA_INVALIDO.code(), exception.getCode());
    }

    @Test
    void mensaje_sesion_no_encontrada_lanza_resourceNotFoundException_no_500() {
        // Texto verificado en vivo para SES_001.
        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> translateFailure(
                        "La sesión de clase especificada no existe o no se encuentra activa.",
                        "Error de búsqueda: Sesión no encontrada o cerrada en uv_sesion: 00000000-0000-0000-0000-000000000000.",
                        "registrarAsistenciasSesion"
                )
        );

        assertEquals("ERR_SESION_NO_EXISTE", exception.getCode());
    }

    @Test
    void mensaje_estudiante_fuera_de_grupo_lanza_forbiddenException_no_500() {
        // Texto verificado en vivo para EST_004.
        final ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> translateFailure(
                        "El estudiante no pertenece al grupo de la sesion seleccionada.",
                        "Validación de pertenencia fallida: El estudiante no está inscrito en el grupo de la sesión.",
                        "registrarAsistenciasSesion"
                )
        );

        assertEquals("ERR_ESTUDIANTE_NO_PERTENECE_SESION", exception.getCode());
    }

    @Test
    void mensaje_forbidden_lanza_forbiddenException_con_codigo_especifico() {
        final ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> translateFailure("El estudiante no pertenece a la sesion.", OPERATION)
        );

        assertEquals("ERR_ESTUDIANTE_NO_PERTENECE_SESION", exception.getCode());
    }

    @Test
    void mensaje_desconocido_lanza_databaseOperationException_con_err_db_unclassified() {
        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> translateFailure("La operacion no pudo completarse por una regla nueva.", OPERATION)
        );

        assertEquals("ERR_DB_UNCLASSIFIED", exception.getCode());
        assertEquals(SAFE_INTERNAL_MESSAGE, exception.getMessage());
    }

    @Test
    void mensaje_null_lanza_databaseOperationException_con_err_db_unclassified() {
        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> translateFailure(null, OPERATION)
        );

        assertEquals("ERR_DB_UNCLASSIFIED", exception.getCode());
        assertEquals(SAFE_INTERNAL_MESSAGE, exception.getMessage());
    }

    @Test
    void mensaje_usuario_null_o_vacio_usa_fallback_seguro_sin_mensaje_tecnico() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("El correo ya existe.", OPERATION)
        );

        assertEquals("ERR_UNICIDAD_CORREO", exception.getCode());
        assertEquals(UsuarioErrorCode.ERR_UNICIDAD_CORREO.defaultMessage(), exception.getMessage());
    }

    @Test
    void mensaje_tecnico_no_se_usa_como_mensaje_publico_de_la_excepcion() {
        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> translateFailure(null, OPERATION)
        );

        assertEquals(SAFE_INTERNAL_MESSAGE, exception.getMessage());
    }

    @Test
    void estado_exitoso_no_lanza_excepcion() {
        assertDoesNotThrow(() -> DbExceptionTranslator.throwIfFailed(
                true,
                USER_MESSAGE,
                TECHNICAL_MESSAGE,
                CORRELATION_ID,
                OPERATION
        ));
    }

    private void translateFailure(final String userMessage, final String operation) {
        translateFailure(userMessage, TECHNICAL_MESSAGE, operation);
    }

    private void translateFailure(final String userMessage, final String technicalMessage, final String operation) {
        DbExceptionTranslator.throwIfFailed(
                false,
                userMessage,
                technicalMessage,
                CORRELATION_ID,
                operation
        );
    }

    private void assertCatalogMessage(final String expectedCode, final String actualMessage) {
        assertEquals(catalogMessage(expectedCode), actualMessage);
    }

    private String catalogMessage(final String expectedCode) {
        for (final ErrorDefinition errorDefinition : new ErrorDefinition[] {
                GrupoErrorCode.ERR_MATRICULA_DUPLICADA,
                GrupoErrorCode.ERR_CUPO_SUPERADO,
                GrupoErrorCode.ERR_CRUCE_HORARIO_ESTUDIANTE,
                GrupoErrorCode.ERR_CRUCE_HORARIO_DOCENTE,
                UsuarioErrorCode.ERR_UNICIDAD_CORREO,
                UsuarioErrorCode.ERR_UNICIDAD_DOCUMENTO,
                UsuarioErrorCode.ERR_USUARIO_INACTIVO,
                EstudianteErrorCode.ERR_ESTUDIANTE_INACTIVO,
                DocenteErrorCode.ERR_DOCENTE_INACTIVO,
                DocenteErrorCode.ERR_DOCENTE_YA_REGISTRADO
        }) {
            if (errorDefinition.code().equals(expectedCode)) {
                return errorDefinition.defaultMessage();
            }
        }
        throw new IllegalArgumentException(expectedCode);
    }
}
