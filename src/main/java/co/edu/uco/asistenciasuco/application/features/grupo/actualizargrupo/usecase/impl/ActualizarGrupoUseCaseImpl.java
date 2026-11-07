package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.primaryports.AsegurarFestivosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.ActualizarGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.domain.ActualizarGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.entity.ActualizarGrupoResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.mapper.ActualizarGrupoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;

import java.util.Objects;

public final class ActualizarGrupoUseCaseImpl implements ActualizarGrupoUseCase {

    private final GrupoRepositoryPort grupoRepositoryPort;
    private final AsegurarFestivosPeriodoInputPort asegurarFestivos;

    public ActualizarGrupoUseCaseImpl(final GrupoRepositoryPort grupoRepositoryPort,
                                      final AsegurarFestivosPeriodoInputPort asegurarFestivos) {
        this.grupoRepositoryPort = Objects.requireNonNull(grupoRepositoryPort, "El puerto GrupoRepositoryPort es obligatorio.");
        this.asegurarFestivos = Objects.requireNonNull(asegurarFestivos, "AsegurarFestivosPeriodoInputPort es obligatorio.");
    }

    @Override
    public ActualizarGrupoResultadoEntity execute(final ActualizarGrupoDomain domain) {
        // CULM-001 B.1 (§3.2.3): un horario nuevo vuelve a precargar sesiones; antes se aseguran los festivos del periodo.
        if (domain.franjas() != null) {
            asegurarFestivos.execute(AsegurarFestivosPeriodoInputPort.Comando.deGrupo(domain.idGrupo()));
        }
        return ActualizarGrupoRepositoryMapper.toEntity(
                grupoRepositoryPort.actualizarGrupo(ActualizarGrupoRepositoryMapper.toRepositoryDTO(domain))
        );
    }
}
