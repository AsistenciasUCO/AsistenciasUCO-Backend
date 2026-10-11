package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.SesionV2ConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.mapper.SesionProcedenciaMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.ConsultarSesionesPorGrupoV2InputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.dto.ConsultarSesionesPorGrupoV2DTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.ConsultarSesionesPorGrupoV2UseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.domain.ConsultarSesionesPorGrupoV2Domain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para listar sesiones v2 de un grupo.
 */
public final class ConsultarSesionesPorGrupoV2Interactor implements ConsultarSesionesPorGrupoV2InputPort {

    private final ConsultarSesionesPorGrupoV2UseCase useCase;

    public ConsultarSesionesPorGrupoV2Interactor(final ConsultarSesionesPorGrupoV2UseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ConsultarSesionesPorGrupoV2UseCase es obligatorio.");
    }

    @Override
    public List<SesionV2ConsultadaDTO> execute(final ConsultarSesionesPorGrupoV2DTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar sesiones v2 por grupo es obligatorio.");
        }
        return SesionProcedenciaMapper.toDTOs(
                useCase.execute(new ConsultarSesionesPorGrupoV2Domain(dto.grupo(), dto.usuarioEjecutor()))
        );
    }
}
