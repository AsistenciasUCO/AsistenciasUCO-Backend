package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase;

import java.util.UUID;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

import java.util.List;

/**
 * Caso de uso para consultar grupos.
 */
public interface ConsultarGruposUseCase extends UseCaseWithReturn<UUID, List<GrupoEntity>> {
}
