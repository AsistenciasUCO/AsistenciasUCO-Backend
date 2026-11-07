package co.edu.uco.asistenciasuco.application.features.asistenciaqr.common;

import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaEventos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * CULM-001 B.2 (contrato 3.8): eventos SSE del QR de asistencia. {@code ASISTENCIA_QR_ROTADO} lleva el token (en la URL y
 * en el QR) y su audiencia es solo el docente titular ([SUPUESTO-B-16]).
 */
public final class AsistenciaQrEventos {

    public static final String ASISTENCIA_QR_ROTADO = "ASISTENCIA_QR_ROTADO";
    public static final String ASISTENCIA_QR_REGISTRADA = "ASISTENCIA_QR_REGISTRADA";

    private final TomaAsistenciaEventos eventos;

    public AsistenciaQrEventos(final TomaAsistenciaEventos eventos) {
        this.eventos = Objects.requireNonNull(eventos, "TomaAsistenciaEventos es obligatorio.");
    }

    public void publicarRotado(final QrTomaResumen qr) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("grupo", qr.idGrupo().toString());
        payload.put("sesion", qr.idSesion().toString());
        payload.put("toma", qr.idToma().toString());
        payload.put("pin", qr.pin());
        payload.put("urlAsistencia", qr.urlAsistencia());
        payload.put("qrPngBase64", qr.qrPngBase64());
        payload.put("fechaRotacionUtc", String.valueOf(qr.fechaRotacionUtc()));
        payload.put("fechaExpiracionUtc", String.valueOf(qr.fechaExpiracionUtc()));
        payload.put("rotacionSegundos", qr.rotacionSegundos());
        eventos.publicar(ASISTENCIA_QR_ROTADO, payload);
    }

    public void publicarRegistrada(final Map<String, Object> payload) {
        eventos.publicar(ASISTENCIA_QR_REGISTRADA, payload);
    }
}
