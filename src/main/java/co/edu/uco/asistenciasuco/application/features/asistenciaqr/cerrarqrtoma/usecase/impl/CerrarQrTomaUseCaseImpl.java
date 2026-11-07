package co.edu.uco.asistenciasuco.application.features.asistenciaqr.cerrarqrtoma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.cerrarqrtoma.usecase.CerrarQrTomaUseCase;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaAcceso;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaQrRepositoryPort;

import java.util.Objects;

/** CULM-001 B.2: cierra el QR de la toma. No exige el interruptor: dejar de proyectar siempre es posible. */
public final class CerrarQrTomaUseCaseImpl implements CerrarQrTomaUseCase {

    private final TomaAsistenciaAcceso tomas;
    private final AsistenciaQrRepositoryPort repositorio;

    public CerrarQrTomaUseCaseImpl(final TomaAsistenciaAcceso tomas, final AsistenciaQrRepositoryPort repositorio) {
        this.tomas = Objects.requireNonNull(tomas, "TomaAsistenciaAcceso es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "AsistenciaQrRepositoryPort es obligatorio.");
    }

    @Override
    public String execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para cerrar el QR de la toma es obligatorio.");
        tomas.exigirTitular(comando.tomaId(), comando.usuarioEjecutor());
        return repositorio.cerrar(comando.tomaId(), comando.usuarioEjecutor());
    }
}
