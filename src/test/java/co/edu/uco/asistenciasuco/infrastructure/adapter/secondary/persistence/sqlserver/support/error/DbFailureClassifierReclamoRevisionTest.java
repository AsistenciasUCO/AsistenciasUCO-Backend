package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
