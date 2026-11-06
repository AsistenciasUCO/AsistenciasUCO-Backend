package co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.CerrarVentanaMatriculaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;

/**
 * HU176B (CA-02): cierra la ventana. No consulta el interruptor: cerrar siempre debe poder hacerse, incluso con la
 * funcionalidad apagada.
 */
public final class CerrarVentanaMatriculaUseCaseImpl implements CerrarVentanaMatriculaUseCase {

    private final InstitutionalScopePort scope;
    private final MatriculaQrRepositoryPort repositorio;

    public CerrarVentanaMatriculaUseCaseImpl(final InstitutionalScopePort scope, final MatriculaQrRepositoryPort repositorio) {
        this.scope = Objects.requireNonNull(scope, "InstitutionalScopePort es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "MatriculaQrRepositoryPort es obligatorio.");
    }

    @Override
    public Void execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para cerrar la ventana de matricula es obligatorio.");
        if (!scope.canDocenteAccessGrupo(comando.usuarioEjecutor(), comando.grupoId())) {
            throw new ForbiddenException("El docente autenticado no tiene titularidad sobre el grupo solicitado.");
        }
        repositorio.cerrarVentana(comando.grupoId(), comando.usuarioEjecutor());
        return null;
    }
}
