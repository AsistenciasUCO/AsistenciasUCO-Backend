package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistenciaqr;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.abrirqrtoma.primaryports.AbrirQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.cerrarqrtoma.primaryports.CerrarQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotarqrtoma.primaryports.RotarQrTomaInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistenciaqr.response.AsistenciaQrHttpResponses.QrTomaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 B.2 (HU175, contrato 3.2.6): el docente titular proyecta, regenera y deja de proyectar el QR/PIN de una toma QR.
 * El token solo viaja en esta respuesta y en el evento SSE {@code ASISTENCIA_QR_ROTADO} del titular.
 */
@RestController
@RequestMapping("/api/v1/tomas/{tomaId}/qr")
public final class TomaQrController {

    private final AbrirQrTomaInputPort abrirQr;
    private final RotarQrTomaInputPort rotarQr;
    private final CerrarQrTomaInputPort cerrarQr;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public TomaQrController(
            final AbrirQrTomaInputPort abrirQr,
            final RotarQrTomaInputPort rotarQr,
            final CerrarQrTomaInputPort cerrarQr,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.abrirQr = Objects.requireNonNull(abrirQr, "AbrirQrTomaInputPort es obligatorio.");
        this.rotarQr = Objects.requireNonNull(rotarQr, "RotarQrTomaInputPort es obligatorio.");
        this.cerrarQr = Objects.requireNonNull(cerrarQr, "CerrarQrTomaInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping
    @AuditableOperation(action = "ABRIR_QR_TOMA", resourceType = "TOMA_ASISTENCIA", resourceIdPathVariable = "tomaId")
    public ResponseEntity<ApiDataResponse<QrTomaResponse>> abrir(@PathVariable final UUID tomaId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, QrTomaResponse.from(abrirQr.execute(
                new AbrirQrTomaInputPort.Comando(tomaId, authenticatedUserResolver.requireAuthenticatedUserId())))));
    }

    @PostMapping("/rotacion")
    @AuditableOperation(action = "ROTAR_QR_TOMA", resourceType = "TOMA_ASISTENCIA", resourceIdPathVariable = "tomaId")
    public ResponseEntity<ApiDataResponse<QrTomaResponse>> rotar(@PathVariable final UUID tomaId) {
        return ResponseEntity.ok(new ApiDataResponse<>(true, QrTomaResponse.from(rotarQr.execute(
                new RotarQrTomaInputPort.Comando(tomaId, authenticatedUserResolver.requireAuthenticatedUserId())))));
    }

    @DeleteMapping
    @AuditableOperation(action = "CERRAR_QR_TOMA", resourceType = "TOMA_ASISTENCIA", resourceIdPathVariable = "tomaId")
    public ResponseEntity<ApiMessageResponse> cerrar(@PathVariable final UUID tomaId) {
        return ResponseEntity.ok(new ApiMessageResponse(true, cerrarQr.execute(
                new CerrarQrTomaInputPort.Comando(tomaId, authenticatedUserResolver.requireAuthenticatedUserId()))));
    }
}
