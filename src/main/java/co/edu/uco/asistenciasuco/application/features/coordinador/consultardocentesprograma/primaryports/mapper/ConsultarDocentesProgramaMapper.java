package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain.DocenteProgramaDomain;

public final class ConsultarDocentesProgramaMapper {

    private ConsultarDocentesProgramaMapper() {
    }

    public static DocenteProgramaDTO toDTO(final DocenteProgramaDomain domain) {
        return new DocenteProgramaDTO(
                domain.id(),
                domain.idUsuario(),
                domain.numeroIdentificacion(),
                domain.nombreCompleto(),
                domain.correo(),
                domain.idPrograma(),
                domain.nombrePrograma(),
                domain.totalGruposAsignados(),
                domain.estaActivoDocente()
        );
    }
}
