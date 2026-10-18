package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto.GuardarAsignaturaDTO;
import java.util.UUID;

public interface GestionarAsignaturaInputPort {

    void crear(GuardarAsignaturaDTO dto);

    void actualizar(GuardarAsignaturaDTO dto);

    void toggleEstado(UUID asignaturaId, UUID usuarioEjecutor);
}
