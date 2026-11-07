package co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.usecase.BuscarEstudiantePorDocumentoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.usecase.domain.BuscarEstudiantePorDocumentoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteBusquedaRepositoryProjection;

import java.util.Objects;
import java.util.Optional;

/**
 * CULM-001 A.2-15 (DEC-027, cierra F-DOC-013): el docente titular busca por documento exacto al estudiante que va a
 * matricular, sin necesitar el directorio institucional. La titularidad (SEC_002, 403), la fila Estudiante y la misma
 * institucion (o sin vinculos) se validan dentro del SP; aqui solo se exigen los datos de entrada.
 */
public final class BuscarEstudiantePorDocumentoUseCaseImpl implements BuscarEstudiantePorDocumentoUseCase {

    private final GrupoRepositoryPort grupoRepositoryPort;

    public BuscarEstudiantePorDocumentoUseCaseImpl(final GrupoRepositoryPort grupoRepositoryPort) {
        this.grupoRepositoryPort = Objects.requireNonNull(grupoRepositoryPort, "GrupoRepositoryPort es obligatorio.");
    }

    @Override
    public Optional<EstudianteBusquedaRepositoryProjection> execute(final BuscarEstudiantePorDocumentoDomain domain) {
        if (domain == null || domain.grupoId() == null) {
            throw new ValidationException("El grupo es obligatorio.");
        }
        if (domain.tipoIdentificacionId() == null) {
            throw new ValidationException("El tipo de identificacion es obligatorio.");
        }
        if (domain.numeroIdentificacion() == null || domain.numeroIdentificacion() <= 0) {
            throw new ValidationException("El numero de documento debe ser un entero positivo.");
        }
        if (domain.actorUsuarioId() == null) {
            throw new ValidationException("El usuario autenticado es obligatorio.");
        }
        return grupoRepositoryPort.buscarEstudiantePorDocumento(
                domain.grupoId(), domain.tipoIdentificacionId(), domain.numeroIdentificacion(), domain.actorUsuarioId());
    }
}
