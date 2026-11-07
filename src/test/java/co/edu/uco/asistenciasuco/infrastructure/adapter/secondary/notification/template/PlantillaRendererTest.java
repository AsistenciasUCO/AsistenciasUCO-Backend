package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlantillaRendererTest {

    private PlantillaNotificacionRepository repository;
    private PlantillaPorDefectoCatalogo catalogoDefecto;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        repository = mock(PlantillaNotificacionRepository.class);
        catalogoDefecto = new PlantillaPorDefectoCatalogo();
        meterRegistry = new SimpleMeterRegistry();
    }

    private PlantillaRenderer crearRenderer(final String platformUrl) {
        return new PlantillaRenderer(repository, catalogoDefecto, meterRegistry, platformUrl);
    }

    private static PlantillaNotificacion plantillaRetiroDb() {
        return new PlantillaNotificacion(
                1,
                "Retiro del grupo {grupo}",
                "Se registró el retiro de {estudiante} del grupo {grupo}.",
                List.of(
                        new PlantillaNotificacion.Marcador("grupo", true),
                        new PlantillaNotificacion.Marcador("estudiante", true),
                        new PlantillaNotificacion.Marcador("motivo", false)
                )
        );
    }

    @Test
    @DisplayName("BE-R01: sustituye {grupo} y {estudiante} con datos validos; origen=db")
    void BE_R01_sustituye_marcadores_con_datos_validos_origen_db() {
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaRetiroDb()));

        final PlantillaRenderer renderer = crearRenderer("");
        final ContenidoRenderizado res = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupo", "G01 de Ingeniería de Software", "estudiante", "Ana María Gómez"),
                "Asunto guardado",
                "Mensaje guardado"
        );

        assertEquals("db", res.origen());
        assertEquals("Retiro del grupo G01 de Ingeniería de Software", res.asunto());
        assertTrue(res.textoPlano().contains("Se registró el retiro de Ana María Gómez del grupo G01 de Ingeniería de Software."));
        assertTrue(res.html().contains("Se registró el retiro de Ana María Gómez del grupo G01 de Ingeniería de Software."));
        assertEquals(1.0, meterRegistry.counter("notification.template.render.total", "origin", "db").count());
    }

    @Test
    @DisplayName("BE-R02: valor con <script>alert(1)</script>, &, \" y ' -> escapado en HTML y literal en texto plano")
    void BE_R02_escapa_html_y_preserva_literal_en_texto_plano() {
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaRetiroDb()));

        final PlantillaRenderer renderer = crearRenderer("");
        final String hostil = "<script>alert(1)</script> & \"comillas\" 'simple'";
        final ContenidoRenderizado res = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupo", "G01 & G02", "estudiante", hostil),
                "Asunto guardado",
                "Mensaje guardado"
        );

        assertEquals("db", res.origen());
        assertFalse(res.html().contains("<script>"));
        assertTrue(res.html().contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; &quot;comillas&quot; &#39;simple&#39;"));
        assertTrue(res.html().contains("G01 &amp; G02"));
        assertTrue(res.textoPlano().contains(hostil));
        assertFalse(res.textoPlano().contains("&lt;script&gt;"));
    }

    @Test
    @DisplayName("BE-R03: falta un marcador obligatorio en datos para plantilla BD -> baja a plantilla por defecto (origen=default)")
    void BE_R03_falta_marcador_obligatorio_en_bd_baja_a_default() {
        // La plantilla en BD exige un marcador obligatorio adicional 'motivo' que no viene en datos,
        // mientras que la plantilla por defecto solo exige 'grupo' y 'estudiante'.
        final PlantillaNotificacion plantillaBdExigente = new PlantillaNotificacion(
                2,
                "Retiro {grupo}",
                "Retiro de {estudiante} por {motivo}.",
                List.of(
                        new PlantillaNotificacion.Marcador("grupo", true),
                        new PlantillaNotificacion.Marcador("estudiante", true),
                        new PlantillaNotificacion.Marcador("motivo", true)
                )
        );
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaBdExigente));

        final PlantillaRenderer renderer = crearRenderer("");
        final ContenidoRenderizado res = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupo", "G01 de Cálculo", "estudiante", "Carlos Ruiz"),
                "Asunto guardado",
                "Mensaje guardado"
        );

        assertEquals("default", res.origen());
        assertEquals("Retiro del grupo G01 de Cálculo", res.asunto());
        assertTrue(res.textoPlano().contains("Carlos Ruiz"));
        assertEquals(1.0, meterRegistry.counter("notification.template.render.total", "origin", "default").count());
    }

    @Test
    @DisplayName("BE-R04: falta marcador obligatorio en BD y en default -> usa asunto/mensaje guardados (origen=stored)")
    void BE_R04_falta_en_default_tambien_usa_stored() {
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaRetiroDb()));

        final PlantillaRenderer renderer = crearRenderer("");
        final ContenidoRenderizado res = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupoId", "123"), // faltan 'grupo' y 'estudiante'
                "Asunto de respaldo guardado",
                "Mensaje de respaldo con <b>etiqueta</b> guardada"
        );

        assertEquals("stored", res.origen());
        assertEquals("Asunto de respaldo guardado", res.asunto());
        assertTrue(res.textoPlano().contains("Mensaje de respaldo con <b>etiqueta</b> guardada"));
        assertTrue(res.html().contains("Mensaje de respaldo con &lt;b&gt;etiqueta&lt;/b&gt; guardada"));
        assertFalse(res.html().contains("<b>etiqueta</b>"));
        assertEquals(1.0, meterRegistry.counter("notification.template.render.total", "origin", "stored").count());
    }

    @Test
    @DisplayName("BE-R05: marcador no declarado para el tipo ({inventado}) -> el nivel se descarta y baja al respaldo")
    void BE_R05_marcador_no_declarado_descarta_nivel_y_baja() {
        final PlantillaNotificacion plantillaConInventado = new PlantillaNotificacion(
                2,
                "Retiro del grupo {grupo}",
                "Hola {estudiante}, codigo {inventado}.",
                List.of(
                        new PlantillaNotificacion.Marcador("grupo", true),
                        new PlantillaNotificacion.Marcador("estudiante", true)
                )
        );
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaConInventado));

        final PlantillaRenderer renderer = crearRenderer("");
        final ContenidoRenderizado res = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupo", "G01", "estudiante", "Laura", "inventado", "X"),
                "Asunto guardado",
                "Mensaje guardado"
        );

        assertEquals("default", res.origen());
        assertFalse(res.textoPlano().contains("{inventado}"));
        assertTrue(res.textoPlano().contains("Se registró el retiro de Laura del grupo G01."));
    }

    @Test
    @DisplayName("BE-R06: parrafos (linea en blanco) -> <p>; salto simple -> <br>; HTML de plantilla no se interpreta")
    void BE_R06_parrafos_saltos_y_escape_de_plantilla() {
        final PlantillaNotificacion plantillaMultiparrafo = new PlantillaNotificacion(
                1,
                "Aviso {grupo}",
                "Primer parrafo de {estudiante}\nsegunda linea del primer parrafo.\n\nSegundo parrafo con <b>HTML</b> literal.",
                List.of(
                        new PlantillaNotificacion.Marcador("grupo", true),
                        new PlantillaNotificacion.Marcador("estudiante", true)
                )
        );
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaMultiparrafo));

        final PlantillaRenderer renderer = crearRenderer("");
        final ContenidoRenderizado res = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupo", "G01", "estudiante", "Pedro"),
                "Asunto",
                "Mensaje"
        );

        assertEquals("db", res.origen());
        assertTrue(res.html().contains("<p>Primer parrafo de Pedro<br>segunda linea del primer parrafo.</p>"));
        assertTrue(res.html().contains("<p>Segundo parrafo con &lt;b&gt;HTML&lt;/b&gt; literal.</p>"));
        assertFalse(res.html().contains("<b>HTML</b>"));
    }

    @Test
    @DisplayName("BE-R07: asunto con saltos de linea -> reemplazados por espacio y recortado a 200 caracteres")
    void BE_R07_asunto_sin_saltos_de_linea_y_recortado_a_200() {
        final String prefijoLargo = "A".repeat(190);
        final PlantillaNotificacion plantillaAsuntoLargo = new PlantillaNotificacion(
                1,
                prefijoLargo + "\r\nLinea2\n{grupo}",
                "Cuerpo {estudiante} en {grupo}.",
                List.of(
                        new PlantillaNotificacion.Marcador("grupo", true),
                        new PlantillaNotificacion.Marcador("estudiante", true)
                )
        );
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaAsuntoLargo));

        final PlantillaRenderer renderer = crearRenderer("");
        final ContenidoRenderizado res = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupo", "GRUPO_MUY_LARGO_1234567890", "estudiante", "Juan"),
                "Asunto",
                "Mensaje"
        );

        assertFalse(res.asunto().contains("\r"));
        assertFalse(res.asunto().contains("\n"));
        assertEquals(200, res.asunto().length());
    }

    @Test
    @DisplayName("BE-R08: llave '{' sin cierre -> queda literal escapada sin lanzar excepcion")
    void BE_R08_llave_sin_cierre_queda_literal_escapada() {
        final PlantillaNotificacion plantillaLlaveAbierta = new PlantillaNotificacion(
                1,
                "Retiro {grupo} {sin_cerrar",
                "Estudiante {estudiante} en {grupo} con {llave & <tag> sin cierre",
                List.of(
                        new PlantillaNotificacion.Marcador("grupo", true),
                        new PlantillaNotificacion.Marcador("estudiante", true)
                )
        );
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaLlaveAbierta));

        final PlantillaRenderer renderer = crearRenderer("");
        final ContenidoRenderizado res = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupo", "G01", "estudiante", "Ana"),
                "Asunto",
                "Mensaje"
        );

        assertEquals("db", res.origen());
        assertEquals("Retiro G01 {sin_cerrar", res.asunto());
        assertTrue(res.textoPlano().contains("con {llave & <tag> sin cierre"));
        assertTrue(res.html().contains("con {llave &amp; &lt;tag&gt; sin cierre"));
    }

    @Test
    @DisplayName("BE-R09: con platform-url el pie incluye el enlace; sin el, no hay enlace")
    void BE_R09_pie_con_y_sin_platform_url() {
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaRetiroDb()));

        final PlantillaRenderer conUrl = crearRenderer("https://asistencia.uco.edu.co");
        final ContenidoRenderizado resConUrl = conUrl.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupo", "G01", "estudiante", "Ana"),
                "Asunto",
                "Mensaje"
        );
        assertTrue(resConUrl.html().contains("href=\"https://asistencia.uco.edu.co\""));
        assertTrue(resConUrl.textoPlano().contains("https://asistencia.uco.edu.co"));

        final PlantillaRenderer sinUrl = crearRenderer("");
        final ContenidoRenderizado resSinUrl = sinUrl.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupo", "G01", "estudiante", "Ana"),
                "Asunto",
                "Mensaje"
        );
        assertFalse(resSinUrl.html().contains("<a "));
        assertFalse(resSinUrl.html().contains("href="));
    }

    @Test
    @DisplayName("BE-R10: la alternativa de texto plano nunca contiene etiquetas HTML ni entidades escapadas")
    void BE_R10_texto_plano_sin_html_ni_entidades() {
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaRetiroDb()));

        final PlantillaRenderer renderer = crearRenderer("https://asistencia.uco.edu.co");
        final ContenidoRenderizado res = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupo", "G01 & G02", "estudiante", "O'Connor \"Jr\""),
                "Asunto",
                "Mensaje"
        );

        assertFalse(res.textoPlano().contains("<html"));
        assertFalse(res.textoPlano().contains("<p>"));
        assertFalse(res.textoPlano().contains("<br>"));
        assertFalse(res.textoPlano().contains("&amp;"));
        assertFalse(res.textoPlano().contains("&quot;"));
        assertFalse(res.textoPlano().contains("&#39;"));
        assertTrue(res.textoPlano().contains("G01 & G02"));
        assertTrue(res.textoPlano().contains("O'Connor \"Jr\""));
    }

    @Test
    @DisplayName("BE-R11: motivo presente en datos pero no usado por la plantilla semilla -> no aparece en el cuerpo")
    void BE_R11_motivo_no_aparece_en_cuerpo_con_plantilla_semilla() {
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaRetiroDb()));

        final PlantillaRenderer renderer = crearRenderer("");
        final String motivoConfidencial = "Incapacidad medica reservada";
        final ContenidoRenderizado res = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupo", "G01", "estudiante", "Ana", "motivo", motivoConfidencial),
                "Asunto",
                "Mensaje"
        );

        assertEquals("db", res.origen());
        assertFalse(res.textoPlano().contains(motivoConfidencial));
        assertFalse(res.html().contains(motivoConfidencial));
    }

    @Test
    @DisplayName("BE-R12: determinismo - misma entrada produce exactamente la misma salida")
    void BE_R12_determinismo() {
        when(repository.consultarActiva("SOLICITUD_INGRESO_RESUELTA", "es"))
                .thenReturn(Optional.empty());

        final PlantillaRenderer renderer = crearRenderer("https://asistencia.uco.edu.co");
        final Map<String, String> datos = Map.of("grupo", "G02 de Física", "resultado", "aceptada");

        final ContenidoRenderizado r1 = renderer.renderizar("SOLICITUD_INGRESO_RESUELTA", datos, "A", "M");
        final ContenidoRenderizado r2 = renderer.renderizar("SOLICITUD_INGRESO_RESUELTA", datos, "A", "M");

        assertEquals("default", r1.origen());
        assertEquals(r1, r2);

        // Casos borde adicionales: tipo desconocido, datos nulos, catalogo con tipo vacio, validaciones de record
        assertTrue(catalogoDefecto.obtener(null, "es").isEmpty());
        assertTrue(catalogoDefecto.obtener("   ", "es").isEmpty());
        assertTrue(catalogoDefecto.obtener("TIPO_INEXISTENTE", null).isEmpty());
        assertTrue(catalogoDefecto.obtener("SOLICITUD_INGRESO_RETIRADA", "   ").isPresent());

        final ContenidoRenderizado rStored = renderer.renderizar(null, null, null, null);
        assertEquals("stored", rStored.origen());
        assertEquals("", rStored.asunto());

        final PlantillaNotificacion sinLista = new PlantillaNotificacion(1, "A", "C", null);
        assertTrue(sinLista.marcadores().isEmpty());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new PlantillaNotificacion(0, "A", "C", List.of()));
    }

    @Test
    @DisplayName("BE-T03: falla de BD al leer plantilla no propaga; baja al respaldo y cuenta origin correspondiente")
    void BE_T03_falla_de_bd_baja_al_respaldo_sin_propagar() {
        when(repository.consultarActiva("SOLICITUD_INGRESO_REGISTRADA", "es"))
                .thenThrow(new RuntimeException("Simulated DB failure"));

        final PlantillaRenderer renderer = crearRenderer("");
        final ContenidoRenderizado res = renderer.renderizar(
                "SOLICITUD_INGRESO_REGISTRADA",
                Map.of("grupo", "G01 de Redes", "estudiante", "Mateo López"),
                "Asunto guardado",
                "Mensaje guardado"
        );

        assertEquals("default", res.origen());
        assertEquals("Nueva solicitud de ingreso al grupo G01 de Redes", res.asunto());
        assertTrue(res.textoPlano().contains("Mateo López solicitó ingresar al grupo G01 de Redes."));
        assertEquals(1.0, meterRegistry.counter("notification.template.render.total", "origin", "default").count());
    }

    @Test
    @DisplayName("BE-R13: dato con {{pie}} y {{contenido}} literales no es sustituido por el pie ni por el cuerpo del layout")
    void BE_R13_dato_con_marcadores_de_layout_queda_literal() {
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaRetiroDb()));

        final PlantillaRenderer renderer = crearRenderer("https://asistencia.uco.edu.co");
        final ContenidoRenderizado res = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                Map.of("grupo", "G01 {{contenido}}", "estudiante", "Ana {{pie}}"),
                "Asunto",
                "Mensaje"
        );

        assertEquals("db", res.origen());
        assertTrue(res.html().contains("Ana {{pie}}"));
        assertTrue(res.html().contains("G01 {{contenido}}"));
        // El enlace del pie debe aparecer exactamente una vez (en el pie real, no dentro del contenido)
        final String marcaPie = "Ir a la plataforma";
        final int primera = res.html().indexOf(marcaPie);
        final int ultima = res.html().lastIndexOf(marcaPie);
        assertTrue(primera > 0);
        assertEquals(primera, ultima);
    }

    @Test
    @DisplayName("BE-R14: sobrecarga del renderizador con aviso: el aviso aparece como primer parrafo en texto plano y en HTML (escapado); sin aviso la salida es identica a 4 argumentos")
    void BE_R14_sobrecarga_con_aviso_modo_prueba() {
        when(repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es"))
                .thenReturn(Optional.of(plantillaRetiroDb()));

        final PlantillaRenderer renderer = crearRenderer("https://asistencia.uco.edu.co");
        final Map<String, String> datos = Map.of("grupo", "G01", "estudiante", "Ana");

        final ContenidoRenderizado base4Args = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                datos,
                "Asunto",
                "Mensaje"
        );
        final ContenidoRenderizado sinAvisoNull = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                datos,
                "Asunto",
                "Mensaje",
                null
        );
        final ContenidoRenderizado sinAvisoBlanco = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                datos,
                "Asunto",
                "Mensaje",
                "   "
        );

        assertEquals(base4Args, sinAvisoNull);
        assertEquals(base4Args, sinAvisoBlanco);

        final String aviso = "MODO DE PRUEBAS: este correo no se envió a los destinatarios reales <test> & control. Destinatarios originales omitidos: 3.";
        final ContenidoRenderizado conAviso = renderer.renderizar(
                "RETIRO_ESTUDIANTE_GRUPO",
                datos,
                "Asunto",
                "Mensaje",
                aviso
        );

        // En texto plano el aviso es el primer parrafo
        assertTrue(conAviso.textoPlano().startsWith(aviso + "\n\n"));
        assertTrue(conAviso.textoPlano().contains("Se registró el retiro de Ana del grupo G01."));

        // En HTML el aviso aparece escapado como primer <p> antes del cuerpo
        final String avisoHtmlEsperado = "<p>MODO DE PRUEBAS: este correo no se envió a los destinatarios reales &lt;test&gt; &amp; control. Destinatarios originales omitidos: 3.</p>";
        final int posAviso = conAviso.html().indexOf(avisoHtmlEsperado);
        final int posCuerpo = conAviso.html().indexOf("<p>Se registró el retiro de Ana del grupo G01.");
        assertTrue(posAviso > 0);
        assertTrue(posCuerpo > posAviso);

        // Tambien funciona en nivel stored (cuando cae al respaldo de fila)
        final ContenidoRenderizado storedConAviso = renderer.renderizar(
                "TIPO_NO_EXISTE",
                Map.of(),
                "Asunto guardado",
                "Cuerpo guardado",
                "MODO DE PRUEBAS: Destinatarios originales omitidos: 1."
        );
        assertEquals("stored", storedConAviso.origen());
        assertTrue(storedConAviso.textoPlano().startsWith("MODO DE PRUEBAS: Destinatarios originales omitidos: 1.\n\nCuerpo guardado"));
        assertTrue(storedConAviso.html().contains("<p>MODO DE PRUEBAS: Destinatarios originales omitidos: 1.</p><p>Cuerpo guardado</p>"));
    }

    @Test
    @DisplayName("BE-R14b: PlantillaPorDefectoCatalogo maneja propiedades sin asunto/cuerpo, tokens vacios en CSV y fallo de lectura")
    void BE_R14b_catalogo_por_defecto_ramas_borde() {
        final ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            // 1. Recurso con asunto vacio -> Optional.empty()
            Thread.currentThread().setContextClassLoader(new ClassLoader(original) {
                @Override
                public java.io.InputStream getResourceAsStream(final String name) {
                    if ("notification/templates/SIN_ASUNTO.es.properties".equals(name)) {
                        return new java.io.ByteArrayInputStream("asunto = \ncuerpo = Hola".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                    if ("notification/templates/SIN_CUERPO.es.properties".equals(name)) {
                        return new java.io.ByteArrayInputStream("asunto = Aviso\ncuerpo =   ".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                    if ("notification/templates/CSV_HUECOS.es.properties".equals(name)) {
                        return new java.io.ByteArrayInputStream(
                                "asunto = Aviso {grupo}\ncuerpo = Cuerpo {grupo}\nmarcadores.obligatorios = grupo, , estudiante\nmarcadores.opcionales = ,motivo,".getBytes(java.nio.charset.StandardCharsets.UTF_8)
                        );
                    }
                    if ("notification/templates/IO_FALLO.es.properties".equals(name)) {
                        return new java.io.InputStream() {
                            @Override
                            public int read() throws java.io.IOException {
                                throw new java.io.IOException("Simulated stream failure");
                            }
                        };
                    }
                    return super.getResourceAsStream(name);
                }
            });

            final PlantillaPorDefectoCatalogo cat = new PlantillaPorDefectoCatalogo(
                    List.of("SIN_ASUNTO", "SIN_CUERPO", "IO_FALLO", "CSV_HUECOS"));
            assertTrue(cat.obtener("SIN_ASUNTO", "es").isEmpty());
            assertTrue(cat.obtener("SIN_CUERPO", "es").isEmpty());
            assertTrue(cat.obtener("IO_FALLO", "es").isEmpty());

            final Optional<PlantillaNotificacion> conHuecos = cat.obtener("CSV_HUECOS", "es");
            assertTrue(conHuecos.isPresent());
            assertEquals(3, conHuecos.get().marcadores().size());
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
