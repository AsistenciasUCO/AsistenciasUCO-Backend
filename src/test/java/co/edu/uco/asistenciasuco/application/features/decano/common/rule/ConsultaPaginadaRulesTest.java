package co.edu.uco.asistenciasuco.application.features.decano.common.rule;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Reglas de entrada compartidas por las consultas paginadas del decano y del administrador. */
class ConsultaPaginadaRulesTest {

    @Test
    void pagina_por_defecto_valida_e_invalida() {
        assertEquals(ConsultaPaginadaRules.PAGINA_POR_DEFECTO, ConsultaPaginadaRules.pagina(null));
        assertEquals(4, ConsultaPaginadaRules.pagina(4));
        assertThrows(ValidationException.class, () -> ConsultaPaginadaRules.pagina(0));
    }

    @Test
    void tamano_por_defecto_limites_y_fuera_de_rango() {
        assertEquals(ConsultaPaginadaRules.TAMANO_POR_DEFECTO, ConsultaPaginadaRules.tamano(null));
        assertEquals(1, ConsultaPaginadaRules.tamano(1));
        assertEquals(ConsultaPaginadaRules.TAMANO_MAXIMO, ConsultaPaginadaRules.tamano(ConsultaPaginadaRules.TAMANO_MAXIMO));
        assertThrows(ValidationException.class, () -> ConsultaPaginadaRules.tamano(0));
        assertThrows(ValidationException.class, () -> ConsultaPaginadaRules.tamano(ConsultaPaginadaRules.TAMANO_MAXIMO + 1));
    }

    @Test
    void busqueda_vacia_recortada_y_demasiado_larga() {
        assertNull(ConsultaPaginadaRules.busqueda(null));
        assertNull(ConsultaPaginadaRules.busqueda("   "));
        assertEquals("ana", ConsultaPaginadaRules.busqueda("  ana "));
        final String larga = "x".repeat(ConsultaPaginadaRules.BUSQUEDA_MAXIMA + 1);
        assertThrows(ValidationException.class, () -> ConsultaPaginadaRules.busqueda(larga));
    }
}
