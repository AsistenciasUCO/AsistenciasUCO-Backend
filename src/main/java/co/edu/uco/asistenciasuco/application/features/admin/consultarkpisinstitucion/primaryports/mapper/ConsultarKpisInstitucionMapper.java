package co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.ConsultarKpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.KpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain.ConsultarKpisInstitucionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain.KpisInstitucionDomain;

public final class ConsultarKpisInstitucionMapper {

    private ConsultarKpisInstitucionMapper() {
    }

    public static ConsultarKpisInstitucionDomain toDomain(final ConsultarKpisInstitucionDTO dto) {
        return new ConsultarKpisInstitucionDomain(dto.actorUsuarioId(), dto.idPeriodoAcademico());
    }

    public static KpisInstitucionDTO toDto(final KpisInstitucionDomain d) {
        return new KpisInstitucionDTO(d.idInstitucion(), d.nombreInstitucion(), d.idPeriodoAcademico(), d.nombrePeriodoAcademico(), d.totalFacultades(), d.totalProgramas(), d.totalEstudiantes(), d.estudiantesRegulares(), d.estudiantesRiesgo(), d.estudiantesPerdida(), d.tasaRetencion(), d.sesionesEjecutadas(), d.faltasSinJustaCausa(), d.porcentajeAusentismo(), d.porcentajeAsistencia(), d.sesionesVencidas(), d.sesionesConRegistro(), d.porcentajeCumplimiento(), d.estudiantesConMatriculaActiva(), d.estudiantesRegistrados(), d.docentesConGrupoActivo(), d.docentesRegistrados(), d.umbralAdvertencia(), d.umbralPerdida(), d.tomasDictadas(), d.horasDictadas(), d.horasFalta());
    }
}
