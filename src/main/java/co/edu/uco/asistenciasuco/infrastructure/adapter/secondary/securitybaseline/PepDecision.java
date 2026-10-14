package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline;

/** Resultado reducido del contrato HTTP PEP para el filtro servlet. */
public record PepDecision(Outcome outcome, String decisionId) {

    public enum Outcome {
        ALLOW,
        UNAUTHORIZED,
        FORBIDDEN,
        UNAVAILABLE
    }
}
