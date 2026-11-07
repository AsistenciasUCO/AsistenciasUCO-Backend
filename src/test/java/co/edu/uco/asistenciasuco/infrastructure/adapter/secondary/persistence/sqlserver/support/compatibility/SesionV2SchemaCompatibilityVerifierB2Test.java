package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.compatibility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** CULM-001 B.2 (CONTRACT 3.6): el arranque exige tambien la vista de tomas y el SP de registro por token. */
class SesionV2SchemaCompatibilityVerifierB2Test {

    @Test
    void el_contrato_verificado_incluye_las_tomas_y_el_registro_por_token() {
        assertTrue(SesionV2SchemaCompatibilityVerifier.SQL_CONTRATO_V2.contains("OBJECT_ID('dbo.uv_toma_asistencia', 'V')"));
        assertTrue(SesionV2SchemaCompatibilityVerifier.SQL_CONTRATO_V2.contains("OBJECT_ID('dbo.usp_registrar_asistencia_por_token', 'P')"));
    }

    @Test
    void sin_el_contrato_el_arranque_falla_y_con_el_contrato_pasa() {
        final EntityManager entityManager = mock(EntityManager.class);
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(SesionV2SchemaCompatibilityVerifier.SQL_CONTRATO_V2)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(0, 1);
        final SesionV2SchemaCompatibilityVerifier verifier = new SesionV2SchemaCompatibilityVerifier(entityManager);

        assertThrows(IllegalStateException.class, verifier::verify);
        assertDoesNotThrow(verifier::verify);
    }
}
