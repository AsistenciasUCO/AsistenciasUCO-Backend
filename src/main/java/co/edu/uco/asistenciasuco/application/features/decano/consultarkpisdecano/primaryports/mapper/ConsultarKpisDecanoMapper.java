package co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.ConsultarKpisDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.ConsultarKpisDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.KpisDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.KpisDecanoDomain;

public final class ConsultarKpisDecanoMapper {

    private ConsultarKpisDecanoMapper() {
    }

    public static ConsultarKpisDecanoDomain toDomain(final ConsultarKpisDecanoDTO dto) {
        return new ConsultarKpisDecanoDomain(dto.actorUsuarioId(), dto.idPeriodoAcademico());
    }

    public static KpisDecanoDTO toDto(final KpisDecanoDomain d) {
        return new KpisDecanoDTO(d.idFacultad(), d.nombreFacultad(), d.idPeriodoAcademico(), d.nombrePeriodoAcademico(), d.totalProgramas(), d.totalEstudiantes(), d.estudiantesRegulares(), d.estudiantesRiesgo(), d.estudiantesPerdida(), d.tasaRetencion(), d.sesionesEjecutadas(), d.faltasSinJustaCausa(), d.porcentajeAusentismo(), d.porcentajeAsistencia(), d.gruposActivos(), d.cupoTotal(), d.matriculadosActivos(), d.tasaAforo(), d.umbralAdvertencia(), d.umbralPerdida(), d.tomasDictadas(), d.horasDictadas(), d.horasFalta());
    }
}
