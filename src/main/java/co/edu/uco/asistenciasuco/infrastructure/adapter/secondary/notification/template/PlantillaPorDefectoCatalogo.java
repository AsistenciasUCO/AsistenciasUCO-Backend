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
 * {@link #TIPOS_CONOCIDOS} es tambien la lista de tipos entregables: un tipo retirado (CULM-001 A.1: SOLICITUD_INGRESO_*)
 * que el administrador reencole se marca como fallo definitivo en el canal.
 */
public class PlantillaPorDefectoCatalogo {

    private static final String RESOURCE_PATTERN = "notification/templates/%s.%s.properties";
    public static final List<String> TIPOS_CONOCIDOS = List.of(
            Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO,
            Notificacion.TIPO_VERIFICACION_CORREO,
            Notificacion.TIPO_RECLAMO_ASISTENCIA_RADICADO,
            Notificacion.TIPO_RECLAMO_ASISTENCIA_RESUELTO,
            Notificacion.TIPO_ALERTA_RIESGO_INASISTENCIA,
            Notificacion.TIPO_ALERTA_PERDIDA_INASISTENCIA,
            Notificacion.TIPO_CIERRE_MASIVO_PERIODO_EJECUTADO
    );

    private static final String IDIOMA_SOPORTADO = "es";

    private final List<String> tiposPermitidos;

    public PlantillaPorDefectoCatalogo() {
        this(TIPOS_CONOCIDOS);
    }

    /** Solo para pruebas: restringe la lista blanca de tipos cuyos recursos pueden cargarse. */
    PlantillaPorDefectoCatalogo(final List<String> tiposPermitidos) {
        this.tiposPermitidos = List.copyOf(tiposPermitidos);
    }

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
        // Lista blanca: la ruta del recurso se arma solo con constantes conocidas, nunca con la entrada.
        final String tipoConocido = tiposPermitidos.stream().filter(conocido -> conocido.equals(tipo)).findFirst().orElse(null);
        if (tipoConocido == null || !IDIOMA_SOPORTADO.equals(idioma)) {
            return Optional.empty();
        }
        final String path = String.format(RESOURCE_PATTERN, tipoConocido, IDIOMA_SOPORTADO);
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
