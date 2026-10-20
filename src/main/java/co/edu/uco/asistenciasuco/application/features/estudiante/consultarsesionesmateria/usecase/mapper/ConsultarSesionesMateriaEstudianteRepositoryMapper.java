package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.EstadoAsistenciaSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;

public final class ConsultarSesionesMateriaEstudianteRepositoryMapper {

    private ConsultarSesionesMateriaEstudianteRepositoryMapper() {
    }

    public static SesionMateriaEstudianteDomain toDomain(final SesionMateriaEstudianteProjection projection,
                                                         final EstadoAsistenciaSesion estadoAsistencia,
                                                         final String estadoSolicitudRevision,
                                                         final boolean reclamable) {
        return new SesionMateriaEstudianteDomain(projection.id(), projection.nombre(), projection.numero(),
                projection.codigo(), projection.numeroSemana(), projection.idGrupo(), projection.codigoGrupo(),
                projection.nombreGrupo(), projection.fechaHoraInicio(), projection.fechaHoraFin(),
                estadoAsistencia, estadoSolicitudRevision, reclamable);
    }
}
