package co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.DecanoIndicadoresRules;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisDecanoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.ConsultarKpisDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.ConsultarKpisDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.KpisDecanoDomain;
import java.util.Objects;

public final class ConsultarKpisDecanoUseCaseImpl implements ConsultarKpisDecanoUseCase {

    private final InstitutionalScopePort scopePort;
    private final DecanoIndicadoresQueryPort queryPort;

    public ConsultarKpisDecanoUseCaseImpl(final InstitutionalScopePort scopePort, final DecanoIndicadoresQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "DecanoIndicadoresQueryPort es obligatorio.");
    }

    @Override
    public KpisDecanoDomain execute(final ConsultarKpisDecanoDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos de la consulta son obligatorios.");
        }
        DecanoIndicadoresRules.facultadDelDecano(scopePort, domain.actorUsuarioId());
        final KpisDecanoProjection p = queryPort.consultarKpis(domain.actorUsuarioId(), domain.idPeriodoAcademico());
        return new KpisDecanoDomain(p.idFacultad(), p.nombreFacultad(), p.idPeriodoAcademico(), p.nombrePeriodoAcademico(), p.totalProgramas(), p.totalEstudiantes(), p.estudiantesRegulares(), p.estudiantesRiesgo(), p.estudiantesPerdida(), p.tasaRetencion(), p.sesionesEjecutadas(), p.faltasSinJustaCausa(), p.porcentajeAusentismo(), p.porcentajeAsistencia(), p.gruposActivos(), p.cupoTotal(), p.matriculadosActivos(), p.tasaAforo(), p.umbralAdvertencia(), p.umbralPerdida(), p.tomasDictadas(), p.horasDictadas(), p.horasFalta());
    }
}
