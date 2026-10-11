package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.common.mapper.SesionProcedenciaMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.ConsultarSesionV2UseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.domain.ConsultarSesionV2Domain;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;

/**
 * Consulta v2 de una sesion. Ademas del rol DOCENTE (matcher HTTP), exige titularidad del docente
 * autenticado sobre el grupo de la sesion: 404 si no existe, 403 si existe y pertenece a otro docente.
 */
public final class ConsultarSesionV2UseCaseImpl implements ConsultarSesionV2UseCase {

    private final SesionProcedenciaQueryPort sesionProcedenciaQueryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public ConsultarSesionV2UseCaseImpl(
            final SesionProcedenciaQueryPort sesionProcedenciaQueryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this.sesionProcedenciaQueryPort = Objects.requireNonNull(sesionProcedenciaQueryPort, "SesionProcedenciaQueryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public SesionProcedenciaEntity execute(final ConsultarSesionV2Domain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar sesion v2 es obligatorio.");
        }
        final SesionProcedenciaRepositoryProjection row = sesionProcedenciaQueryPort.consultarSesion(domain.getSesion())
                .orElseThrow(() -> new ResourceNotFoundException(SesionErrorCode.ERR_SESION_NO_EXISTE));
        if (!institutionalScopePort.canDocenteAccessGrupo(domain.getUsuarioEjecutor(), row.grupo())) {
            throw new ForbiddenException("El docente autenticado no tiene titularidad sobre la sesion consultada.");
        }
        return SesionProcedenciaMapper.toEntity(row);
    }
}
