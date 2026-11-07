package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

/**
 * Mapper para convertir el DTO de entrada al dominio del caso de uso.
 */
public final class RegistrarEstudianteMapper {

    private RegistrarEstudianteMapper() {
    }

    public static RegistrarEstudianteDomain toDomain(final RegistrarEstudianteDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para registrar estudiante en grupo es obligatorio.");
        }

        // CULM-001 A.2-15: sin correo, el estudiante existente se resuelve en el caso de uso por su documento (busqueda con
        // titularidad, fila Estudiante y misma institucion dentro del SP); nunca a partir de un documento libre.
        if (TextHelper.isNullOrBlank(dto.getCorreo())) {
            return RegistrarEstudianteDomain.pendienteDeResolverPorDocumento(
                    dto.getTipoIdentificacionId(), dto.getNumeroIdentificacion(), dto.getPassword(), dto.getGrupoId(),
                    dto.getUsuarioEjecutor());
        }
        return new RegistrarEstudianteDomain(
                dto.getTipoIdentificacionId(),
                dto.getNumeroIdentificacion(),
                dto.getPrimerApellido(),
                dto.getSegundoApellido(),
                dto.getPrimerNombre(),
                dto.getSegundoNombre(),
                dto.getCorreo(),
                dto.getPassword(),
                dto.getGrupoId(),
                dto.getUsuarioEjecutor()
        );
    }

    public static RegistrarEstudianteResultadoDTO toDTO(final RegistrarEstudianteResultadoEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("El resultado de registrar estudiante en grupo es obligatorio.");
        }

        return new RegistrarEstudianteResultadoDTO(entity.isExitoso(), entity.getMensajeUsuario());
    }
}
