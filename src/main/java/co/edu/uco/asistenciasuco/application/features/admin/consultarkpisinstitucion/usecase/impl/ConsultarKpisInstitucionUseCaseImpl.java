package co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdminIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.ConsultarKpisInstitucionUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain.ConsultarKpisInstitucionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisInstitucionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain.KpisInstitucionDomain;
import java.util.Objects;

/** El ambito (institucion del administrador) lo fuerza la DB: aqui solo se valida la forma de la consulta. */
public final class ConsultarKpisInstitucionUseCaseImpl implements ConsultarKpisInstitucionUseCase {

    private final AdminIndicadoresQueryPort queryPort;

    public ConsultarKpisInstitucionUseCaseImpl(final AdminIndicadoresQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "AdminIndicadoresQueryPort es obligatorio.");
    }

    @Override
    public KpisInstitucionDomain execute(final ConsultarKpisInstitucionDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos de la consulta son obligatorios.");
        }
        final KpisInstitucionProjection p = queryPort.consultarKpis(domain.actorUsuarioId(), domain.idPeriodoAcademico());
        return new KpisInstitucionDomain(p.idInstitucion(), p.nombreInstitucion(), p.idPeriodoAcademico(), p.nombrePeriodoAcademico(), p.totalFacultades(), p.totalProgramas(), p.totalEstudiantes(), p.estudiantesRegulares(), p.estudiantesRiesgo(), p.estudiantesPerdida(), p.tasaRetencion(), p.sesionesEjecutadas(), p.faltasSinJustaCausa(), p.porcentajeAusentismo(), p.porcentajeAsistencia(), p.sesionesVencidas(), p.sesionesConRegistro(), p.porcentajeCumplimiento(), p.estudiantesConMatriculaActiva(), p.estudiantesRegistrados(), p.docentesConGrupoActivo(), p.docentesRegistrados(), p.umbralAdvertencia(), p.umbralPerdida(), p.tomasDictadas(), p.horasDictadas(), p.horasFalta());
    }
}
