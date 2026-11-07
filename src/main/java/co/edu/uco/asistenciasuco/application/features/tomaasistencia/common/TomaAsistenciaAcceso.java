package co.edu.uco.asistenciasuco.application.features.tomaasistencia.common;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.exception.TomaAsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 B.2 (contrato §3.9): la toma debe existir ({@code TOM_001}, 404) y su sesion pertenecer a un grupo del docente
 * autenticado (403). La base de datos repite RBAC DOCENTE y la titularidad dentro del SP.
 */
public final class TomaAsistenciaAcceso {

    private final TomaAsistenciaRepositoryPort repositorio;
    private final InstitutionalScopePort scope;

    public TomaAsistenciaAcceso(final TomaAsistenciaRepositoryPort repositorio, final InstitutionalScopePort scope) {
        this.repositorio = Objects.requireNonNull(repositorio, "TomaAsistenciaRepositoryPort es obligatorio.");
        this.scope = Objects.requireNonNull(scope, "InstitutionalScopePort es obligatorio.");
    }

    public TomaAsistenciaResumen exigirTitular(final UUID tomaId, final UUID usuarioEjecutor) {
        final TomaAsistenciaResumen toma = repositorio.consultarToma(tomaId)
                .orElseThrow(() -> new ResourceNotFoundException(TomaAsistenciaErrorCode.TOM_001));
        if (!scope.canDocenteAccessGrupo(usuarioEjecutor, toma.idGrupo())) {
            throw new ForbiddenException(AsistenciaErrorCode.ERR_DOCENTE_SIN_TITULARIDAD_SESION);
        }
        return toma;
    }

    /** Relee la toma tras un comando (estado y conteos ya confirmados por la base de datos). */
    public TomaAsistenciaResumen releer(final UUID tomaId) {
        return repositorio.consultarToma(tomaId)
                .orElseThrow(() -> new ResourceNotFoundException(TomaAsistenciaErrorCode.TOM_001));
    }
}
