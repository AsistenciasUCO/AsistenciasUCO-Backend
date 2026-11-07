package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SES-04: rechazos de usp_resolver_solicitud_revision_asistencia y su traduccion HTTP (codigo publico, sin detalle
 * tecnico): accion invalida 400, solicitud inexistente 404, ya resuelta / no justificable / sin detalle 409.
 */
class DbFailureClassifierReclamoRevisionTest {

    @ParameterizedTest
    @CsvSource({
            "REV_001, ERR_ACCION_REVISION_INVALIDA, VALIDATION",
            "REV_002, ERR_RECLAMO_YA_RESUELTO, CONFLICT",
            "REV_003, ERR_RECLAMO_ASISTENCIA_NO_JUSTIFICABLE, CONFLICT",
            "REV_004, ERR_RECLAMO_SIN_DETALLE_ASISTENCIA, CONFLICT",
            "REV_005, ERR_RECLAMO_NO_EXISTE, NOT_FOUND"
    })
    void rechazo_de_resolucion_se_traduce_al_codigo_publico(
            final String codigoDb, final AsistenciaErrorCode esperado, final String tipo) {
        assertEquals(esperado, DbFailureClassifier.classify(
                "mensaje", "DBCODE=" + codigoDb + "|detalle", "resolverSolicitudRevisionAsistencia"));

        final ApplicationException exception = assertThrows(ApplicationException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=" + codigoDb + "|SES-04 detalle tecnico", "corr", "resolverSolicitudRevisionAsistencia"));
        assertEquals(esperado.code(), exception.getCode());
        assertEquals(esperado.defaultMessage(), exception.getMessage());
        switch (tipo) {
            case "VALIDATION" -> assertInstanceOf(ValidationException.class, exception);
            case "NOT_FOUND" -> assertInstanceOf(ResourceNotFoundException.class, exception);
            default -> assertInstanceOf(ConflictException.class, exception);
        }
    }

    // ---- CULM-001 D.1 (HU026B): plazo de excusas vencido (REV_020, 409) y configuracion ausente (REV_025, 500) ----

    @Test
    void plazo_vencido_es_409_con_el_codigo_publico_y_el_mensaje_del_catalogo_que_trae_la_fecha() {
        assertEquals(AsistenciaErrorCode.ERR_PLAZO_JUSTIFICACION_VENCIDO, DbFailureClassifier.classify(
                "mensaje", "DBCODE=REV_020|HU026B: instante x >= limite y", "solicitarRevisionAsistencia"));

        final String mensajeDb = "El plazo para justificar esta inasistencia venció el 2026-11-09.";
        final ApplicationException exception = assertThrows(ApplicationException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, mensajeDb, "DBCODE=REV_020|HU026B: instante x >= limite y", "corr", "solicitarRevisionAsistencia"));

        assertInstanceOf(ConflictException.class, exception);
        assertEquals("ERR_PLAZO_JUSTIFICACION_VENCIDO", exception.getCode());
        assertEquals(mensajeDb, exception.getMessage());
    }

    @Test
    void plazo_vencido_sin_mensaje_de_usuario_usa_el_texto_por_defecto() {
        final ApplicationException exception = assertThrows(ApplicationException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, " ", "DBCODE=REV_020|detalle", "corr", "solicitarRevisionAsistencia"));

        assertInstanceOf(ConflictException.class, exception);
        assertEquals(AsistenciaErrorCode.ERR_PLAZO_JUSTIFICACION_VENCIDO.defaultMessage(), exception.getMessage());
    }

    @Test
    void configuracion_del_plazo_ausente_es_un_fallo_tecnico_500_con_su_codigo() {
        assertEquals(AsistenciaErrorCode.ERR_CONFIGURACION_PLAZO_JUSTIFICACION, DbFailureClassifier.classify(
                "mensaje", "DBCODE=REV_025|HU026B: falta el parametro", "solicitarRevisionAsistencia"));

        final RuntimeException exception = assertThrows(RuntimeException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "No es posible validar el plazo de justificación en este momento.", "DBCODE=REV_025|detalle", "corr",
                "solicitarRevisionAsistencia"));

        assertInstanceOf(DatabaseOperationException.class, exception);
        assertEquals(ErrorKind.TECHNICAL, AsistenciaErrorCode.ERR_CONFIGURACION_PLAZO_JUSTIFICACION.kind());
        assertEquals(ErrorKind.CONFLICT, AsistenciaErrorCode.ERR_PLAZO_JUSTIFICACION_VENCIDO.kind());
    }

    @Test
    void el_plazo_vencido_tambien_se_traduce_en_las_consultas_con_throw() {
        assertInstanceOf(ConflictException.class, DbExceptionTranslator.translateDbCode("REV_020", "consultar").orElseThrow());
        assertTrue(DbExceptionTranslator.translateDbCode("REV_025", "consultar").isEmpty());
    }
}
