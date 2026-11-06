package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor determinista de renderizado de plantillas de notificacion sin reflexion ni expresiones ejecutables.
 */
public class PlantillaRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlantillaRenderer.class);
    private static final String DEFAULT_IDIOMA = "es";
    private static final String LAYOUT_RESOURCE = "notification/email/layout.html";
    private static final String METRIC_RENDER_TOTAL = "notification.template.render.total";
    private static final int MAX_SUBJECT_LENGTH = 200;
    private static final String MARCADOR_ENLACE = "enlace";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{([a-zA-Z]\\w*)\\}");

    private final PlantillaNotificacionRepository repository;
    private final PlantillaPorDefectoCatalogo catalogoDefecto;
    private final MeterRegistry meterRegistry;
    private final String platformUrl;
    private final String layoutHtml;

    public PlantillaRenderer(
            final PlantillaNotificacionRepository repository,
            final PlantillaPorDefectoCatalogo catalogoDefecto,
            final MeterRegistry meterRegistry,
            final String platformUrl
    ) {
        this.repository = Objects.requireNonNull(repository, "PlantillaNotificacionRepository es obligatorio.");
        this.catalogoDefecto = Objects.requireNonNull(catalogoDefecto, "PlantillaPorDefectoCatalogo es obligatorio.");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");
        this.platformUrl = platformUrl == null ? "" : platformUrl.trim();
        this.layoutHtml = cargarLayout();
    }

    public ContenidoRenderizado renderizar(
            final String tipo,
            final Map<String, String> datos,
            final String asuntoGuardado,
            final String mensajeGuardado
    ) {
        return renderizar(tipo, datos, asuntoGuardado, mensajeGuardado, null);
    }

    public ContenidoRenderizado renderizar(
            final String tipo,
            final Map<String, String> datos,
            final String asuntoGuardado,
            final String mensajeGuardado,
            final String avisoModoPrueba
    ) {
        final Map<String, String> mapaDatos = datos == null ? Map.of() : datos;
        final String tipoNorm = tipo == null ? "" : tipo.trim();
        final String aviso = (avisoModoPrueba == null || avisoModoPrueba.isBlank()) ? "" : avisoModoPrueba.trim();

        // Nivel 1: Plantilla activa en BD
        final Optional<PlantillaNotificacion> desdeBd = consultarBdSegura(tipoNorm);
        if (desdeBd.isPresent() && esPlantillaValidaParaDatos(desdeBd.get(), mapaDatos)) {
            return construirDesdePlantilla(desdeBd.get(), mapaDatos, "db", aviso);
        }

        // Nivel 2: Plantilla por defecto en recursos del backend
        final Optional<PlantillaNotificacion> porDefecto = catalogoDefecto.obtener(tipoNorm, DEFAULT_IDIOMA);
        if (porDefecto.isPresent() && esPlantillaValidaParaDatos(porDefecto.get(), mapaDatos)) {
            return construirDesdePlantilla(porDefecto.get(), mapaDatos, "default", aviso);
        }

        // Nivel 3: Asunto y mensaje guardados en el outbox
        final String asuntoRaw = asuntoGuardado == null ? "" : asuntoGuardado;
        final String cuerpoRaw = mensajeGuardado == null ? "" : mensajeGuardado;
        final String asuntoSanitizado = sanitizarAsunto(asuntoRaw);
        final String textoPlano = construirTextoPlano(cuerpoRaw, aviso);
        final String html = envolverEnLayout(antecederAvisoHtml(convertirCuerpoLiteralAHtml(cuerpoRaw), aviso));
        incrementarMetrica("stored");
        return new ContenidoRenderizado(asuntoSanitizado, textoPlano, html, "stored");
    }

    public ContenidoRenderizado renderizarExplicito(
            final String asunto,
            final String cuerpo,
            final Map<String, String> datos
    ) {
        final Map<String, String> mapaDatos = datos == null ? Map.of() : datos;
        final String asuntoSanitizado = sanitizarAsunto(sustituirTextoPlano(asunto == null ? "" : asunto, mapaDatos));
        final String cuerpoPlano = sustituirTextoPlano(cuerpo == null ? "" : cuerpo, mapaDatos);
        final String textoPlano = construirTextoPlano(cuerpoPlano, "");
        final String cuerpoHtml = convertirPlantillaAHtml(cuerpo == null ? "" : cuerpo, mapaDatos);
        final String html = envolverEnLayout(cuerpoHtml);
        return new ContenidoRenderizado(asuntoSanitizado, textoPlano, html, "preview");
    }

    private Optional<PlantillaNotificacion> consultarBdSegura(final String tipo) {
        if (tipo.isEmpty()) {
            return Optional.empty();
        }
        try {
            return repository.consultarActiva(tipo, DEFAULT_IDIOMA);
        } catch (final RuntimeException _) {
            LOGGER.warn("No fue posible consultar plantilla activa en BD para tipo={}. Se usara respaldo.", tipo);
            return Optional.empty();
        }
    }

    private boolean esPlantillaValidaParaDatos(
            final PlantillaNotificacion plantilla,
            final Map<String, String> datos
    ) {
        final Set<String> permitidos = new HashSet<>();
        for (final PlantillaNotificacion.Marcador marcador : plantilla.marcadores()) {
            permitidos.add(marcador.nombre());
            if (marcador.obligatorio()) {
                final String valor = datos.get(marcador.nombre());
                if (valor == null || valor.isBlank()) {
                    return false;
                }
            }
        }
        return todosMarcadoresDeclarados(plantilla.asunto(), permitidos)
                && todosMarcadoresDeclarados(plantilla.cuerpo(), permitidos);
    }

    private static boolean todosMarcadoresDeclarados(final String texto, final Set<String> permitidos) {
        final Matcher matcher = TOKEN_PATTERN.matcher(texto);
        while (matcher.find()) {
            final String nombre = matcher.group(1);
            if (!permitidos.contains(nombre)) {
                return false;
            }
        }
        return true;
    }

    private ContenidoRenderizado construirDesdePlantilla(
            final PlantillaNotificacion plantilla,
            final Map<String, String> datos,
            final String origen,
            final String aviso
    ) {
        final String asuntoRenderizado = sanitizarAsunto(sustituirTextoPlano(plantilla.asunto(), datos));
        final String cuerpoPlano = sustituirTextoPlano(plantilla.cuerpo(), datos);
        final String textoPlano = construirTextoPlano(cuerpoPlano, aviso);
        final String cuerpoHtml = antecederAvisoHtml(convertirPlantillaAHtml(plantilla.cuerpo(), datos), aviso);
        final String html = envolverEnLayout(cuerpoHtml);
        incrementarMetrica(origen);
        return new ContenidoRenderizado(asuntoRenderizado, textoPlano, html, origen);
    }

    private static String sustituirTextoPlano(final String plantillaTexto, final Map<String, String> datos) {
        final Matcher matcher = TOKEN_PATTERN.matcher(plantillaTexto);
        final StringBuilder sb = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            sb.append(plantillaTexto, cursor, matcher.start());
            final String nombre = matcher.group(1);
            final String valor = datos.getOrDefault(nombre, "");
            sb.append(valor == null ? "" : valor);
            cursor = matcher.end();
        }
        sb.append(plantillaTexto, cursor, plantillaTexto.length());
        return sb.toString();
    }

    private static String sustituirEscapadoHtml(final String lineaPlantilla, final Map<String, String> datos) {
        final Matcher matcher = TOKEN_PATTERN.matcher(lineaPlantilla);
        final StringBuilder sb = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            sb.append(escaparHtml(lineaPlantilla.substring(cursor, matcher.start())));
            final String nombre = matcher.group(1);
            final String valor = datos.getOrDefault(nombre, "");
            sb.append(renderizarValorHtml(nombre, valor));
            cursor = matcher.end();
        }
        sb.append(escaparHtml(lineaPlantilla.substring(cursor)));
        return sb.toString();
    }

    /**
     * El marcador {@code enlace} (valor emitido por el sistema, nunca por una persona) se muestra como vinculo solo si es
     * una URL http o https; cualquier otro valor, y cualquier otro marcador, se escapa como texto.
     */
    private static String renderizarValorHtml(final String nombre, final String valor) {
        final String texto = valor == null ? "" : valor;
        final String escapado = escaparHtml(texto);
        if (MARCADOR_ENLACE.equals(nombre) && esUrlHttp(texto)) {
            return "<a href=\"" + escapado + "\" style=\"color:#0f5132;\">" + escapado + "</a>";
        }
        return escapado;
    }

    private static boolean esUrlHttp(final String texto) {
        final String minusculas = texto.toLowerCase(java.util.Locale.ROOT);
        return (minusculas.startsWith("https://") || minusculas.startsWith("http://")) && texto.indexOf(' ') < 0;
    }

    private static String convertirPlantillaAHtml(final String cuerpoPlantilla, final Map<String, String> datos) {
        final String normalizado = cuerpoPlantilla.replace("\r\n", "\n").replace("\r", "\n");
        final String[] parrafos = normalizado.split("\n\\s*\n", -1);
        final StringBuilder sb = new StringBuilder();
        for (final String parrafo : parrafos) {
            final String[] lineas = parrafo.split("\n", -1);
            sb.append("<p>");
            for (int i = 0; i < lineas.length; i++) {
                if (i > 0) {
                    sb.append("<br>");
                }
                sb.append(sustituirEscapadoHtml(lineas[i], datos));
            }
            sb.append("</p>");
        }
        return sb.toString();
    }

    private static String convertirCuerpoLiteralAHtml(final String cuerpoLiteral) {
        final String normalizado = cuerpoLiteral.replace("\r\n", "\n").replace("\r", "\n");
        final String[] parrafos = normalizado.split("\n\\s*\n", -1);
        final StringBuilder sb = new StringBuilder();
        for (final String parrafo : parrafos) {
            final String[] lineas = parrafo.split("\n", -1);
            sb.append("<p>");
            for (int i = 0; i < lineas.length; i++) {
                if (i > 0) {
                    sb.append("<br>");
                }
                sb.append(escaparHtml(lineas[i]));
            }
            sb.append("</p>");
        }
        return sb.toString();
    }

    private String construirTextoPlano(final String cuerpoPlano, final String aviso) {
        final String cuerpoConAviso = aviso.isEmpty() ? cuerpoPlano : aviso + "\n\n" + cuerpoPlano;
        if (platformUrl.isEmpty()) {
            return cuerpoConAviso + "\n\n---\nUniversidad Católica de Oriente — Sistema de Gestión de Asistencia";
        }
        return cuerpoConAviso + "\n\n---\nUniversidad Católica de Oriente — Sistema de Gestión de Asistencia\n" + platformUrl;
    }

    private static String antecederAvisoHtml(final String cuerpoHtml, final String aviso) {
        if (aviso.isEmpty()) {
            return cuerpoHtml;
        }
        return "<p>" + escaparHtml(aviso) + "</p>" + cuerpoHtml;
    }

    private String envolverEnLayout(final String contenidoHtml) {
        final String pieHtml;
        if (platformUrl.isEmpty()) {
            pieHtml = "Mensaje automático del Sistema de Gestión de Asistencia — Universidad Católica de Oriente.";
        } else {
            final String urlEscapada = escaparHtml(platformUrl);
            pieHtml = "Mensaje automático del Sistema de Gestión de Asistencia — "
                    + "<a href=\"" + urlEscapada + "\" style=\"color:#0f5132;\">Ir a la plataforma</a>.";
        }
        return layoutHtml
                .replace("{{pie}}", pieHtml)
                .replace("{{contenido}}", contenidoHtml);
    }

    private static String sanitizarAsunto(final String asunto) {
        final String sinSaltos = asunto
                .replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
        if (sinSaltos.length() > MAX_SUBJECT_LENGTH) {
            return sinSaltos.substring(0, MAX_SUBJECT_LENGTH);
        }
        return sinSaltos;
    }

    static String escaparHtml(final String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private void incrementarMetrica(final String origen) {
        meterRegistry.counter(METRIC_RENDER_TOTAL, "origin", origen).increment();
    }

    private static String cargarLayout() {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(LAYOUT_RESOURCE)) {
            if (in == null) {
                return "<!DOCTYPE html><html><body><div>{{contenido}}</div><footer>{{pie}}</footer></body></html>";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException _) {
            return "<!DOCTYPE html><html><body><div>{{contenido}}</div><footer>{{pie}}</footer></body></html>";
        }
    }
}
