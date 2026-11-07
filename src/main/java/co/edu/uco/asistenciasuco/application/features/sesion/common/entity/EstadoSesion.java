package co.edu.uco.asistenciasuco.application.features.sesion.common.entity;

/**
 * CULM-001 B.1 (§3.2.4): estado de una sesion en todas las respuestas de sesion, derivado de
 * {@code uv_sesion_estado.estaCancelada}.
 */
public enum EstadoSesion {
    PROGRAMADA,
    CANCELADA;

    public static EstadoSesion de(final boolean estaCancelada) {
        return estaCancelada ? CANCELADA : PROGRAMADA;
    }
}
