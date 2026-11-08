package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.ConsultarSesionesMateriaEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.mapper.ConsultarSesionesMateriaEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.ConsultarSesionesMateriaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionMateriaEstudianteDTO;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarSesionesMateriaEstudianteInteractor implements ConsultarSesionesMateriaEstudianteInputPort {
    private final ConsultarSesionesMateriaEstudianteUseCase useCase;
    public ConsultarSesionesMateriaEstudianteInteractor(final ConsultarSesionesMateriaEstudianteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase);
    }
    @Override public List<SesionMateriaEstudianteDTO> execute(final UUID actorUsuarioId, final UUID idAsignatura,
                                                              final Integer pagina, final Integer tamano) {
        return useCase.execute(actorUsuarioId, idAsignatura, pagina, tamano).stream()
                .map(ConsultarSesionesMateriaEstudianteMapper::toDTO)
                .toList();
    }
}
