package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** HU053-HU055: codigos SIG_* de las solicitudes de ingreso y su clasificacion HTTP (CLS-01..CLS-04). */
class DbFailureClassifierSolicitudIngresoTest {

    private static ErrorDefinition clasificar(final String codigo) {
        return DbFailureClassifier.classify("mensaje", "DBCODE=" + codigo + "|detalle", "resolverSolicitudIngresoGrupo");
    }

    @Test
    void CLS01_no_estudiante_es_403() {
        assertEquals(SecurityErrorCode.FORBIDDEN, clasificar("SIG_001"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIG_002", "SIG_007"})
    void CLS02_grupo_o_solicitud_inexistentes_son_404(final String codigo) {
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, clasificar(codigo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIG_003", "SIG_004", "SIG_005", "SIG_008", "SIG_013"})
    void CLS03_reglas_de_negocio_son_409(final String codigo) {
        assertEquals(CommonErrorCode.CONFLICT, clasificar(codigo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIG_009", "SIG_010"})
    void CLS04_accion_o_motivo_invalidos_son_400(final String codigo) {
        assertEquals(CommonErrorCode.VALIDATION_ERROR, clasificar(codigo));
    }

    @Test
    void clasifica_todos_los_codigos_mqr_de_matricula_por_qr() {
        assertEquals(co.edu.uco.asistenciasuco.application.features.matriculaqr.exception.MatriculaQrErrorCode.MQR_001, clasificar("MQR_001"));
        assertEquals(co.edu.uco.asistenciasuco.application.features.matriculaqr.exception.MatriculaQrErrorCode.MQR_004, clasificar("MQR_004"));
        assertEquals(co.edu.uco.asistenciasuco.application.features.matriculaqr.exception.MatriculaQrErrorCode.MQR_005, clasificar("MQR_005"));
        assertEquals(co.edu.uco.asistenciasuco.application.features.matriculaqr.exception.MatriculaQrErrorCode.MQR_006, clasificar("MQR_006"));
        assertEquals(co.edu.uco.asistenciasuco.application.features.matriculaqr.exception.MatriculaQrErrorCode.MQR_007, clasificar("MQR_007"));
    }
}
