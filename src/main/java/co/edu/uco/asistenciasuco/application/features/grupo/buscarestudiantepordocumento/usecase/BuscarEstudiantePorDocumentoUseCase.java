package co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.usecase;

import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.usecase.domain.BuscarEstudiantePorDocumentoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteBusquedaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

import java.util.Optional;

public interface BuscarEstudiantePorDocumentoUseCase
        extends UseCaseWithReturn<BuscarEstudiantePorDocumentoDomain, Optional<EstudianteBusquedaRepositoryProjection>> {
}
