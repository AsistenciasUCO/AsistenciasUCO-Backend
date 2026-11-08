package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.EstadoAsistenciaSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;

public final class ConsultarSesionesMateriaEstudianteRepositoryMapper {

    private ConsultarSesionesMateriaEstudianteRepositoryMapper() {
    }

    public static SesionMateriaEstudianteDomain toDomain(final SesionMateriaEstudianteProjection projection,
                                                         final SesionProcedenciaEntity procedencia,
                                                         final EstadoAsistenciaSesion estadoAsistencia,
                                                         final String estadoSolicitudRevision,
                                                         final boolean reclamable) {
        return new SesionMateriaEstudianteDomain(projection.id(), projection.nombre(), projection.numero(),
                projection.codigo(), projection.numeroSemana(), projection.idGrupo(), projection.codigoGrupo(),
                projection.nombreGrupo(), projection.fechaHoraInicio(), projection.fechaHoraFin(),
                procedencia == null ? null : procedencia.fechaHoraInicioUtc(),
                procedencia == null ? null : procedencia.fechaHoraFinUtc(),
                procedencia == null ? EstadoHorarioSesion.INDETERMINADA : procedencia.estadoTemporal(),
                estadoAsistencia, estadoSolicitudRevision, reclamable,
                procedencia == null ? null : procedencia.cancelacion(),
                projection.fechaLimiteJustificacionUtc(), projection.plazoJustificacionVigente());
    }
}
