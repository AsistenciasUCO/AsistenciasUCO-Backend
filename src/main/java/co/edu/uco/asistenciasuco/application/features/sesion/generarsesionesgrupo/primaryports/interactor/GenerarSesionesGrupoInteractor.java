package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.GenerarSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto.GeneracionSesionesGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto.GenerarSesionesGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.mapper.GenerarSesionesGrupoMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.GenerarSesionesGrupoUseCase;

import java.util.Objects;

public final class GenerarSesionesGrupoInteractor implements GenerarSesionesGrupoInputPort {

    private final GenerarSesionesGrupoUseCase useCase;

    public GenerarSesionesGrupoInteractor(final GenerarSesionesGrupoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "GenerarSesionesGrupoUseCase es obligatorio.");
    }

    @Override
    public GeneracionSesionesGrupoDTO execute(final GenerarSesionesGrupoDTO dto) {
        final var generacion = useCase.execute(GenerarSesionesGrupoMapper.toDomain(dto));
        return new GeneracionSesionesGrupoDTO(generacion.sesionesCanceladasPorFestivo(), generacion.sesionesFestivoConAsistencia());
    }
}
