package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.features.decano.exception.CoordinadorErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * HU121/HU122 (CONTRACT HU121-HU122 §3.2): clasificacion de los codigos de DB de la gestion del coordinador.
 * {@code GEN_001} es generico (queda tecnico); solo en estas dos operaciones significa "coordinador no encontrado".
 */
class DbFailureClassifierCoordinadorTest {

    private static final String USER = "Mensaje publico.";

    private static String tecnico(final String codigo) {
        return "DBCODE=" + codigo + "|detalle tecnico mutable";
    }

    @ParameterizedTest
    @ValueSource(strings = {"actualizarCoordinador", "cambiarEstadoCoordinador"})
    void gen_001_en_la_gestion_del_coordinador_es_recurso_no_encontrado(final String operacion) {
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, DbFailureClassifier.classify(USER, tecnico("GEN_001"), operacion));
    }

    @Test
    void gen_001_en_otras_operaciones_sigue_sin_clasificar() {
        assertNotEquals(CommonErrorCode.RESOURCE_NOT_FOUND,
                DbFailureClassifier.classify(USER, tecnico("GEN_001"), "crearCoordinador"));
    }

    // Hallazgo de la validacion en la UI: con CommonErrorCode.CONFLICT el usuario recibia el mensaje generico
    // "La operacion genera un conflicto..." en vez del texto institucional de COO_001.
    @ParameterizedTest
    @ValueSource(strings = {"actualizarCoordinador", "cambiarEstadoCoordinador"})
    void coo_001_programa_activo_es_conflicto_con_su_propio_codigo_de_catalogo(final String operacion) {
        final var definicion = DbFailureClassifier.classify(USER, tecnico("COO_001"), operacion);

        assertEquals(CoordinadorErrorCode.COO_001, definicion);
        assertEquals("COO_001", definicion.code());
        assertEquals(ErrorKind.CONFLICT, definicion.kind());
    }

    @Test
    void val_006_correo_duplicado_al_actualizar_es_conflicto() {
        assertEquals(CommonErrorCode.CONFLICT,
                DbFailureClassifier.classify(USER, tecnico("VAL_006"), "actualizarCoordinador"));
    }

    @Test
    void rbac_y_titularidad_se_clasifican_como_prohibido() {
        assertEquals(SecurityErrorCode.FORBIDDEN, DbFailureClassifier.classify(USER, tecnico("SEC_001"), "cambiarEstadoCoordinador"));
        assertEquals(SecurityErrorCode.FORBIDDEN, DbFailureClassifier.classify(USER, tecnico("SEC_002"), "actualizarCoordinador"));
    }

    @Test
    void gen_002_ejecutor_nulo_es_validacion() {
        assertEquals(CommonErrorCode.VALIDATION_ERROR,
                DbFailureClassifier.classify(USER, tecnico("GEN_002"), "actualizarCoordinador"));
    }

    @Test
    void HU159_correccion_de_identidad_clasifica_motivo_interruptor_y_ambito() {
        assertEquals(CommonErrorCode.VALIDATION_ERROR, DbFailureClassifier.classify(USER, tecnico("IDE_001"), "corregirIdentidadUsuario"));
        assertEquals(CommonErrorCode.CONFLICT, DbFailureClassifier.classify(USER, tecnico("IDE_003"), "actualizarDocentePrograma"));
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, DbFailureClassifier.classify(USER, tecnico("GEN_001"), "corregirIdentidadUsuario"));
    }
}
