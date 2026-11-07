package co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.BuscarEstudiantePorDocumentoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.dto.BuscarEstudiantePorDocumentoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.dto.EstudianteBusquedaDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.usecase.BuscarEstudiantePorDocumentoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.usecase.domain.BuscarEstudiantePorDocumentoDomain;

import java.util.Objects;
import java.util.Optional;

public final class BuscarEstudiantePorDocumentoInteractor implements BuscarEstudiantePorDocumentoInputPort {

    private final BuscarEstudiantePorDocumentoUseCase useCase;

    public BuscarEstudiantePorDocumentoInteractor(final BuscarEstudiantePorDocumentoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "BuscarEstudiantePorDocumentoUseCase es obligatorio.");
    }

    @Override
    public Optional<EstudianteBusquedaDTO> execute(final BuscarEstudiantePorDocumentoDTO dto) {
        if (dto == null) {
            throw new ValidationException("Los datos de la busqueda son obligatorios.");
        }
        return useCase.execute(new BuscarEstudiantePorDocumentoDomain(
                        dto.grupoId(), dto.tipoIdentificacionId(), dto.numeroIdentificacion(), dto.actorUsuarioId()))
                .map(e -> new EstudianteBusquedaDTO(e.idEstudiante(), e.primerNombre(), e.segundoNombre(), e.primerApellido(),
                        e.segundoApellido(), e.nombreCompleto(), e.numeroIdentificacion(), e.correoEnmascarado(),
                        e.codigoEstadoMatriculaGrupo()));
    }
}
