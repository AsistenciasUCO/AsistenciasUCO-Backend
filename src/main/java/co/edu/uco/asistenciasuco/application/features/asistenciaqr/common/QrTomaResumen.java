package co.edu.uco.asistenciasuco.application.features.asistenciaqr.common;

import java.time.Instant;
import java.util.UUID;

/**
 * CULM-001 B.2 (contrato 3.2.6): QR vigente de una toma. El token en claro solo viaja dentro de {@code urlAsistencia} y
 * {@code qrPngBase64}, en esta respuesta y en el evento {@code ASISTENCIA_QR_ROTADO} del docente titular.
 */
public record QrTomaResumen(
        UUID idToma,
        UUID idSesion,
        UUID idGrupo,
        String pin,
        String urlAsistencia,
        String qrPngBase64,
        Instant fechaRotacionUtc,
        Instant fechaExpiracionUtc,
        int rotacionSegundos
) {
}
