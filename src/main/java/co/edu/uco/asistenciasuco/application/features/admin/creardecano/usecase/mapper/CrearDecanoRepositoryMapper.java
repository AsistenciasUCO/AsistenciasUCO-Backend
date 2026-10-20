package co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.CrearDecanoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FacultadProjection;

import java.util.UUID;

/**
 * Mapper entre el dominio de crear decano y el contrato del puerto secundario.
 */
public final class CrearDecanoRepositoryMapper {

    private CrearDecanoRepositoryMapper() {
    }

    public static DecanoCommandPort.CrearDecanoCommand toCommand(
            final CrearDecanoDomain domain,
            final UUID idDecano,
            final FacultadProjection facultad,
            final String passwordParaPersistir
    ) {
        return new DecanoCommandPort.CrearDecanoCommand(
                idDecano,
                domain.getTipoIdentificacionId(),
                domain.getNumeroIdentificacion(),
                domain.getPrimerNombre(),
                domain.getSegundoNombre(),
                domain.getPrimerApellido(),
                domain.getSegundoApellido(),
                domain.getCorreo(),
                domain.getIdFacultad(),
                facultad != null ? facultad.nombreFacultad() : null,
                passwordParaPersistir,
                domain.getUsuarioEjecutor()
        );
    }
}
