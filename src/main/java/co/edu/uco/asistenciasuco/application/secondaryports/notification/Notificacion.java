package co.edu.uco.asistenciasuco.application.secondaryports.notification;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Aviso institucional independiente del canal (correo, WhatsApp, SMS...): el componente de notificaciones decide
 * como entregarlo a cada destinatario. {@code datos} lleva identificadores para trazabilidad, nunca secretos.
 */
public record Notificacion(
        String tipo,
        String asunto,
        String mensaje,
        List<Destinatario> destinatarios,
        Map<String, String> datos
) {

    /** HU051: el docente titular retira a un estudiante del grupo. */
    public static final String TIPO_RETIRO_ESTUDIANTE_GRUPO = "RETIRO_ESTUDIANTE_GRUPO";
    /** HU053: un estudiante solicito el ingreso a un grupo (aviso al docente titular). */
    public static final String TIPO_SOLICITUD_INGRESO_REGISTRADA = "SOLICITUD_INGRESO_REGISTRADA";
    /** HU053/HU054/HU055: el docente acepto, dejo en espera o rechazo la solicitud (aviso al estudiante). */
    public static final String TIPO_SOLICITUD_INGRESO_RESUELTA = "SOLICITUD_INGRESO_RESUELTA";
    /** HU055: el estudiante retiro su solicitud (aviso al docente titular). */
    public static final String TIPO_SOLICITUD_INGRESO_RETIRADA = "SOLICITUD_INGRESO_RETIRADA";
    /** IDENT-001: aviso al estudiante registrado por el docente para verificar su correo (el enlace lo emite el canal al entregar). */
    public static final String TIPO_VERIFICACION_CORREO = "VERIFICACION_CORREO";
    /** HU010/HU035: el estudiante radica una solicitud de revision de asistencia (aviso al docente titular). */
    public static final String TIPO_RECLAMO_ASISTENCIA_RADICADO = "RECLAMO_ASISTENCIA_RADICADO";
    /** HU037/HU042/HU146: el docente resuelve la solicitud de revision (aviso al estudiante). */
    public static final String TIPO_RECLAMO_ASISTENCIA_RESUELTO = "RECLAMO_ASISTENCIA_RESUELTO";
    /** HU012/HU015: alerta de riesgo de inasistencia (regla institucional 20%). */
    public static final String TIPO_ALERTA_RIESGO_INASISTENCIA = "ALERTA_RIESGO_INASISTENCIA";
    /** HU013: alerta de perdida de asignatura por inasistencia. */
    public static final String TIPO_ALERTA_PERDIDA_INASISTENCIA = "ALERTA_PERDIDA_INASISTENCIA";
    /** HU174: el administrador institucional ejecuta el cierre masivo del periodo academico. */
    public static final String TIPO_CIERRE_MASIVO_PERIODO_EJECUTADO = "CIERRE_MASIVO_PERIODO_EJECUTADO";

    public Notificacion {
        Objects.requireNonNull(tipo, "El tipo de notificacion es obligatorio.");
        destinatarios = destinatarios == null ? List.of() : List.copyOf(destinatarios);
        datos = datos == null ? Map.of() : Map.copyOf(datos);
    }

    /** Destinatario con su rol en el evento (ESTUDIANTE, DOCENTE, COORDINADOR...) y sus datos de contacto. */
    public record Destinatario(String rol, UUID idUsuario, String nombre, String correo) {
    }
}
