package co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotartokensasistenciaautomatico.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrCredencialesEmisor;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrEventos;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrPresentador;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.QrTomaResumen;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotartokensasistenciaautomatico.usecase.RotarTokensAsistenciaAutomaticoUseCase;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaQrRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * CULM-001 B.2: rotacion automatica. La base de datos decide que tomas rotar ({@code debeRotar}) y si la rotacion aplica
 * ({@code rotado = 0} cuando otra instancia ya roto: sin evento). Un fallo en una toma no detiene las demas; con el
 * interruptor apagado no rota. El token jamas se loguea.
 */
public final class RotarTokensAsistenciaAutomaticoUseCaseImpl implements RotarTokensAsistenciaAutomaticoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotarTokensAsistenciaAutomaticoUseCaseImpl.class);

    private final AsistenciaQrAccesoPolicy acceso;
    private final AsistenciaQrCredencialesEmisor emisor;
    private final AsistenciaQrPresentador presentador;
    private final AsistenciaQrRepositoryPort repositorio;
    private final TomaAsistenciaRepositoryPort tomas;
    private final AsistenciaQrEventos eventos;

    public RotarTokensAsistenciaAutomaticoUseCaseImpl(
            final AsistenciaQrAccesoPolicy acceso,
            final AsistenciaQrCredencialesEmisor emisor,
            final AsistenciaQrPresentador presentador,
            final AsistenciaQrRepositoryPort repositorio,
            final TomaAsistenciaRepositoryPort tomas,
            final AsistenciaQrEventos eventos
    ) {
        this.acceso = Objects.requireNonNull(acceso, "AsistenciaQrAccesoPolicy es obligatoria.");
        this.emisor = Objects.requireNonNull(emisor, "AsistenciaQrCredencialesEmisor es obligatorio.");
        this.presentador = Objects.requireNonNull(presentador, "AsistenciaQrPresentador es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "AsistenciaQrRepositoryPort es obligatorio.");
        this.tomas = Objects.requireNonNull(tomas, "TomaAsistenciaRepositoryPort es obligatorio.");
        this.eventos = Objects.requireNonNull(eventos, "AsistenciaQrEventos es obligatorio.");
    }

    @Override
    public int execute() {
        if (!acceso.estaHabilitada()) {
            return 0;
        }
        int rotadas = 0;
        for (final AsistenciaQrRepositoryPort.TomaPorRotar pendiente : repositorio.consultarTomasPorRotar()) {
            try {
                if (rotar(pendiente)) {
                    rotadas++;
                }
            } catch (final RuntimeException exception) {
                LOGGER.error("No fue posible rotar el QR de asistencia de la toma {}: {}",
                        pendiente.idToma(), exception.getClass().getSimpleName());
            }
        }
        return rotadas;
    }

    private boolean rotar(final AsistenciaQrRepositoryPort.TomaPorRotar pendiente) {
        final Optional<QrTomaResumen> qr = emisor.emitir(credenciales -> repositorio
                .rotarAutomatico(pendiente.idToma(), credenciales.token(), credenciales.pin())
                .map(rotado -> {
                    final AsistenciaQrPresentador.Presentacion presentacion = presentador.presentar(credenciales.token());
                    final TomaAsistenciaResumen toma = tomas.consultarToma(pendiente.idToma()).orElseThrow();
                    return new QrTomaResumen(rotado.idToma(), toma.idSesion(), rotado.idGrupo(), credenciales.pin(),
                            presentacion.urlAsistencia(), presentacion.qrPngBase64(), rotado.fechaRotacionUtc(),
                            rotado.fechaExpiracionUtc(), acceso.rotacionSegundos());
                }));
        qr.ifPresent(eventos::publicarRotado);
        return qr.isPresent();
    }
}
