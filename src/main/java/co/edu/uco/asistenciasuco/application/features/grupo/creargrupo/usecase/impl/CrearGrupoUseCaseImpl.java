package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.primaryports.AsegurarFestivosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.CrearGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.domain.CrearGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.entity.CrearGrupoResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.mapper.CrearGrupoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public final class CrearGrupoUseCaseImpl implements CrearGrupoUseCase {

    private final GrupoRepositoryPort grupoRepositoryPort;
    private final AsegurarFestivosPeriodoInputPort asegurarFestivos;

    public CrearGrupoUseCaseImpl(final GrupoRepositoryPort grupoRepositoryPort,
                                 final AsegurarFestivosPeriodoInputPort asegurarFestivos) {
        this.grupoRepositoryPort = Objects.requireNonNull(grupoRepositoryPort, "El puerto GrupoRepositoryPort es obligatorio.");
        this.asegurarFestivos = Objects.requireNonNull(asegurarFestivos, "AsegurarFestivosPeriodoInputPort es obligatorio.");
    }

    @Override
    public CrearGrupoResultadoEntity execute(final CrearGrupoDomain domain) {
        if (domain.generarSesionesAutomaticas()) {
            throw new FeatureUnavailableException(
                    "La generacion automatica de sesiones al crear el grupo aun no esta disponible (fase F2: precarga de sesiones)."
            );
        }
        // CULM-001 B.1 (§3.2.3): con horario, la creacion precarga sesiones y la DB las cancela si caen en un festivo;
        // antes de abrir esa transaccion se aseguran los festivos de los anios del periodo (nunca bloquea la creacion).
        if (domain.franjas() != null) {
            asegurarFestivos.execute(AsegurarFestivosPeriodoInputPort.Comando.dePeriodo(domain.idPeriodoAcademico()));
        }
        final UUID idGrupo = UUID.randomUUID();
        final var projection = grupoRepositoryPort.crearGrupo(CrearGrupoRepositoryMapper.toRepositoryDTO(domain, idGrupo));
        return CrearGrupoRepositoryMapper.toEntity(projection);
    }
}
