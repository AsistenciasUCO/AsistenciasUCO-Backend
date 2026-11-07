package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.exception.DecanoErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CULM-001 A.2 (A2-B-11, A2-B-12, A2-B-13, A2-B-14): codigos nuevos de la BD. NTF_002 (reintento de una notificacion
 * inexistente) y REV_006 (reclamo ajeno o inexistente) son 404; REV_007 (reclamo no pendiente) y DEC_002 (decano de una
 * facultad con programas activos) son 409; SES_017 (sesion solapada) es 400 como SES_004, porque el OpenAPI de las
 * sesiones declara 400 y no 409.
 */
class DbFailureClassifierCulm001ATest {

    private static final String USER = "Mensaje publico.";

    private static String tecnico(final String codigo) {
        return "DBCODE=" + codigo + "|detalle tecnico mutable";
    }

    @Test
    void ntf_002_y_rev_006_son_recurso_no_encontrado() {
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, DbFailureClassifier.classify(USER, tecnico("NTF_002"), "reintentarNotificacionSaliente"));
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, DbFailureClassifier.classify(USER, tecnico("REV_006"), "retirarSolicitudRevisionAsistencia"));
        assertThrows(ResourceNotFoundException.class,
                () -> DbExceptionTranslator.throwIfFailed(false, USER, tecnico("NTF_002"), "c", "reintentarNotificacionSaliente"));
    }

    @Test
    void rev_007_y_dec_002_son_conflicto() {
        assertEquals(CommonErrorCode.CONFLICT, DbFailureClassifier.classify(USER, tecnico("REV_007"), "retirarSolicitudRevisionAsistencia"));
        // F-A-S02 (auditoria en pantalla): DEC_002 conserva su codigo de catalogo (como COO_001) para que el usuario reciba
        // el texto institucional y no el generico de CONFLICT; sigue siendo 409.
        final var dec002 = DbFailureClassifier.classify(USER, tecnico("DEC_002"), "cambiarEstadoDecano");
        assertEquals(DecanoErrorCode.DEC_002, dec002);
        assertEquals("DEC_002", dec002.code());
        assertEquals(ErrorKind.CONFLICT, dec002.kind());
        assertThrows(ConflictException.class,
                () -> DbExceptionTranslator.throwIfFailed(false, USER, tecnico("DEC_002"), "c", "cambiarEstadoDecano"));
    }

    @Test
    void ses_017_es_validacion_como_ses_004() {
        // F-A-S04 (auditoria en pantalla): SES_017 es 400 como SES_004, pero con su propio codigo de catalogo para que el
        // formulario muestre el texto del solape (A2-F-09, N11) y no el generico de VALIDATION_ERROR.
        final var ses017 = DbFailureClassifier.classify(USER, tecnico("SES_017"), "crearSesion");
        assertEquals(SesionErrorCode.SES_017, ses017);
        assertEquals("SES_017", ses017.code());
        assertEquals(DbFailureClassifier.classify(USER, tecnico("SES_004"), "crearSesion").kind(), ses017.kind());
        assertEquals(ErrorKind.VALIDATION, ses017.kind());
        assertThrows(ValidationException.class,
                () -> DbExceptionTranslator.throwIfFailed(false, USER, tecnico("SES_017"), "c", "actualizarSesion"));
    }

    @Test
    void la_traduccion_de_consultas_reutiliza_la_misma_tabla() {
        assertInstanceOf(ForbiddenException.class, DbExceptionTranslator.translateDbCode("SEC_002", null).orElseThrow());
        assertInstanceOf(ResourceNotFoundException.class, DbExceptionTranslator.translateDbCode("NTF_002", null).orElseThrow());
        assertInstanceOf(co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException.class,
                DbExceptionTranslator.translateDbCode("SES_003", null).orElseThrow());
        assertTrue(DbExceptionTranslator.translateDbCode("SYS_001", null).isEmpty());
        assertTrue(DbExceptionTranslator.translateDbCode(" ", null).isEmpty());
        assertTrue(DbExceptionTranslator.translateDbCode(null, null).isEmpty());
    }
}
