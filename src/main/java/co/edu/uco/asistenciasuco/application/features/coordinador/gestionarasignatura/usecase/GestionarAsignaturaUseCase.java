package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.AsignaturaDomain;
import java.util.UUID;

public interface GestionarAsignaturaUseCase {

    void crear(AsignaturaDomain domain);

    void actualizar(AsignaturaDomain domain);

    void toggleEstado(UUID asignaturaId, UUID usuarioEjecutor);
}
