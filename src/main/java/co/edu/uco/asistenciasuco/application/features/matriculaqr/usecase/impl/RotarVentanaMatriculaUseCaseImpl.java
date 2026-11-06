package co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatriculaQrCredencialesEmisor;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatriculaQrPresentador;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.MatriculaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.AbrirVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.RotarVentanaMatriculaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;

/** HU176B (D2): regeneracion manual del codigo por el docente titular. Misma respuesta que abrir la ventana. */
public final class RotarVentanaMatriculaUseCaseImpl implements RotarVentanaMatriculaUseCase {

    private final MatriculaQrAccesoPolicy acceso;
    private final InstitutionalScopePort scope;
    private final MatriculaQrCredencialesEmisor emisor;
    private final MatriculaQrPresentador presentador;
    private final MatriculaQrRepositoryPort repositorio;

    public RotarVentanaMatriculaUseCaseImpl(
            final MatriculaQrAccesoPolicy acceso,
            final InstitutionalScopePort scope,
            final MatriculaQrCredencialesEmisor emisor,
            final MatriculaQrPresentador presentador,
            final MatriculaQrRepositoryPort repositorio
    ) {
        this.acceso = Objects.requireNonNull(acceso, "MatriculaQrAccesoPolicy es obligatoria.");
        this.scope = Objects.requireNonNull(scope, "InstitutionalScopePort es obligatorio.");
        this.emisor = Objects.requireNonNull(emisor, "MatriculaQrCredencialesEmisor es obligatorio.");
        this.presentador = Objects.requireNonNull(presentador, "MatriculaQrPresentador es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "MatriculaQrRepositoryPort es obligatorio.");
    }

    @Override
    public AbrirVentanaMatriculaInputPort.Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para rotar la ventana de matricula es obligatorio.");
        acceso.exigirHabilitada();
        if (!scope.canDocenteAccessGrupo(comando.usuarioEjecutor(), comando.grupoId())) {
            throw new ForbiddenException("El docente autenticado no tiene titularidad sobre el grupo solicitado.");
        }
        return emisor.emitir(credenciales -> {
            final MatriculaQrRepositoryPort.VentanaRotada ventana = repositorio.rotarManual(
                    comando.grupoId(), credenciales.token(), credenciales.pin(), comando.usuarioEjecutor());
            final MatriculaQrPresentador.Presentacion presentacion = presentador.presentar(credenciales.token());
            return new AbrirVentanaMatriculaInputPort.Resultado(
                    ventana.idVentana(), credenciales.pin(), presentacion.urlMatricula(), presentacion.qrPngBase64(),
                    ventana.fechaRotacionUtc(), ventana.fechaExpiracionUtc());
        });
    }
}
