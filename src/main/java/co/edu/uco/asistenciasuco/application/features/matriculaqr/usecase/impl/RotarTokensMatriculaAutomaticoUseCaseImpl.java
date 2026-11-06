package co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatriculaQrCredencialesEmisor;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatriculaQrPresentador;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.MatriculaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.RotarTokensMatriculaAutomaticoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * HU176B (D2): rota las ventanas vencidas y empuja el QR nuevo al docente titular por el SSE existente. El stream es por
 * grupo y solo lo recibe el docente titular, de modo que el evento lleva {@code grupo} (CONTRACT A3). Un fallo en una
 * ventana no detiene las demas; el token jamas se loguea.
 */
public final class RotarTokensMatriculaAutomaticoUseCaseImpl implements RotarTokensMatriculaAutomaticoUseCase {

    public static final String EVENTO_ROTADO = "MATRICULA_QR_ROTADO";
    private static final Logger LOGGER = LoggerFactory.getLogger(RotarTokensMatriculaAutomaticoUseCaseImpl.class);

    private final MatriculaQrAccesoPolicy acceso;
    private final MatriculaQrCredencialesEmisor emisor;
    private final MatriculaQrPresentador presentador;
    private final MatriculaQrRepositoryPort repositorio;
    private final RealtimePublisherPort realtime;

    public RotarTokensMatriculaAutomaticoUseCaseImpl(
            final MatriculaQrAccesoPolicy acceso,
            final MatriculaQrCredencialesEmisor emisor,
            final MatriculaQrPresentador presentador,
            final MatriculaQrRepositoryPort repositorio,
            final RealtimePublisherPort realtime
    ) {
        this.acceso = Objects.requireNonNull(acceso, "MatriculaQrAccesoPolicy es obligatoria.");
        this.emisor = Objects.requireNonNull(emisor, "MatriculaQrCredencialesEmisor es obligatorio.");
        this.presentador = Objects.requireNonNull(presentador, "MatriculaQrPresentador es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "MatriculaQrRepositoryPort es obligatorio.");
        this.realtime = Objects.requireNonNull(realtime, "RealtimePublisherPort es obligatorio.");
    }

    @Override
    public int execute() {
        try {
            acceso.exigirHabilitada();
        } catch (final RuntimeException _) {
            return 0;
        }
        int rotadas = 0;
        for (final MatriculaQrRepositoryPort.VentanaPorRotar pendiente : repositorio.consultarVentanasPorRotar()) {
            try {
                if (rotar(pendiente)) {
                    rotadas++;
                }
            } catch (final RuntimeException exception) {
                LOGGER.error("No fue posible rotar la ventana de matricula del grupo {}: {}",
                        pendiente.idGrupo(), exception.getClass().getSimpleName());
            }
        }
        return rotadas;
    }

    private boolean rotar(final MatriculaQrRepositoryPort.VentanaPorRotar pendiente) {
        final MatriculaQrCredencialesEmisor.Credenciales[] emitidas = new MatriculaQrCredencialesEmisor.Credenciales[1];
        final MatriculaQrRepositoryPort.VentanaRotada ventana = emisor.emitir(credenciales -> {
            emitidas[0] = credenciales;
            return repositorio.rotarAutomatico(pendiente.idGrupo(), credenciales.token(), credenciales.pin());
        });
        // Otra instancia del job ya roto esta ventana: no hay QR nuevo que publicar.
        if (ventana == null) {
            return false;
        }
        final MatriculaQrPresentador.Presentacion presentacion = presentador.presentar(emitidas[0].token());
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("grupo", pendiente.idGrupo().toString());
        payload.put("pin", emitidas[0].pin());
        payload.put("urlMatricula", presentacion.urlMatricula());
        payload.put("qrPngBase64", presentacion.qrPngBase64());
        payload.put("fechaRotacionUtc", String.valueOf(ventana.fechaRotacionUtc()));
        payload.put("fechaExpiracionUtc", String.valueOf(ventana.fechaExpiracionUtc()));
        realtime.publish(RealtimeEvent.of(EVENTO_ROTADO, payload));
        return true;
    }
}
