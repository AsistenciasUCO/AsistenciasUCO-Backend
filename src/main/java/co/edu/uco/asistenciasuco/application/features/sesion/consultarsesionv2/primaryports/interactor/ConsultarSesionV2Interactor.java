package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.SesionV2ConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.mapper.SesionProcedenciaMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.ConsultarSesionV2InputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.dto.ConsultarSesionV2DTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.ConsultarSesionV2UseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.domain.ConsultarSesionV2Domain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;

/**
 * Interactor del puerto de entrada para consultar una sesion v2.
 */
public final class ConsultarSesionV2Interactor implements ConsultarSesionV2InputPort {

    private final ConsultarSesionV2UseCase useCase;

    public ConsultarSesionV2Interactor(final ConsultarSesionV2UseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ConsultarSesionV2UseCase es obligatorio.");
    }

    @Override
    public SesionV2ConsultadaDTO execute(final ConsultarSesionV2DTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar sesion v2 es obligatorio.");
        }
        return SesionProcedenciaMapper.toDTO(
                useCase.execute(new ConsultarSesionV2Domain(dto.sesion(), dto.usuarioEjecutor()))
        );
    }
}
