package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.calendar;

import co.edu.uco.asistenciasuco.application.secondaryports.calendar.CalendarioFestivosPort.CalendarioFestivosException;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Soporte comun de los proveedores HTTP de festivos: URL y timeout por {@code FESTIVOS/*} (valor por defecto igual a la
 * semilla, DEC-018), un unico {@code GET} sin reintentos propios y la lectura de los campos {@code date}/{@code name}.
 * Ningun encabezado (en particular {@code Authorization}) se registra en logs ni viaja en los mensajes de error.
 */
final class FestivosHttpSupport {

    static final String GRUPO = "FESTIVOS";
    static final String CLAVE_TIMEOUT = "TIMEOUT_SEGUNDOS";
    static final int TIMEOUT_POR_DEFECTO = 10;
    private static final String MARCADOR_ANIO = "{anio}";
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final HttpClient httpClient;
    private final ParameterCatalogPort parametros;

    FestivosHttpSupport(final HttpClient httpClient, final ParameterCatalogPort parametros) {
        this.httpClient = Objects.requireNonNull(httpClient, "El HttpClient de festivos es obligatorio.");
        this.parametros = Objects.requireNonNull(parametros, "ParameterCatalogPort es obligatorio.");
    }

    String parametro(final String clave, final String defecto) {
        try {
            return Objects.requireNonNullElse(parametros.getParameter(GRUPO, clave), Optional.<String>empty())
                    .map(String::strip)
                    .filter(v -> !v.isEmpty())
                    .orElse(defecto);
        } catch (final RuntimeException exception) {
            return defecto;
        }
    }

    Duration timeout() {
        try {
            final int segundos = Integer.parseInt(parametro(CLAVE_TIMEOUT, String.valueOf(TIMEOUT_POR_DEFECTO)));
            return Duration.ofSeconds(segundos > 0 ? segundos : TIMEOUT_POR_DEFECTO);
        } catch (final NumberFormatException exception) {
            return Duration.ofSeconds(TIMEOUT_POR_DEFECTO);
        }
    }

    JsonNode get(final String fuente, final String plantillaUrl, final int anio, final Map<String, String> encabezados) {
        final URI uri;
        try {
            uri = URI.create(plantillaUrl.replace(MARCADOR_ANIO, String.valueOf(anio)));
        } catch (final IllegalArgumentException exception) {
            throw new CalendarioFestivosException("URL de " + fuente + " invalida.", exception);
        }
        final HttpRequest.Builder solicitud = HttpRequest.newBuilder(uri)
                .timeout(timeout())
                .header("Accept", "application/json")
                .GET();
        encabezados.forEach(solicitud::header);
        final HttpResponse<String> respuesta;
        try {
            respuesta = httpClient.send(solicitud.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (final IOException exception) {
            throw new CalendarioFestivosException(fuente + " no respondio: " + exception.getClass().getSimpleName(), exception);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CalendarioFestivosException(fuente + " fue interrumpido.", exception);
        }
        if (respuesta == null || respuesta.statusCode() != 200) {
            throw new CalendarioFestivosException(fuente + " respondio HTTP "
                    + (respuesta == null ? "sin respuesta" : respuesta.statusCode()) + ".");
        }
        try {
            final JsonNode raiz = JSON.readTree(respuesta.body() == null ? "" : respuesta.body());
            if (raiz == null || raiz.isMissingNode() || raiz.isNull()) {
                throw new CalendarioFestivosException(fuente + " entrego un cuerpo vacio.");
            }
            return raiz;
        } catch (final RuntimeException exception) {
            if (exception instanceof CalendarioFestivosException propia) {
                throw propia;
            }
            throw new CalendarioFestivosException(fuente + " entrego un JSON mal formado.", exception);
        }
    }

    /** Lee {@code [{date, name}, ...]} y conserva solo las fechas del anio pedido; cualquier elemento ilegible falla. */
    static List<FestivoColombia> leerFestivos(final String fuente, final JsonNode arreglo, final int anio) {
        if (arreglo == null || !arreglo.isArray()) {
            throw new CalendarioFestivosException(fuente + " no entrego el arreglo de festivos.");
        }
        final List<FestivoColombia> festivos = new ArrayList<>();
        for (final JsonNode elemento : arreglo) {
            final String fecha = texto(elemento, "date");
            final String nombre = texto(elemento, "name");
            if (fecha == null || nombre == null || nombre.isBlank()) {
                throw new CalendarioFestivosException(fuente + " entrego un festivo sin fecha o sin nombre.");
            }
            final LocalDate dia;
            try {
                dia = LocalDate.parse(fecha.length() > 10 ? fecha.substring(0, 10) : fecha);
            } catch (final DateTimeParseException exception) {
                throw new CalendarioFestivosException(fuente + " entrego una fecha invalida.", exception);
            }
            if (dia.getYear() == anio) {
                festivos.add(new FestivoColombia(dia, nombre.strip()));
            }
        }
        return List.copyOf(festivos);
    }

    private static String texto(final JsonNode nodo, final String campo) {
        final JsonNode valor = nodo == null ? null : nodo.get(campo);
        return valor == null || valor.isNull() ? null : valor.asString();
    }
}
