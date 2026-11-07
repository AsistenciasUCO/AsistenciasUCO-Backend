package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.mapper.RegistrarEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.RegistrarEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteDetalleRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteResumenRepositoryProjection;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para registrar estudiante en grupo.
 */
public final class RegistrarEstudianteInteractor implements RegistrarEstudianteInputPort {

    private final RegistrarEstudianteUseCase useCase;
    private final EstudianteRepositoryPort estudianteRepositoryPort;

    public RegistrarEstudianteInteractor(final RegistrarEstudianteUseCase useCase) {
        this(useCase, null);
    }

    public RegistrarEstudianteInteractor(
            final RegistrarEstudianteUseCase useCase,
            final EstudianteRepositoryPort estudianteRepositoryPort
    ) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso RegistrarEstudianteUseCase es obligatorio.");
        this.estudianteRepositoryPort = estudianteRepositoryPort;
    }

    @Override
    public RegistrarEstudianteResultadoDTO execute(final RegistrarEstudianteDTO dto) {
        Objects.requireNonNull(dto, "El DTO para registrar estudiante en grupo es obligatorio.");
        resolverEstudianteExistenteSiAplica(dto);
        final RegistrarEstudianteDomain domain = RegistrarEstudianteMapper.toDomain(dto);
        return RegistrarEstudianteMapper.toDTO(useCase.execute(domain));
    }

    private void resolverEstudianteExistenteSiAplica(final RegistrarEstudianteDTO dto) {
        if (dto.getEstudianteId() != null && estudianteRepositoryPort != null) {
            final EstudianteDetalleRepositoryProjection estudiante = estudianteRepositoryPort
                    .consultarEstudiantePorId(dto.getEstudianteId(), dto.getUsuarioEjecutor())
                    .orElseThrow(() -> new ResourceNotFoundException("El estudiante especificado no se encuentra registrado."));
            final EstudianteResumenRepositoryProjection datos = estudiante.datosPersonales();
            dto.setTipoIdentificacionId(datos.tipoIdentificacionId());
            dto.setNumeroIdentificacion(datos.numeroIdentificacion());
            dto.setPrimerApellido(datos.primerApellido());
            dto.setSegundoApellido(datos.segundoApellido());
            dto.setPrimerNombre(datos.primerNombre());
            dto.setSegundoNombre(datos.segundoNombre());
            dto.setCorreo(datos.correo());
            if (dto.getPassword() == null || dto.getPassword().isBlank()) {
                dto.setPassword("Temporal2026*");
            }
        }
        // CULM-001 A.2-15 (REVIEW-3 B1): ya no se resuelve el estudiante por un documento libre con
        // consultarUsuarioPorIdentificacion (no exigia fila Estudiante ni la misma institucion). Sin correo, el caso de
        // uso lo resuelve con usp_buscar_estudiante_por_documento_grupo.
    }
}