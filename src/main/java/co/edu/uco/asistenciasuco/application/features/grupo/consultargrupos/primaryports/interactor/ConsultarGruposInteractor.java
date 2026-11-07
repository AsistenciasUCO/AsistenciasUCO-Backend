package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.interactor;

import java.util.UUID;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.ConsultarGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.mapper.ConsultarGruposMapper;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.ConsultarGruposUseCase;

import java.util.List;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para consultar grupos.
 */
public final class ConsultarGruposInteractor implements ConsultarGruposInputPort {

    private final ConsultarGruposUseCase useCase;

    public ConsultarGruposInteractor(final ConsultarGruposUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ConsultarGruposUseCase es obligatorio.");
    }

    @Override
    public List<GrupoDTO> execute(final UUID usuarioEjecutor) {
        return ConsultarGruposMapper.toDTOs(useCase.execute(usuarioEjecutor));
    }
}