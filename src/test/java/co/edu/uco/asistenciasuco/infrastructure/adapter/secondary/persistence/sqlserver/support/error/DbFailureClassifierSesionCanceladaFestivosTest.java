package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.TechnicalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CULM-001 B.1 (CONTRACT 3.1, 2.10): los codigos nuevos de la DB se exponen tal cual en {@code code} con su HTTP;
 * {@code SES_010} se publica como {@code ERR_SESION_CANCELADA} (409), igual que {@code SES_007..009} (NB-1).
 */
class DbFailureClassifierSesionCanceladaFestivosTest {

    @ParameterizedTest
    @CsvSource({
            "SES_011, cancelarSesion, SES_011, CONFLICT",
            "SES_012, cancelarSesion, SES_012, CONFLICT",
            "SES_013, cancelarSesion, SES_013, VALIDATION",
            "SES_016, cancelarSesion, SES_016, CONFLICT",
            "FES_001, cargarDiasFestivos, FES_001, VALIDATION",
            "FES_002, cargarDiasFestivos, FES_002, VALIDATION",
            "FES_003, cargarDiasFestivos, FES_003, TECHNICAL",
            "FES_004, cargarDiasFestivos, FES_004, VALIDATION",
            "FES_006, aplicarFestivosPeriodo, FES_006, VALIDATION",
            "FES_007, aplicarFestivosPeriodo, FES_007, NOT_FOUND",
            "FES_009, aplicarFestivosPeriodo, FES_009, TECHNICAL",
            "SES_010, registrarAsistenciasSesion, ERR_SESION_CANCELADA, CONFLICT"
    })
    void los_codigos_de_b1_se_clasifican_con_su_codigo_publico_y_su_tipo(
            final String codigoDb, final String operacion, final String codigoPublico, final ErrorKind tipo) {
        final ErrorDefinition clasificado = DbFailureClassifier.classify("mensaje", "DBCODE=" + codigoDb + "|detalle", operacion);
        assertEquals(codigoPublico, clasificado.code());
        assertEquals(tipo, clasificado.kind());
    }

    @Test
    void la_sesion_cancelada_en_el_lote_es_un_conflicto_sin_detalle_tecnico() {
        final ConflictException error = assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=SES_010|Sesion cancelada", "corr", "registrarAsistenciasSesion"));
        assertEquals("ERR_SESION_CANCELADA", error.getCode());
        assertEquals(AsistenciaErrorCode.ERR_SESION_CANCELADA.defaultMessage(), error.getMessage());
    }

    @Test
    void los_rechazos_de_cancelacion_y_festivos_son_excepciones_de_negocio_con_el_codigo_de_la_db() {
        assertEquals("SES_012", assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=SES_012|con asistencia", "corr", "cancelarSesion")).getCode());
        assertEquals("SES_013", assertThrows(ValidationException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=SES_013|motivo corto", "corr", "cancelarSesion")).getCode());
        assertEquals("FES_007", assertThrows(ResourceNotFoundException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=FES_007|periodo", "corr", "aplicarFestivosPeriodo")).getCode());
        assertEquals("FES_009", assertThrows(TechnicalException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=FES_009|grupos", "corr", "aplicarFestivosPeriodo")).getErrorDefinition().orElseThrow().code());
    }
}
