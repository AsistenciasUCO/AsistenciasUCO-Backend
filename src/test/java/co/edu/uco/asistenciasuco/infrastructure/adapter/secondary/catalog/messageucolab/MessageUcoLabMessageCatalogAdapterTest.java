package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure.AzureAppConfigMessageCatalogAdapter;
import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.api.exception.MessageClientResponseException;
import co.edu.uco.messageucolab.client.api.exception.MessageClientTransportException;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageUcoLabMessageCatalogAdapterTest {

    private MessageReader reader;
    private MessageCatalogPort fallback;
    private MessageUcoLabMessageCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        reader = mock(MessageReader.class);
        fallback = mock(MessageCatalogPort.class);
        adapter = adapter(fallback);
    }

    private MessageUcoLabMessageCatalogAdapter adapter(final MessageCatalogPort fallbackPort) {
        return new MessageUcoLabMessageCatalogAdapter(
                reader,
                new MessageUcoLabCodeMapper("MSG_", Map.of("ERR_CORRELACION_REQUERIDA", "CORRREQ")),
                Duration.ofMillis(300),
                Duration.ZERO,
                fallbackPort);
    }

    private MessageUcoLabMessageCatalogAdapter conCache() {
        return new MessageUcoLabMessageCatalogAdapter(
                reader, new MessageUcoLabCodeMapper("MSG_", Map.of()), Duration.ofMillis(300), Duration.ofMinutes(5), fallback);
    }

    private static MessageDetails details(final String code, final String content) {
        return new MessageDetails(code, "es", null, "Titulo del mensaje", content, null, null, null, null, null);
    }

    @Test
    void entrega_el_contenido_de_messageucolab_con_el_codigo_prefijado() {
        when(reader.getDetails("MSG_VAL001"))
                .thenReturn(Mono.just(details("MSG_VAL001", "El numero de identificacion es obligatorio.")));

        assertEquals(Optional.of("El numero de identificacion es obligatorio."), adapter.findUserMessage("VAL_001"));
        verifyNoInteractions(fallback);
    }

    @Test
    void usa_el_alias_para_codigos_largos() {
        when(reader.getDetails("MSG_CORRREQ")).thenReturn(Mono.just(details("MSG_CORRREQ", "Correlacion requerida.")));

        assertEquals(Optional.of("Correlacion requerida."), adapter.findUserMessage("ERR_CORRELACION_REQUERIDA"));
    }

    @Test
    void reemplaza_marcadores_posicionales_vacios_en_orden() {
        when(reader.getDetails("MSG_GEN003"))
                .thenReturn(Mono.just(details("MSG_GEN003", "Ya existe un {} registrado con este {}.")));

        assertEquals("Ya existe un usuario registrado con este correo.",
                adapter.getUserMessage("GEN_003", "usuario", "correo"));
    }

    @Test
    void reemplaza_marcadores_indexados() {
        when(reader.getDetails("MSG_VAL002"))
                .thenReturn(Mono.just(details("MSG_VAL002", "Debe tener entre {0} y {1} digitos.")));

        assertEquals("Debe tener entre 6 y 12 digitos.", adapter.getUserMessage("VAL_002", 6, 12));
    }

    @Test
    void sin_argumentos_devuelve_la_plantilla_intacta() {
        when(reader.getDetails("MSG_GEN001")).thenReturn(Mono.just(details("MSG_GEN001", "El {} no existe.")));

        assertEquals("El {} no existe.", adapter.getUserMessage("GEN_001"));
    }

    @Test
    void plantilla_indexada_invalida_se_devuelve_sin_formatear() {
        when(reader.getDetails("MSG_GEN009")).thenReturn(Mono.just(details("MSG_GEN009", "Valor {abc} invalido")));

        assertEquals("Valor {abc} invalido", adapter.getUserMessage("GEN_009", "x"));
    }

    @Test
    void arg_con_caracteres_especiales_de_regex_no_rompe_el_reemplazo() {
        when(reader.getDetails("MSG_GEN004")).thenReturn(Mono.just(details("MSG_GEN004", "{} registrado.")));

        assertEquals("$1\\ registrado.", adapter.getUserMessage("GEN_004", "$1\\"));
    }

    @Test
    void mensaje_inexistente_en_messageucolab_degrada_al_fallback() {
        when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.empty());
        when(fallback.findUserMessage("VAL_001")).thenReturn(Optional.of("Texto de Azure"));

        assertEquals(Optional.of("Texto de Azure"), adapter.findUserMessage("VAL_001"));
    }

    @Test
    void contenido_vacio_degrada_al_fallback() {
        when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.just(details("MSG_VAL001", "  ")));
        when(fallback.findUserMessage("VAL_001")).thenReturn(Optional.of("Texto de Azure"));

        assertEquals(Optional.of("Texto de Azure"), adapter.findUserMessage("VAL_001"));
    }

    @Test
    void contenido_nulo_degrada_al_fallback() {
        when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.just(details("MSG_VAL001", null)));
        when(fallback.findUserMessage("VAL_001")).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), adapter.findUserMessage("VAL_001"));
    }

    @Test
    void error_http_del_componente_degrada_al_fallback() {
        when(reader.getDetails("MSG_VAL001"))
                .thenReturn(Mono.error(new MessageClientResponseException(409, new RuntimeException("conflicto"))));
        when(fallback.findUserMessage("VAL_001")).thenReturn(Optional.of("Texto de Azure"));

        assertEquals(Optional.of("Texto de Azure"), adapter.findUserMessage("VAL_001"));
    }

    @Test
    void error_de_transporte_degrada_al_fallback() {
        when(reader.getDetails("MSG_VAL001"))
                .thenReturn(Mono.error(new MessageClientTransportException(new RuntimeException("sin red"))));
        when(fallback.findUserMessage("VAL_001")).thenReturn(Optional.of("Texto de Azure"));

        assertEquals(Optional.of("Texto de Azure"), adapter.findUserMessage("VAL_001"));
    }

    @Test
    void timeout_degrada_al_fallback() {
        when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.never());
        when(fallback.findUserMessage("VAL_001")).thenReturn(Optional.of("Texto de Azure"));

        assertEquals(Optional.of("Texto de Azure"), adapter.findUserMessage("VAL_001"));
    }

    @Test
    void codigo_sin_equivalente_remoto_no_consulta_messageucolab_y_usa_el_fallback() {
        when(fallback.findUserMessage("ERR_TIPO_IDENTIFICACION_NOMBRE_LONGITUD_INVALIDA"))
                .thenReturn(Optional.of("Texto de Azure"));

        assertEquals(Optional.of("Texto de Azure"),
                adapter.findUserMessage("ERR_TIPO_IDENTIFICACION_NOMBRE_LONGITUD_INVALIDA"));
        verifyNoInteractions(reader);
    }

    @Test
    void sin_fallback_la_ausencia_devuelve_vacio_y_getUserMessage_el_codigo_crudo() {
        final MessageUcoLabMessageCatalogAdapter sinFallback = adapter(null);
        when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.empty());

        assertEquals(Optional.empty(), sinFallback.findUserMessage("VAL_001"));
        assertEquals("VAL_001", sinFallback.getUserMessage("VAL_001"));
    }

    @Test
    void codigo_nulo_o_en_blanco_devuelve_vacio_sin_consultar() {
        assertEquals(Optional.empty(), adapter.findUserMessage(null));
        assertEquals(Optional.empty(), adapter.findUserMessage("  "));
        verifyNoInteractions(reader, fallback);
    }

    @Test
    void mensaje_tecnico_se_delega_al_fallback() {
        when(fallback.getTechnicalMessage("VAL_001", "x")).thenReturn("tecnico");

        assertEquals("tecnico", adapter.getTechnicalMessage("VAL_001", "x"));
        verifyNoInteractions(reader);
    }

    @Test
    void mensaje_tecnico_sin_fallback_devuelve_el_codigo() {
        assertEquals("VAL_001", adapter(null).getTechnicalMessage("VAL_001"));
    }

    @Test
    void invalida_el_cache_del_fallback_azure() {
        final AzureAppConfigMessageCatalogAdapter azure = mock(AzureAppConfigMessageCatalogAdapter.class);
        final MessageUcoLabMessageCatalogAdapter conAzure = adapter(azure);

        conAzure.invalidateMessage("VAL_001");
        conAzure.invalidateAll();

        verify(azure).invalidateMessage("VAL_001");
        verify(azure).invalidateAll();
    }

    @Test
    void invalidar_con_fallback_no_azure_o_ausente_no_hace_nada() {
        adapter.invalidateMessage("VAL_001");
        adapter.invalidateAll();
        adapter(null).invalidateAll();

        verify(fallback, never()).findUserMessage(any());
    }

    @Test
    void con_cache_la_segunda_consulta_no_llama_a_messageucolab() {
        when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.just(details("MSG_VAL001", "Texto remoto")));
        final MessageUcoLabMessageCatalogAdapter cached = conCache();

        assertEquals(Optional.of("Texto remoto"), cached.findUserMessage("VAL_001"));
        assertEquals(Optional.of("Texto remoto"), cached.findUserMessage("VAL_001"));

        verify(reader, Mockito.times(1)).getDetails("MSG_VAL001");
    }

    @Test
    void con_cache_una_ausencia_no_se_cachea_y_se_reintenta() {
        when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.empty());
        when(fallback.findUserMessage("VAL_001")).thenReturn(Optional.empty());
        final MessageUcoLabMessageCatalogAdapter cached = conCache();

        cached.findUserMessage("VAL_001");
        cached.findUserMessage("VAL_001");

        verify(reader, Mockito.times(2)).getDetails("MSG_VAL001");
    }

    @Test
    void invalidar_un_mensaje_o_todos_fuerza_nueva_consulta() {
        when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.just(details("MSG_VAL001", "Texto remoto")));
        final MessageUcoLabMessageCatalogAdapter cached = conCache();

        cached.findUserMessage("VAL_001");
        cached.invalidateMessage("VAL_001");
        cached.findUserMessage("VAL_001");
        cached.invalidateAll();
        cached.findUserMessage("VAL_001");
        cached.invalidateMessage(null);

        verify(reader, Mockito.times(3)).getDetails("MSG_VAL001");
    }

    @Test
    void cache_en_cero_consulta_siempre() {
        when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.just(details("MSG_VAL001", "Texto remoto")));

        adapter.findUserMessage("VAL_001");
        adapter.findUserMessage("VAL_001");

        verify(reader, Mockito.times(2)).getDetails("MSG_VAL001");
    }

    @Test
    void exige_dependencias_obligatorias() {
        final MessageUcoLabCodeMapper mapper = new MessageUcoLabCodeMapper("", Map.of());
        final Duration uno = Duration.ofSeconds(1);

        assertThrows(NullPointerException.class, () ->
                new MessageUcoLabMessageCatalogAdapter(null, mapper, uno, Duration.ZERO, null));
        assertThrows(NullPointerException.class, () ->
                new MessageUcoLabMessageCatalogAdapter(reader, null, uno, Duration.ZERO, null));
        assertThrows(NullPointerException.class, () ->
                new MessageUcoLabMessageCatalogAdapter(reader, mapper, null, Duration.ZERO, null));
        assertThrows(NullPointerException.class, () ->
                new MessageUcoLabMessageCatalogAdapter(reader, mapper, uno, null, null));
        Mockito.verifyNoInteractions(reader);
    }
}
