package co.edu.uco.asistenciasuco.application.secondaryports.security;

/**
 * CULM-001 B.2 (3.2.7): limite de intentos por minuto de una clave (estudiante, hash del token o PIN del QR de asistencia).
 * {@code true} registra el intento y lo permite; {@code false} indica que la clave ya alcanzo el limite.
 */
public interface LimitadorIntentosPort {

    boolean permitir(String clave, int limitePorMinuto);
}
