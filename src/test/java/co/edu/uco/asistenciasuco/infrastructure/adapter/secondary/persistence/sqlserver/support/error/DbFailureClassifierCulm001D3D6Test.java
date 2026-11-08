package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** CULM-001 D.3 y D.6 (CONTRACT v2 §4.3, §7.3): los 12 codigos PRM/AUD se exponen tal cual con su HTTP. */
class DbFailureClassifierCulm001D3D6Test {

    @ParameterizedTest
    @CsvSource({
            "PRM_002, NOT_FOUND",
            "PRM_003, FORBIDDEN",
            "PRM_004, VALIDATION",
            "PRM_005, VALIDATION",
            "PRM_006, VALIDATION",
            "PRM_007, VALIDATION",
            "PRM_008, CONFLICT",
            "PRM_009, VALIDATION",
            "PRM_010, CONFLICT",
            "PRM_012, CONFLICT",
            "AUD_001, CONFLICT",
            "AUD_002, VALIDATION"
    })
    void los_codigos_se_clasifican_con_su_codigo_publico_y_su_tipo(final String codigoDb, final ErrorKind tipo) {
        final ErrorDefinition clasificado = DbFailureClassifier.classify("mensaje", "DBCODE=" + codigoDb + "|detalle", "actualizarParametro");

        assertEquals(codigoDb, clasificado.code());
        assertEquals(tipo, clasificado.kind());
    }

    @ParameterizedTest
    @CsvSource({
            "PRM_002, ResourceNotFoundException",
            "PRM_003, ForbiddenException",
            "PRM_005, ValidationException",
            "PRM_008, ConflictException",
            "PRM_012, ConflictException",
            "AUD_001, ConflictException",
            "AUD_002, ValidationException"
    })
    void el_traductor_lanza_la_excepcion_de_negocio_con_el_codigo_de_la_db(final String codigoDb, final String excepcion) {
        final RuntimeException error = assertThrows(RuntimeException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=" + codigoDb + "|detalle", "corr", "actualizarParametro"));

        final Class<?> esperada = switch (excepcion) {
            case "ResourceNotFoundException" -> ResourceNotFoundException.class;
            case "ForbiddenException" -> ForbiddenException.class;
            case "ValidationException" -> ValidationException.class;
            default -> ConflictException.class;
        };
        assertInstanceOf(esperada, error);
        assertEquals(codigoDb, ((co.edu.uco.asistenciasuco.application.exception.ApplicationException) error).getCode());
    }

    @ParameterizedTest
    @CsvSource({"PRM_012", "AUD_001", "AUD_002", "PRM_002"})
    void los_sp_de_consulta_traducen_el_throw_con_la_misma_tabla(final String codigoDb) {
        assertInstanceOf(RuntimeException.class, DbExceptionTranslator.translateDbCode(codigoDb, "consultarParametrosAdmin").orElseThrow());
    }
}
