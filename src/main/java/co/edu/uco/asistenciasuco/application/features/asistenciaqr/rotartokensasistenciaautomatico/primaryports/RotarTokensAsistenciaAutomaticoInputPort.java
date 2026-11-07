package co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotartokensasistenciaautomatico.primaryports;

/**
 * CULM-001 B.2 (contrato 3.5): el job del servidor rota los tokens de asistencia cuyo periodo vencio y empuja el QR nuevo
 * al docente titular por SSE. Devuelve cuantos tokens roto en la pasada.
 */
public interface RotarTokensAsistenciaAutomaticoInputPort {

    int execute();
}
