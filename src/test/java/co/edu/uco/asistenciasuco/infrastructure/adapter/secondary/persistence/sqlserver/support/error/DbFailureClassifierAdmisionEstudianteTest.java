package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.exception.AdmisionEstudianteErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** CULM-001 D.4 (HU156): rechazos de usp_admitir_estudiante y su traduccion HTTP con codigo publico propio. */
class DbFailureClassifierAdmisionEstudianteTest {

    @ParameterizedTest
    @CsvSource({
            "ADS_002, ADS_002, CONFLICT",
            "ADS_003, ADS_003, FORBIDDEN",
            "ADS_004, ADS_004, CONFLICT",
            "ADS_005, ADS_005, VALIDATION",
            "ADS_006, ADS_006, VALIDATION"
    })
    void rechazo_de_admision_se_traduce_al_codigo_publico(final String codigoDb, final AdmisionEstudianteErrorCode esperado, final String tipo) {
        assertEquals(esperado, DbFailureClassifier.classify("mensaje", "DBCODE=" + codigoDb + "|detalle", "admitirEstudiante"));

        final ApplicationException exception = assertThrows(ApplicationException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=" + codigoDb + "|detalle", "corr", "admitirEstudiante"));

        assertEquals(esperado.code(), exception.getCode());
        switch (tipo) {
            case "CONFLICT" -> assertInstanceOf(ConflictException.class, exception);
            case "FORBIDDEN" -> assertInstanceOf(ForbiddenException.class, exception);
            default -> assertInstanceOf(ValidationException.class, exception);
        }
    }
}
