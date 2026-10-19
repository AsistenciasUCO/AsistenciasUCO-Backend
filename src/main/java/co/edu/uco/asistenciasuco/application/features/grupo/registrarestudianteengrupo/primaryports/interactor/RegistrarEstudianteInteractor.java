package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.mapper.RegistrarEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.RegistrarEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteDetalleRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteResumenRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para registrar estudiante en grupo.
 */
public final class RegistrarEstudianteInteractor implements RegistrarEstudianteInputPort {

    private final RegistrarEstudianteUseCase useCase;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final EstudianteRepositoryPort estudianteRepositoryPort;

    public RegistrarEstudianteInteractor(final RegistrarEstudianteUseCase useCase) {
        this(useCase, null, null);
    }

    public RegistrarEstudianteInteractor(
            final RegistrarEstudianteUseCase useCase,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final EstudianteRepositoryPort estudianteRepositoryPort
    ) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso RegistrarEstudianteUseCase es obligatorio.");
        this.usuarioRepositoryPort = usuarioRepositoryPort;
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
                    .consultarEstudiantePorId(dto.getEstudianteId())
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
            return;
        }

        final boolean faltanDatosPersonales = dto.getPrimerNombre() == null
                || dto.getPrimerApellido() == null
                || dto.getCorreo() == null;

        if (faltanDatosPersonales && dto.getTipoIdentificacionId() != null && dto.getNumeroIdentificacion() != null) {
            if (usuarioRepositoryPort != null) {
                final UsuarioIdentidadRepositoryProjection usuario = usuarioRepositoryPort
                        .consultarUsuarioPorIdentificacion(dto.getTipoIdentificacionId(), dto.getNumeroIdentificacion())
                        .orElseThrow(() -> new ValidationException(
                                "El estudiante no se encuentra registrado en el sistema. Debe ingresar primer nombre, primer apellido y correo para nuevo alumno."
                        ));
                dto.setPrimerNombre(usuario.primerNombre());
                dto.setPrimerApellido(usuario.primerApellido());
                dto.setCorreo(usuario.correo());
                if (dto.getPassword() == null || dto.getPassword().isBlank()) {
                    dto.setPassword("Temporal2026*");
                }
            }
        }
    }
}