package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.domain.MateriaEstudianteDomain;

public final class ConsultarMateriasEstudianteMapper {

    private ConsultarMateriasEstudianteMapper() {
    }

    public static MateriaEstudianteDTO toDTO(final MateriaEstudianteDomain domain) {
        return new MateriaEstudianteDTO(domain.idAsignatura(), domain.nombreAsignatura(), domain.idGrupo(),
                domain.nombreGrupo(), domain.indicadores());
    }
}
