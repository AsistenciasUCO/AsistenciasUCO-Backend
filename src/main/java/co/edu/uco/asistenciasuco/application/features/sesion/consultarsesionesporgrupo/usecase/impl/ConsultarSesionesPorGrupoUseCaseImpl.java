package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.mapper.ConsultarSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.ConsultarSesionesPorGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.domain.ConsultarSesionesPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;
import java.util.Objects;

/**
 * Implementacion del caso de uso consultar sesiones por grupo.
 *
 * <p>Autorizacion: si el usuario autenticado resuelve a una identidad Docente, debe tener
 * titularidad sobre el grupo consultado ({@link InstitutionalScopePort#canDocenteAccessGrupo}).
 * El matcher de seguridad HTTP ya restringe este endpoint a DOCENTE (ver {@code SecurityConfig}),
 * por lo que un usuario autenticado sin identidad Docente nunca deberia llegar aqui; se rechaza
 * de forma explicita en vez de asumir acceso.</p>
 */
public final class ConsultarSesionesPorGrupoUseCaseImpl implements ConsultarSesionesPorGrupoUseCase {

    private final SesionRepositoryPort sesionRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public ConsultarSesionesPorGrupoUseCaseImpl(
            final SesionRepositoryPort sesionRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this.sesionRepositoryPort = Objects.requireNonNull(sesionRepositoryPort, "El puerto de salida SesionRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public List<SesionConsultadaEntity> execute(final ConsultarSesionesPorGrupoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar sesiones por grupo es obligatorio.");
        }

        if (!institutionalScopePort.canDocenteAccessGrupo(domain.getUsuarioEjecutor(), domain.getGrupo())) {
            throw new ForbiddenException("El docente autenticado no tiene titularidad sobre el grupo consultado.");
        }

        return sesionRepositoryPort.consultarSesionesPorGrupo(domain.getGrupo(), domain.getUsuarioEjecutor()).stream()
                .map(ConsultarSesionRepositoryMapper::toUseCaseEntity)
                .toList();
    }
}
