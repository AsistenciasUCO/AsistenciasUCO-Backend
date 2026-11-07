package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.calendar;

import co.edu.uco.asistenciasuco.application.secondaryports.calendar.CalendarioFestivosPort.CalendarioFestivosException;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CULM-001 B.1 (CONTRACT 3.4.1 y 3.4.2): proveedores HTTP de festivos con un {@link HttpClient} simulado: 200, error HTTP,
 * timeout, JSON mal formado, fechas de otro anio, cabecera Bearer de feriados.io y su ausencia sin API key. La key nunca
 * aparece en los mensajes de error.
 */
class FestivosHttpAdaptersTest {

    private static final String JSON_CALENDARIOS = """
            {"year":2026,"holidays":[
              {"date":"2026-01-01","name":"Año Nuevo","scope":"nacional","rule":"fixed"},
              {"date":"2026-07-13","movedFrom":"2026-07-09","name":" Nuestra Señora del Rosario de Chiquinquirá "},
              {"date":"2027-01-01","name":"Otro anio"}
            ]}""";

    private final HttpClient http = mock(HttpClient.class);
    private final ParameterCatalogPort catalogo = mock(ParameterCatalogPort.class);
    private final SecretVaultPort vault = mock(SecretVaultPort.class);
    @SuppressWarnings("unchecked")
    private final HttpResponse<String> respuesta = mock(HttpResponse.class);

    @BeforeEach
    void preparar() throws Exception {
        when(catalogo.getParameter(anyString(), anyString())).thenReturn(Optional.empty());
        when(http.send(any(HttpRequest.class), any())).thenReturn((HttpResponse) respuesta);
    }

    @Test
    void calendarios_nacionales_lee_holidays_y_descarta_fechas_de_otro_anio() throws Exception {
        when(respuesta.statusCode()).thenReturn(200);
        when(respuesta.body()).thenReturn(JSON_CALENDARIOS);
        final CalendariosNacionalesFestivosAdapter adapter = new CalendariosNacionalesFestivosAdapter(http, catalogo);

        final List<FestivoColombia> festivos = adapter.obtenerFestivos(2026);

        assertEquals("CALENDARIOS_NACIONALES", adapter.fuente());
        assertTrue(adapter.disponible());
        assertEquals(List.of(new FestivoColombia(LocalDate.of(2026, 1, 1), "Año Nuevo"),
                new FestivoColombia(LocalDate.of(2026, 7, 13), "Nuestra Señora del Rosario de Chiquinquirá")), festivos);
        final ArgumentCaptor<HttpRequest> solicitud = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(solicitud.capture(), any());
        assertEquals("https://calendariosnacionales.com/co/v1/2026/nacionales.json", solicitud.getValue().uri().toString());
        assertEquals(Optional.of(Duration.ofSeconds(10)), solicitud.getValue().timeout());
        assertFalse(solicitud.getValue().headers().firstValue("Authorization").isPresent());
    }

    @Test
    void la_url_y_el_timeout_salen_de_los_parametros() throws Exception {
        when(catalogo.getParameter("FESTIVOS", "URL_CALENDARIOS_NACIONALES"))
                .thenReturn(Optional.of("https://espejo.example/{anio}.json"));
        when(catalogo.getParameter("FESTIVOS", "TIMEOUT_SEGUNDOS")).thenReturn(Optional.of("3"));
        when(respuesta.statusCode()).thenReturn(200);
        when(respuesta.body()).thenReturn("{\"holidays\":[]}");

        assertTrue(new CalendariosNacionalesFestivosAdapter(http, catalogo).obtenerFestivos(2027).isEmpty());

        final ArgumentCaptor<HttpRequest> solicitud = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(solicitud.capture(), any());
        assertEquals("https://espejo.example/2027.json", solicitud.getValue().uri().toString());
        assertEquals(Optional.of(Duration.ofSeconds(3)), solicitud.getValue().timeout());
    }

    @Test
    void un_timeout_invalido_usa_el_valor_por_defecto() throws Exception {
        for (final String valor : new String[]{"abc", "0", "-5"}) {
            when(catalogo.getParameter("FESTIVOS", "TIMEOUT_SEGUNDOS")).thenReturn(Optional.of(valor));
            assertEquals(Duration.ofSeconds(10), new FestivosHttpSupport(http, catalogo).timeout());
        }
        when(catalogo.getParameter("FESTIVOS", "TIMEOUT_SEGUNDOS")).thenThrow(new IllegalStateException("catalogo"));
        assertEquals(Duration.ofSeconds(10), new FestivosHttpSupport(http, catalogo).timeout());
        when(catalogo.getParameter("FESTIVOS", "OTRO")).thenReturn(null);
        assertEquals("d", new FestivosHttpSupport(http, catalogo).parametro("OTRO", "d"));
    }

    @Test
    void errores_http_de_red_timeout_y_json_mal_formado_son_fallos_del_proveedor() throws Exception {
        final CalendariosNacionalesFestivosAdapter adapter = new CalendariosNacionalesFestivosAdapter(http, catalogo);
        when(respuesta.statusCode()).thenReturn(500);
        assertThrows(CalendarioFestivosException.class, () -> adapter.obtenerFestivos(2026));

        when(respuesta.statusCode()).thenReturn(200);
        when(respuesta.body()).thenReturn("{no es json");
        assertThrows(CalendarioFestivosException.class, () -> adapter.obtenerFestivos(2026));
        when(respuesta.body()).thenReturn(null);
        assertThrows(CalendarioFestivosException.class, () -> adapter.obtenerFestivos(2026));
        when(respuesta.body()).thenReturn("null");
        assertThrows(CalendarioFestivosException.class, () -> adapter.obtenerFestivos(2026));
        when(respuesta.body()).thenReturn("{\"year\":2026}");
        assertThrows(CalendarioFestivosException.class, () -> adapter.obtenerFestivos(2026));
        when(respuesta.body()).thenReturn("{\"holidays\":[{\"date\":\"2026-01-01\"}]}");
        assertThrows(CalendarioFestivosException.class, () -> adapter.obtenerFestivos(2026));
        when(respuesta.body()).thenReturn("{\"holidays\":[{\"date\":\"2026-01-01\",\"name\":\" \"}]}");
        assertThrows(CalendarioFestivosException.class, () -> adapter.obtenerFestivos(2026));
        when(respuesta.body()).thenReturn("{\"holidays\":[{\"date\":\"01/01/2026\",\"name\":\"x\"}]}");
        assertThrows(CalendarioFestivosException.class, () -> adapter.obtenerFestivos(2026));
        when(respuesta.body()).thenReturn("{\"holidays\":[{\"date\":null,\"name\":\"x\"}]}");
        assertThrows(CalendarioFestivosException.class, () -> adapter.obtenerFestivos(2026));

        when(http.send(any(HttpRequest.class), any())).thenThrow(new HttpTimeoutException("timeout"));
        assertThrows(CalendarioFestivosException.class, () -> adapter.obtenerFestivos(2026));
        when(http.send(any(HttpRequest.class), any())).thenThrow(new IOException("red"));
        assertThrows(CalendarioFestivosException.class, () -> adapter.obtenerFestivos(2026));
        when(http.send(any(HttpRequest.class), any())).thenReturn(null);
        assertThrows(CalendarioFestivosException.class, () -> adapter.obtenerFestivos(2026));
    }

    @Test
    void una_interrupcion_restaura_la_bandera_del_hilo() throws Exception {
        when(http.send(any(HttpRequest.class), any())).thenThrow(new InterruptedException("stop"));
        assertThrows(CalendarioFestivosException.class,
                () -> new CalendariosNacionalesFestivosAdapter(http, catalogo).obtenerFestivos(2026));
        assertTrue(Thread.interrupted());
    }

    @Test
    void una_url_invalida_es_un_fallo_del_proveedor() {
        when(catalogo.getParameter("FESTIVOS", "URL_CALENDARIOS_NACIONALES")).thenReturn(Optional.of("ht tp://mal {anio}"));
        assertThrows(CalendarioFestivosException.class,
                () -> new CalendariosNacionalesFestivosAdapter(http, catalogo).obtenerFestivos(2026));
    }

    @Test
    void feriados_io_envia_el_bearer_del_baul_y_lee_un_arreglo_raiz_o_data() throws Exception {
        when(vault.getSecret("feriados-io-api-key")).thenReturn(Optional.of("clave-secreta"));
        when(respuesta.statusCode()).thenReturn(200);
        when(respuesta.body()).thenReturn("[{\"date\":\"2026-01-01T00:00:00\",\"name\":\"Año Nuevo\"}]");
        final FeriadosIoFestivosAdapter adapter = new FeriadosIoFestivosAdapter(http, catalogo, vault);

        assertTrue(adapter.disponible());
        assertEquals("FERIADOS_IO", adapter.fuente());
        assertEquals(List.of(new FestivoColombia(LocalDate.of(2026, 1, 1), "Año Nuevo")), adapter.obtenerFestivos(2026));
        final ArgumentCaptor<HttpRequest> solicitud = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(solicitud.capture(), any());
        assertEquals("Bearer clave-secreta", solicitud.getValue().headers().firstValue("Authorization").orElseThrow());
        assertEquals("https://api.feriados.io/v1/CO/holidays/2026", solicitud.getValue().uri().toString());

        when(respuesta.body()).thenReturn("{\"data\":[{\"date\":\"2026-05-01\",\"name\":\"Trabajo\"}]}");
        assertEquals(LocalDate.of(2026, 5, 1), adapter.obtenerFestivos(2026).getFirst().fecha());
        when(respuesta.body()).thenReturn("{\"holidays\":[{\"date\":\"2026-05-01\",\"name\":\"Trabajo\"}]}");
        assertEquals(1, adapter.obtenerFestivos(2026).size());
    }

    @Test
    void feriados_io_sin_key_no_esta_disponible_y_nunca_expone_la_key() throws Exception {
        final FeriadosIoFestivosAdapter sinBaul = new FeriadosIoFestivosAdapter(http, catalogo, null);
        assertFalse(sinBaul.disponible());
        assertThrows(CalendarioFestivosException.class, () -> sinBaul.obtenerFestivos(2026));

        when(vault.getSecret(anyString())).thenReturn(Optional.empty());
        assertFalse(new FeriadosIoFestivosAdapter(http, catalogo, vault).disponible());
        when(vault.getSecret(anyString())).thenReturn(Optional.of("  "));
        assertFalse(new FeriadosIoFestivosAdapter(http, catalogo, vault).disponible());
        when(vault.getSecret(anyString())).thenReturn(null);
        assertFalse(new FeriadosIoFestivosAdapter(http, catalogo, vault).disponible());
        when(vault.getSecret(anyString())).thenThrow(new SecretVaultPort.SecretVaultException("baul caido"));
        assertFalse(new FeriadosIoFestivosAdapter(http, catalogo, vault).disponible());
        verify(http, never()).send(any(HttpRequest.class), any());

        org.mockito.Mockito.doReturn(Optional.of("clave-secreta")).when(vault).getSecret("otro-nombre");
        when(catalogo.getParameter("FESTIVOS", "NOMBRE_SECRETO_FERIADOS_IO")).thenReturn(Optional.of("otro-nombre"));
        when(respuesta.statusCode()).thenReturn(401);
        final CalendarioFestivosException error = assertThrows(CalendarioFestivosException.class,
                () -> new FeriadosIoFestivosAdapter(http, catalogo, vault).obtenerFestivos(2026));
        assertFalse(error.getMessage().contains("clave-secreta"));
    }
}
