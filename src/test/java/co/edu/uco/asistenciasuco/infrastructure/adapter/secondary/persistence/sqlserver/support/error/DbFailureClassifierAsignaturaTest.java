package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Decisiones de John (2026-11-04): ASI_001 (creditos obligatorios y mayores que cero) es 400; GEN_001 al guardar una
 * asignatura (area o componente inexistente) o un tipo de documento (HU153) es 404.
 */
class DbFailureClassifierAsignaturaTest {

    @Test
    void asi_001_es_error_de_validacion() {
        assertEquals(CommonErrorCode.VALIDATION_ERROR,
                DbFailureClassifier.classify("mensaje", "DBCODE=ASI_001|detalle", "crearAsignatura"));
    }

    @Test
    void gen_001_al_guardar_asignaturas_y_tipos_de_documento_es_no_encontrado() {
        for (final String operacion : new String[]{"crearAsignatura", "actualizarAsignatura", "guardarTipoIdentificacion"}) {
            assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND,
                    DbFailureClassifier.classify("mensaje", "DBCODE=GEN_001|detalle", operacion));
        }
    }
}
