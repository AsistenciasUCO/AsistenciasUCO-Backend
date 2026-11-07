package co.edu.uco.asistenciasuco.application.features.tomaasistencia.common;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;
import java.util.UUID;

/** CULM-001 B.2: la sesion debe existir (404) y pertenecer a un grupo del docente autenticado (403). */
public final class SesionTitularAcceso {

    private final SesionProcedenciaQueryPort sesiones;
    private final InstitutionalScopePort scope;

    public SesionTitularAcceso(final SesionProcedenciaQueryPort sesiones, final InstitutionalScopePort scope) {
        this.sesiones = Objects.requireNonNull(sesiones, "SesionProcedenciaQueryPort es obligatorio.");
        this.scope = Objects.requireNonNull(scope, "InstitutionalScopePort es obligatorio.");
    }

    public SesionProcedenciaRepositoryProjection exigirTitular(final UUID sesionId, final UUID usuarioEjecutor) {
        final SesionProcedenciaRepositoryProjection sesion = sesiones.consultarSesion(sesionId)
                .orElseThrow(() -> new ResourceNotFoundException(SesionErrorCode.ERR_SESION_NO_EXISTE));
        if (!scope.canDocenteAccessGrupo(usuarioEjecutor, sesion.grupo())) {
            throw new ForbiddenException(AsistenciaErrorCode.ERR_DOCENTE_SIN_TITULARIDAD_SESION);
        }
        return sesion;
    }
}
