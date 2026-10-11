package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface PlanEstudioCommandPort {

    /**
     * Registra o actualiza un plan de estudio. {@code idUsuarioEjecutor} es el coordinador autenticado: el
     * provider DB lo revalida (perfil COORDINADOR y titularidad del programa) y no confia solo en Application.
     */
    void registrarOActualizarPlanEstudio(UUID idPlanEstudio, UUID idPrograma, Integer inp, UUID idUsuarioEjecutor);
}
