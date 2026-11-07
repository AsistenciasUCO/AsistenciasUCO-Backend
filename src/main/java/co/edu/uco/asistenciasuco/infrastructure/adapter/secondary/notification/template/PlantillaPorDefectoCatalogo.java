package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Catalogo en recursos del backend para plantillas por defecto cuando BD no responde o no tiene plantilla.
 */
public class PlantillaPorDefectoCatalogo {

    private static final String RESOURCE_PATTERN = "notification/templates/%s.%s.properties";
    public static final List<String> TIPOS_CONOCIDOS = List.of(
            Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO,
            Notificacion.TIPO_SOLICITUD_INGRESO_REGISTRADA,
            Notificacion.TIPO_SOLICITUD_INGRESO_RESUELTA,
            Notificacion.TIPO_SOLICITUD_INGRESO_RETIRADA,
            Notificacion.TIPO_VERIFICACION_CORREO,
            Notificacion.TIPO_RECLAMO_ASISTENCIA_RADICADO,
            Notificacion.TIPO_RECLAMO_ASISTENCIA_RESUELTO,
            Notificacion.TIPO_ALERTA_RIESGO_INASISTENCIA,
            Notificacion.TIPO_ALERTA_PERDIDA_INASISTENCIA,
            Notificacion.TIPO_CIERRE_MASIVO_PERIODO_EJECUTADO
    );

    private static final java.util.regex.Pattern TIPO_VALIDO = java.util.regex.Pattern.compile("[A-Z0-9_]{3,60}");
    private static final java.util.regex.Pattern IDIOMA_VALIDO = java.util.regex.Pattern.compile("[a-z]{2}");

    private final Map<String, Optional<PlantillaNotificacion>> cache = new ConcurrentHashMap<>();

    public List<String> listarTiposConocidos() {
        return TIPOS_CONOCIDOS;
    }

    public Optional<PlantillaNotificacion> obtener(final String tipo, final String idioma) {
        if (tipo == null || tipo.isBlank()) {
            return Optional.empty();
        }
        final String tipoNorm = tipo.trim();
        final String idiomaNorm = (idioma == null || idioma.isBlank()) ? "es" : idioma.trim();
        final String key = tipoNorm + ":" + idiomaNorm;
        return cache.computeIfAbsent(key, ignored -> cargarRecurso(tipoNorm, idiomaNorm));
    }

    private Optional<PlantillaNotificacion> cargarRecurso(final String tipo, final String idioma) {
        if (!TIPO_VALIDO.matcher(tipo).matches() || !IDIOMA_VALIDO.matcher(idioma).matches()) {
            return Optional.empty();
        }
        final String path = String.format(RESOURCE_PATTERN, tipo, idioma);
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(path)) {
            if (stream == null) {
                return Optional.empty();
            }
            final Properties props = new Properties();
            props.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            final String asunto = props.getProperty("asunto", "").trim();
            final String cuerpo = props.getProperty("cuerpo", "").trim();
            if (asunto.isEmpty() || cuerpo.isEmpty()) {
                return Optional.empty();
            }
            final List<PlantillaNotificacion.Marcador> marcadores = new ArrayList<>();
            agregarMarcadores(marcadores, props.getProperty("marcadores.obligatorios", ""), true);
            agregarMarcadores(marcadores, props.getProperty("marcadores.opcionales", ""), false);
            return Optional.of(new PlantillaNotificacion(1, asunto, cuerpo, marcadores));
        } catch (final IOException _) {
            return Optional.empty();
        }
    }

    private static void agregarMarcadores(
            final List<PlantillaNotificacion.Marcador> destino,
            final String csv,
            final boolean obligatorio
    ) {
        if (csv.isBlank()) {
            return;
        }
        for (final String token : csv.split(",")) {
            final String limpio = token.trim();
            if (!limpio.isEmpty()) {
                destino.add(new PlantillaNotificacion.Marcador(limpio, obligatorio));
            }
        }
    }
}
