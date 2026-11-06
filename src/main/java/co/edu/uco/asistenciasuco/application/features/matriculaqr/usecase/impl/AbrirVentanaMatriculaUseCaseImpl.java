package co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatriculaQrCredencialesEmisor;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatriculaQrPresentador;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.MatriculaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.AbrirVentanaMatriculaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.time.Clock;
import java.util.Objects;

/**
 * HU176B (CA-01): abre la ventana. La titularidad se valida aqui (403) y de nuevo en la base de datos. La DB solo recibe
 * el SHA-256 del token (lo calcula el adaptador de persistencia).
 */
public final class AbrirVentanaMatriculaUseCaseImpl implements AbrirVentanaMatriculaUseCase {

    private final MatriculaQrAccesoPolicy acceso;
    private final InstitutionalScopePort scope;
    private final MatriculaQrCredencialesEmisor emisor;
    private final MatriculaQrPresentador presentador;
    private final MatriculaQrRepositoryPort repositorio;
    private final Clock clock;

    public AbrirVentanaMatriculaUseCaseImpl(
            final MatriculaQrAccesoPolicy acceso,
            final InstitutionalScopePort scope,
            final MatriculaQrCredencialesEmisor emisor,
            final MatriculaQrPresentador presentador,
            final MatriculaQrRepositoryPort repositorio,
            final Clock clock
    ) {
        this.acceso = Objects.requireNonNull(acceso, "MatriculaQrAccesoPolicy es obligatoria.");
        this.scope = Objects.requireNonNull(scope, "InstitutionalScopePort es obligatorio.");
        this.emisor = Objects.requireNonNull(emisor, "MatriculaQrCredencialesEmisor es obligatorio.");
        this.presentador = Objects.requireNonNull(presentador, "MatriculaQrPresentador es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "MatriculaQrRepositoryPort es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para abrir la ventana de matricula es obligatorio.");
        acceso.exigirHabilitada();
        if (!scope.canDocenteAccessGrupo(comando.usuarioEjecutor(), comando.grupoId())) {
            throw new ForbiddenException("El docente autenticado no tiene titularidad sobre el grupo solicitado.");
        }
        return emisor.emitir(credenciales -> {
            final MatriculaQrRepositoryPort.VentanaAbierta ventana = repositorio.abrirVentana(
                    comando.grupoId(), credenciales.token(), credenciales.pin(), comando.usuarioEjecutor());
            final MatriculaQrPresentador.Presentacion presentacion = presentador.presentar(credenciales.token());
            return new Resultado(
                    ventana.idVentana(), credenciales.pin(), presentacion.urlMatricula(), presentacion.qrPngBase64(),
                    clock.instant(), ventana.fechaExpiracionUtc());
        });
    }
}
