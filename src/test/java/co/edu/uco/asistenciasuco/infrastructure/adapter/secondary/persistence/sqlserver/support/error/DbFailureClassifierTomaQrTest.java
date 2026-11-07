package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** CULM-001 B.2 (CONTRACT 3.1, 2.10): TOM_* y AQR_* se exponen tal cual en {@code code} con su HTTP. */
class DbFailureClassifierTomaQrTest {

    @ParameterizedTest
    @CsvSource({
            "TOM_001, NOT_FOUND", "TOM_002, VALIDATION", "TOM_003, CONFLICT", "TOM_004, CONFLICT",
            "AQR_001, NOT_FOUND", "AQR_003, CONFLICT", "AQR_004, FORBIDDEN", "AQR_005, CONFLICT",
            "AQR_006, CONFLICT", "AQR_007, VALIDATION"
    })
    void los_codigos_de_b2_conservan_su_codigo_y_su_tipo(final String codigo, final ErrorKind tipo) {
        final ErrorDefinition clasificado = DbFailureClassifier.classify("mensaje", "DBCODE=" + codigo + "|detalle", "abrirTomaAsistencia");
        assertEquals(codigo, clasificado.code());
        assertEquals(tipo, clasificado.kind());
    }

    @Test
    void cada_codigo_se_traduce_a_la_excepcion_de_su_http() {
        assertInstanceOf(ResourceNotFoundException.class, DbExceptionTranslator.translateDbCode("TOM_001", null).orElseThrow());
        assertInstanceOf(ResourceNotFoundException.class, DbExceptionTranslator.translateDbCode("AQR_001", null).orElseThrow());
        assertInstanceOf(ValidationException.class, DbExceptionTranslator.translateDbCode("TOM_002", null).orElseThrow());
        assertInstanceOf(ValidationException.class, DbExceptionTranslator.translateDbCode("AQR_007", null).orElseThrow());
        assertInstanceOf(ForbiddenException.class, DbExceptionTranslator.translateDbCode("AQR_004", null).orElseThrow());
        assertEquals("TOM_003", assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=TOM_003|toma abierta", "corr", "abrirTomaAsistencia")).getCode());
        assertEquals("AQR_006", assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=AQR_006|pin", "corr", "abrirQrToma")).getCode());
        assertEquals("AQR_005", assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=AQR_005|cerrada", "corr", "registrarAsistenciaPorToken")).getCode());
    }
}
