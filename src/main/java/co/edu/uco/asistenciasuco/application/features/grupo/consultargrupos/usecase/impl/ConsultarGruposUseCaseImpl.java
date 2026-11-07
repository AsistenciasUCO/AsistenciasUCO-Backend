package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.impl;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import java.util.UUID;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.ConsultarGruposUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.mapper.ConsultarGruposRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;

import java.util.List;
import java.util.Objects;

/**
 * Implementacion del caso de uso consultar grupos.
 */
public final class ConsultarGruposUseCaseImpl implements ConsultarGruposUseCase {

    private final GrupoRepositoryPort grupoRepositoryPort;

    public ConsultarGruposUseCaseImpl(final GrupoRepositoryPort grupoRepositoryPort) {
        this.grupoRepositoryPort = Objects.requireNonNull(grupoRepositoryPort, "El puerto de salida GrupoRepositoryPort es obligatorio.");
    }

    @Override
    public List<GrupoEntity> execute(final UUID usuarioEjecutor) {
        if (usuarioEjecutor == null) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta de grupos es obligatorio.");
        }
        // CULM-001 A.2-2: el ambito (COORDINADOR su programa, ADMINISTRADOR su institucion) lo aplica el SP.
        return ConsultarGruposRepositoryMapper.toUseCaseEntities(grupoRepositoryPort.consultarGrupos(usuarioEjecutor));
    }
}