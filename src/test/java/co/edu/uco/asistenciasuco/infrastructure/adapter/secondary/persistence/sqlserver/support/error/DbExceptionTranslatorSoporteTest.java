package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** LB-004D: codigos ARC_* del soporte de revision. ARC_001 es 404 identico para todos los casos no disponibles. */
class DbExceptionTranslatorSoporteTest {

    private static void fallo(final String dbCode) {
        DbExceptionTranslator.throwIfFailed(false, "mensaje", "DBCODE=" + dbCode + "|detalle", "corr", "solicitarRevisionAsistencia");
    }

    @Test
    void arc_001_es_recurso_no_encontrado() {
        assertThrows(ResourceNotFoundException.class, () -> fallo("ARC_001"));
    }

    @Test
    void arc_002_es_conflicto() {
        assertThrows(ConflictException.class, () -> fallo("ARC_002"));
    }

    @Test
    void arc_003_es_validacion() {
        assertThrows(ValidationException.class, () -> fallo("ARC_003"));
    }

    @Test
    void arc_005_es_forbidden() {
        assertThrows(ForbiddenException.class, () -> fallo("ARC_005"));
    }
}
