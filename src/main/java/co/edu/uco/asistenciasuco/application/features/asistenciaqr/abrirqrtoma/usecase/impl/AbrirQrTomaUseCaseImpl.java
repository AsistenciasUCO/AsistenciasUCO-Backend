package co.edu.uco.asistenciasuco.application.features.asistenciaqr.abrirqrtoma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrCredencialesEmisor;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrEventos;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrPresentador;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.QrTomaResumen;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.abrirqrtoma.usecase.AbrirQrTomaUseCase;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaAcceso;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaQrRepositoryPort;

import java.util.Objects;

/**
 * CULM-001 B.2 (HU175): abre (o re-proyecta) el QR de una toma QR abierta. Exige el interruptor ({@code AQR_010}) y la titularidad (403) en Application; la
 * base de datos repite la titularidad y exige modo QR ({@code AQR_003}), toma abierta ({@code AQR_005}) y ventana
 * ({@code SES_*}). Un PIN repetido ({@code AQR_006}) se reintenta hasta 5 veces. Tras el commit publica
 * {@code ASISTENCIA_QR_ROTADO} al titular.
 */
public final class AbrirQrTomaUseCaseImpl implements AbrirQrTomaUseCase {

    private final AsistenciaQrAccesoPolicy acceso;
    private final TomaAsistenciaAcceso tomas;
    private final AsistenciaQrCredencialesEmisor emisor;
    private final AsistenciaQrPresentador presentador;
    private final AsistenciaQrRepositoryPort repositorio;
    private final AsistenciaQrEventos eventos;

    public AbrirQrTomaUseCaseImpl(
            final AsistenciaQrAccesoPolicy acceso,
            final TomaAsistenciaAcceso tomas,
            final AsistenciaQrCredencialesEmisor emisor,
            final AsistenciaQrPresentador presentador,
            final AsistenciaQrRepositoryPort repositorio,
            final AsistenciaQrEventos eventos
    ) {
        this.acceso = Objects.requireNonNull(acceso, "AsistenciaQrAccesoPolicy es obligatoria.");
        this.tomas = Objects.requireNonNull(tomas, "TomaAsistenciaAcceso es obligatorio.");
        this.emisor = Objects.requireNonNull(emisor, "AsistenciaQrCredencialesEmisor es obligatorio.");
        this.presentador = Objects.requireNonNull(presentador, "AsistenciaQrPresentador es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "AsistenciaQrRepositoryPort es obligatorio.");
        this.eventos = Objects.requireNonNull(eventos, "AsistenciaQrEventos es obligatorio.");
    }

    @Override
    public QrTomaResumen execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando del QR de la toma es obligatorio.");
        acceso.exigirHabilitada();
        tomas.exigirTitular(comando.tomaId(), comando.usuarioEjecutor());
        final QrTomaResumen qr = emisor.emitir(credenciales -> {
            final AsistenciaQrRepositoryPort.QrEmitido emitido = repositorio.abrir(
                    comando.tomaId(), credenciales.token(), credenciales.pin(), comando.usuarioEjecutor());
            final AsistenciaQrPresentador.Presentacion presentacion = presentador.presentar(credenciales.token());
            return new QrTomaResumen(emitido.idToma(), emitido.idSesion(), emitido.idGrupo(), credenciales.pin(),
                    presentacion.urlAsistencia(), presentacion.qrPngBase64(), emitido.fechaRotacionUtc(),
                    emitido.fechaExpiracionUtc(), emitido.rotacionSegundos());
        });
        eventos.publicarRotado(qr);
        return qr;
    }
}
