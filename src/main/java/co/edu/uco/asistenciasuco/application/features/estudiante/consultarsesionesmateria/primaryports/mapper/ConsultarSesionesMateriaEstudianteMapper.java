package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionMateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;

public final class ConsultarSesionesMateriaEstudianteMapper {

    private ConsultarSesionesMateriaEstudianteMapper() {
    }

    public static SesionMateriaEstudianteDTO toDTO(final SesionMateriaEstudianteDomain domain) {
        return new SesionMateriaEstudianteDTO(domain.id(), domain.nombre(), domain.numero(), domain.codigo(),
                domain.numeroSemana(), domain.idGrupo(), domain.codigoGrupo(), domain.nombreGrupo(),
                domain.fechaHoraInicio(), domain.fechaHoraFin(), domain.estadoAsistencia().name(),
                domain.estadoSolicitudRevision(), domain.reclamable());
    }
}
