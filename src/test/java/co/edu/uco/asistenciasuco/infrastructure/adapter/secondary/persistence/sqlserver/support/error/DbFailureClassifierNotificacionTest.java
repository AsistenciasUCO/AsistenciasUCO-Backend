package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** NOTIF-002B: clasificacion de los codigos NTF de la administracion de plantillas y cola outbox. */
class DbFailureClassifierNotificacionTest {

    private static final String USER = "Mensaje publico.";

    private static String tecnico(final String codigo) {
        return "DBCODE=" + codigo + "|detalle tecnico mutable";
    }

    @Test
    void marcadores_invalidos_es_validacion() {
        assertEquals(CommonErrorCode.VALIDATION_ERROR, DbFailureClassifier.classify(USER, tecnico("NTF_004"), "guardarPlantillaNotificacion"));
    }

    @Test
    void version_inexistente_es_recurso_no_encontrado() {
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, DbFailureClassifier.classify(USER, tecnico("NTF_005"), "activarVersionPlantilla"));
    }

    @Test
    void estado_no_reintentable_es_conflicto() {
        assertEquals(CommonErrorCode.CONFLICT, DbFailureClassifier.classify(USER, tecnico("NTF_008"), "reintentarNotificacionSaliente"));
    }
}
