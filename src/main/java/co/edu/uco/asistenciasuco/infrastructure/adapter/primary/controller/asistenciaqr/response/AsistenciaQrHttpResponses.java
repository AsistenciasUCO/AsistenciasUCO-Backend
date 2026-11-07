package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistenciaqr.response;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.QrTomaResumen;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.primaryports.RegistrarAsistenciaPorTokenInputPort;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/** CULM-001 B.2 (contrato 3.2.6 y 3.2.7): cuerpos HTTP del QR de asistencia. Instantes UTC ISO-8601 con {@code Z}. */
public final class AsistenciaQrHttpResponses {

    private AsistenciaQrHttpResponses() {
    }

    /** El token en claro solo viaja dentro de {@code urlAsistencia} y {@code qrPngBase64}. */
    public record QrTomaResponse(UUID idToma, UUID idSesion, UUID idGrupo, String pin, String urlAsistencia, String qrPngBase64,
                                 String fechaRotacionUtc, String fechaExpiracionUtc, int rotacionSegundos) {

        public static QrTomaResponse from(final QrTomaResumen q) {
            return new QrTomaResponse(q.idToma(), q.idSesion(), q.idGrupo(), q.pin(), q.urlAsistencia(), q.qrPngBase64(),
                    utc(q.fechaRotacionUtc()), utc(q.fechaExpiracionUtc()), q.rotacionSegundos());
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record RegistroAsistenciaQrResponse(UUID idToma, UUID idSesion, String nombreAsignatura, String nombreGrupo, int numeroSesion,
                                               int numeroToma, boolean yaRegistrado, String fechaRegistroUtc, String mensaje) {

        public static RegistroAsistenciaQrResponse from(final RegistrarAsistenciaPorTokenInputPort.Resultado r) {
            return new RegistroAsistenciaQrResponse(r.idToma(), r.idSesion(), r.nombreAsignatura(), r.nombreGrupo(), r.numeroSesion(),
                    r.numeroToma(), r.yaRegistrado(), utc(r.fechaRegistroUtc()), r.mensaje());
        }
    }

    static String utc(final Instant instante) {
        return instante == null ? null : instante.toString();
    }
}
