package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SES-01: la DB repite la ventana de registro (defensa en profundidad bajo bloqueo). Sus rechazos SES_007/008/009
 * se traducen al mismo codigo 409 que la validacion previa de Application, en el lote y en el registro autonomo.
 */
class DbFailureClassifierVentanaAsistenciaTest {

    @ParameterizedTest
    @CsvSource({
            "SES_007, registrarAsistenciasSesion, ERR_SESION_NO_INICIADA",
            "SES_008, registrarAsistenciasSesion, ERR_SESION_CERRADA",
            "SES_009, registrarAsistenciasSesion, ERR_SESION_HORARIO_INDETERMINADO",
            "SES_007, registrarAsistenciaAutonoma, ERR_SESION_NO_INICIADA",
            "SES_008, registrarAsistenciaAutonoma, ERR_SESION_CERRADA",
            "SES_009, registrarAsistenciaAutonoma, ERR_SESION_HORARIO_INDETERMINADO"
    })
    void rechazo_de_ventana_de_la_db_responde_409_con_el_codigo_de_application(
            final String codigoDb, final String operacion, final AsistenciaErrorCode esperado) {
        final ErrorDefinition clasificado = DbFailureClassifier.classify(
                "mensaje", "DBCODE=" + codigoDb + "|detalle", operacion);

        assertEquals(esperado, clasificado);
        assertEquals(ErrorKind.CONFLICT, clasificado.kind());
    }

    @ParameterizedTest
    @CsvSource({
            "SES_007, ERR_SESION_NO_INICIADA",
            "SES_008, ERR_SESION_CERRADA",
            "SES_009, ERR_SESION_HORARIO_INDETERMINADO"
    })
    void el_traductor_lanza_conflicto_con_el_codigo_publico_y_sin_detalle_tecnico(
            final String codigoDb, final String codigoPublico) {
        final ConflictException exception = assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "mensaje", "DBCODE=" + codigoDb + "|SES-01 ventana de registro: detalle", "corr", "registrarAsistenciasSesion"));

        assertEquals(codigoPublico, exception.getCode());
        assertEquals(AsistenciaErrorCode.valueOf(codigoPublico).defaultMessage(), exception.getMessage());
    }
}
