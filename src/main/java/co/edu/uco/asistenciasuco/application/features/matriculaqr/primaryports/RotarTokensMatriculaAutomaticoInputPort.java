package co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports;

/**
 * HU176B (D2): lo invoca el job del servidor. Rota las ventanas vencidas y publica el QR nuevo al docente titular por SSE.
 * Devuelve cuantas ventanas rotaron.
 */
public interface RotarTokensMatriculaAutomaticoInputPort {

    int execute();
}
