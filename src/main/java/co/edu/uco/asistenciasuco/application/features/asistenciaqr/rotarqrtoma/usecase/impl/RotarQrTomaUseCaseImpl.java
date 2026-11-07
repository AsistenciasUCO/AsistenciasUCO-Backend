package co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotarqrtoma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrCredencialesEmisor;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrEventos;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrPresentador;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.QrTomaResumen;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotarqrtoma.usecase.RotarQrTomaUseCase;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaQrRepositoryPort;

import java.util.Objects;

/**
 * CULM-001 B.2 (HU175): regenera a demanda el QR vigente de la toma. Exige el interruptor ({@code AQR_010}) y la titularidad (403) en Application; la
 * base de datos repite la titularidad y exige modo QR ({@code AQR_003}), toma abierta ({@code AQR_005}) y ventana
 * ({@code SES_*}). Un PIN repetido ({@code AQR_006}) se reintenta hasta 5 veces. Tras el commit publica
 * {@code ASISTENCIA_QR_ROTADO} al titular.
 */
public final class RotarQrTomaUseCaseImpl implements RotarQrTomaUseCase {

    private final AsistenciaQrAccesoPolicy acceso;
    private final TomaAsistenciaAcceso tomas;
    private final AsistenciaQrCredencialesEmisor emisor;
    private final AsistenciaQrPresentador presentador;
    private final AsistenciaQrRepositoryPort repositorio;
    private final AsistenciaQrEventos eventos;

    public RotarQrTomaUseCaseImpl(
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
        final TomaAsistenciaResumen toma = tomas.exigirTitular(comando.tomaId(), comando.usuarioEjecutor());
        final QrTomaResumen qr = emisor.emitir(credenciales -> {
            final AsistenciaQrRepositoryPort.QrRotado rotado = repositorio.rotarManual(
                    comando.tomaId(), credenciales.token(), credenciales.pin(), comando.usuarioEjecutor());
            final AsistenciaQrPresentador.Presentacion presentacion = presentador.presentar(credenciales.token());
            return new QrTomaResumen(rotado.idToma(), toma.idSesion(), rotado.idGrupo(), credenciales.pin(),
                    presentacion.urlAsistencia(), presentacion.qrPngBase64(), rotado.fechaRotacionUtc(),
                    rotado.fechaExpiracionUtc(), acceso.rotacionSegundos());
        });
        eventos.publicarRotado(qr);
        return qr;
    }
}
