package co.edu.uco.asistenciasuco.application.features.festivos.common;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** CULM-001 B.1 (DEC-018): valores por defecto iguales a la semilla ante parametro ausente, mal formado o catalogo caido. */
class FestivosParametrosTest {

    private final ParameterCatalogPort catalogo = mock(ParameterCatalogPort.class);
    private final FestivosParametros parametros = new FestivosParametros(catalogo);

    @Test
    void sin_parametros_usa_los_valores_de_la_semilla() {
        when(catalogo.getParameter(anyString(), anyString())).thenReturn(Optional.empty());
        assertTrue(parametros.habilitado());
        assertEquals("CALENDARIOS_NACIONALES",
                parametros.texto(FestivosParametros.PROVEEDOR_PRINCIPAL, FestivosParametros.DEFECTO_PROVEEDOR_PRINCIPAL));
        assertEquals(1, parametros.entero(FestivosParametros.ANIOS_ADELANTE, FestivosParametros.DEFECTO_ANIOS_ADELANTE));
        assertTrue(parametros.anioValido(2000));
        assertTrue(parametros.anioValido(2100));
        assertFalse(parametros.anioValido(1999));
        assertFalse(parametros.anioValido(2101));
    }

    @Test
    void lee_los_valores_del_catalogo_y_descarta_los_vacios_o_mal_formados() {
        when(catalogo.getParameter("FESTIVOS", "HABILITADO")).thenReturn(Optional.of(" false "));
        when(catalogo.getParameter("FESTIVOS", "ATRIBUCION_TEXTO")).thenReturn(Optional.of("   "));
        when(catalogo.getParameter("FESTIVOS", "ANIOS_ADELANTE")).thenReturn(Optional.of(" 2 "));
        when(catalogo.getParameter("FESTIVOS", "ANIO_MINIMO")).thenReturn(Optional.of("abc"));
        when(catalogo.getParameter("FESTIVOS", "ANIO_MAXIMO")).thenReturn(null);
        assertFalse(parametros.habilitado());
        assertEquals("por defecto", parametros.texto(FestivosParametros.ATRIBUCION_TEXTO, "por defecto"));
        assertEquals(2, parametros.entero(FestivosParametros.ANIOS_ADELANTE, 1));
        assertEquals(7, parametros.entero(FestivosParametros.ANIO_MINIMO, 7));
        assertEquals(9, parametros.entero(FestivosParametros.ANIO_MAXIMO, 9));
    }

    @Test
    void un_catalogo_caido_no_detiene_los_festivos() {
        when(catalogo.getParameter(anyString(), anyString())).thenThrow(new ParameterCatalogPort.ParameterCatalogException("caido"));
        assertTrue(parametros.habilitado());
        assertEquals(15, parametros.entero(FestivosParametros.MINIMO_POR_ANIO, FestivosParametros.DEFECTO_MINIMO_POR_ANIO));
    }

    @Test
    void exigir_anio_valido_responde_fes_001() {
        when(catalogo.getParameter(anyString(), anyString())).thenReturn(Optional.empty());
        assertEquals("FES_001", assertThrows(ValidationException.class, () -> parametros.exigirAnioValido(1800)).getCode());
        assertDoesNotThrow(() -> parametros.exigirAnioValido(2026));
        assertThrows(NullPointerException.class, () -> new FestivosParametros(null));
    }
}
