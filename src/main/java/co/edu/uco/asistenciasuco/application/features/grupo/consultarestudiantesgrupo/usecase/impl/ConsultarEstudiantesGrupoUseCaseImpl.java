package co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.ConsultarEstudiantesGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.domain.ConsultarEstudiantesGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.entity.EstudianteGrupoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.mapper.ConsultarEstudiantesGrupoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.List;
import java.util.Objects;

/**
 * Autorizacion: el matcher HTTP admite DOCENTE, COORDINADOR y ADMINISTRADOR sobre
 * {@code GET /api/v1/grupos/{grupoId}/estudiantes}. CULM-001 A.2-2: el SP
 * {@code usp_consultar_estudiantes_grupo_ambito_paginado} es la autoridad de ambito para los tres roles (DOCENTE
 * titular, COORDINADOR del programa del grupo, ADMINISTRADOR de la institucion); un grupo inexistente o ajeno responde
 * 403. El control previo de titularidad del docente se conserva como defensa adicional.
 */
public final class ConsultarEstudiantesGrupoUseCaseImpl implements ConsultarEstudiantesGrupoUseCase {

    private final GrupoRepositoryPort grupoRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public ConsultarEstudiantesGrupoUseCaseImpl(
            final GrupoRepositoryPort grupoRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this.grupoRepositoryPort = Objects.requireNonNull(grupoRepositoryPort, "El puerto GrupoRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public List<EstudianteGrupoEntity> execute(final ConsultarEstudiantesGrupoDomain domain) {
        if (institutionalScopePort.findDocenteIdByUsuario(domain.usuarioEjecutor()).isPresent()
                && !institutionalScopePort.canDocenteAccessGrupo(domain.usuarioEjecutor(), domain.grupoId())) {
            throw new ForbiddenException("El docente autenticado no tiene titularidad sobre el grupo consultado.");
        }
        return ConsultarEstudiantesGrupoRepositoryMapper.toEntities(
                grupoRepositoryPort.consultarEstudiantesGrupo(domain.grupoId(), domain.usuarioEjecutor())
        );
    }
}
