package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto.CoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain.CoordinadorDomain;

public final class ConsultarCoordinadoresMapper {

    private ConsultarCoordinadoresMapper() {
    }

    public static CoordinadorDTO toDTO(final CoordinadorDomain domain) {
        return new CoordinadorDTO(domain.id(), domain.idUsuario(), domain.numeroIdentificacion(), domain.nombreCompleto(),
                domain.idPrograma(), domain.nombrePrograma(), domain.estaActivoCoordinador(),
                domain.primerNombre(), domain.segundoNombre(), domain.primerApellido(), domain.segundoApellido(),
                domain.correo(), domain.estaActivoRolCoordinador(), domain.nombreFacultad(), domain.totalDocentes(),
                domain.totalGrupos());
    }
}
