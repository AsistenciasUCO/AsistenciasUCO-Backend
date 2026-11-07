package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Valores de muestra para vistas previas y correos de prueba de las plantillas. Sin estado ni Spring (AR-10).
 */
final class MuestrasNotificacion {

    private static final String GRUPO = "G01 de Ingeniería de Software";
    private static final String ESTUDIANTE = "Ana María Gómez";
    private static final String FECHA = "2026-11-06";
    private static final String MOTIVO = "Incapacidad médica EPS";
    private static final String PERIODO = "2026-2";

    private static final Map<String, String> VALORES = Map.ofEntries(
            Map.entry("grupo", GRUPO),
            Map.entry("estudiante", ESTUDIANTE),
            Map.entry("fecha", FECHA),
            Map.entry("fechaSesion", FECHA),
            Map.entry("porcentaje", "15.5"),
            Map.entry("umbral", "15"),
            Map.entry("limite", "20"),
            Map.entry("motivo", MOTIVO),
            Map.entry("causal", MOTIVO),
            Map.entry("resultado", "aceptada"),
            Map.entry("estado", "APROBADA"),
            Map.entry("observacion", "Soporte médico verificado."),
            Map.entry("periodo", PERIODO),
            Map.entry("totalEstudiantes", "120"),
            Map.entry("totalAprobados", "108"),
            Map.entry("totalReprobados", "12"),
            Map.entry("nombre", "Administrador Institucional"),
            Map.entry("enlace", "http://localhost:4200/verificar-correo#token=prueba-admin")
    );

    private MuestrasNotificacion() {
    }

    /** Todos los valores de muestra, para el correo de prueba. */
    static Map<String, String> completo() {
        return new LinkedHashMap<>(VALORES);
    }

    /** Valor de muestra de un marcador; los desconocidos se muestran entre corchetes. */
    static String valorPara(final String marcador) {
        return VALORES.getOrDefault(marcador, "[" + marcador + "]");
    }

    /** Subconjunto de muestra cuando el tipo no tiene plantilla por defecto. */
    static Map<String, String> basicoVistaPrevia() {
        final Map<String, String> datos = new LinkedHashMap<>();
        for (final String clave : new String[]{"grupo", "estudiante", "fecha", "porcentaje", "motivo", "estado", "observacion", "periodo"}) {
            datos.put(clave, VALORES.get(clave));
        }
        return datos;
    }
}
