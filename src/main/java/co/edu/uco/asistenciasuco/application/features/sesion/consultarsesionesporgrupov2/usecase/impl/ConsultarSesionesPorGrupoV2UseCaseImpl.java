package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.common.mapper.SesionProcedenciaMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.ConsultarSesionesPorGrupoV2UseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.domain.ConsultarSesionesPorGrupoV2Domain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;
import java.util.Objects;

/**
 * Listado v2 por grupo con la misma titularidad que v1 ({@code canDocenteAccessGrupo}). Conserva el
 * orden de la fuente y TODAS las filas; cada fila decide su propio estado temporal.
 */
public final class ConsultarSesionesPorGrupoV2UseCaseImpl implements ConsultarSesionesPorGrupoV2UseCase {

    private final SesionProcedenciaQueryPort sesionProcedenciaQueryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public ConsultarSesionesPorGrupoV2UseCaseImpl(
            final SesionProcedenciaQueryPort sesionProcedenciaQueryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this.sesionProcedenciaQueryPort = Objects.requireNonNull(sesionProcedenciaQueryPort, "SesionProcedenciaQueryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public List<SesionProcedenciaEntity> execute(final ConsultarSesionesPorGrupoV2Domain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar sesiones v2 por grupo es obligatorio.");
        }
        if (!institutionalScopePort.canDocenteAccessGrupo(domain.getUsuarioEjecutor(), domain.getGrupo())) {
            throw new ForbiddenException("El docente autenticado no tiene titularidad sobre el grupo consultado.");
        }
        return sesionProcedenciaQueryPort.consultarSesionesPorGrupo(domain.getGrupo()).stream()
                .map(SesionProcedenciaMapper::toEntity)
                .toList();
    }
}
