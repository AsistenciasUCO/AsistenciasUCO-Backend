package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.AbrirVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.CerrarVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.RotarVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * HU176B: el docente titular abre, regenera y cierra la ventana de matricula por QR/PIN de su grupo. La respuesta lleva
 * el token solo dentro de la URL y del QR; no hay endpoint para consultarlo despues.
 */
@RestController
@RequestMapping("/api/v1/grupos/{grupoId}/matricula-qr")
public final class GrupoMatriculaQrController {

    /** Respuesta de apertura/regeneracion de la ventana (CONTRACT §3.1). */
    public record VentanaMatriculaResponse(
            UUID idVentana,
            String pin,
            String urlMatricula,
            String qrPngBase64,
            Instant fechaRotacionUtc,
            Instant fechaExpiracionUtc
    ) {
        static VentanaMatriculaResponse de(final AbrirVentanaMatriculaInputPort.Resultado r) {
            return new VentanaMatriculaResponse(
                    r.idVentana(), r.pin(), r.urlMatricula(), r.qrPngBase64(), r.fechaRotacionUtc(), r.fechaExpiracionUtc());
        }
    }

    private final AbrirVentanaMatriculaInputPort abrirVentana;
    private final RotarVentanaMatriculaInputPort rotarVentana;
    private final CerrarVentanaMatriculaInputPort cerrarVentana;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public GrupoMatriculaQrController(
            final AbrirVentanaMatriculaInputPort abrirVentana,
            final RotarVentanaMatriculaInputPort rotarVentana,
            final CerrarVentanaMatriculaInputPort cerrarVentana,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.abrirVentana = Objects.requireNonNull(abrirVentana, "AbrirVentanaMatriculaInputPort es obligatorio.");
        this.rotarVentana = Objects.requireNonNull(rotarVentana, "RotarVentanaMatriculaInputPort es obligatorio.");
        this.cerrarVentana = Objects.requireNonNull(cerrarVentana, "CerrarVentanaMatriculaInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping
    @AuditableOperation(action = "ABRIR_VENTANA_MATRICULA_QR", resourceType = "GRUPO", resourceIdPathVariable = "grupoId")
    public ResponseEntity<ApiDataResponse<VentanaMatriculaResponse>> abrir(@PathVariable final UUID grupoId) {
        final var resultado = abrirVentana.execute(
                new AbrirVentanaMatriculaInputPort.Comando(grupoId, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, VentanaMatriculaResponse.de(resultado)));
    }

    @PostMapping("/rotacion")
    @AuditableOperation(action = "ROTAR_VENTANA_MATRICULA_QR", resourceType = "GRUPO", resourceIdPathVariable = "grupoId")
    public ResponseEntity<ApiDataResponse<VentanaMatriculaResponse>> rotar(@PathVariable final UUID grupoId) {
        final var resultado = rotarVentana.execute(
                new RotarVentanaMatriculaInputPort.Comando(grupoId, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, VentanaMatriculaResponse.de(resultado)));
    }

    @DeleteMapping
    @AuditableOperation(action = "CERRAR_VENTANA_MATRICULA_QR", resourceType = "GRUPO", resourceIdPathVariable = "grupoId")
    public ResponseEntity<ApiDataResponse<Void>> cerrar(@PathVariable final UUID grupoId) {
        cerrarVentana.execute(
                new CerrarVentanaMatriculaInputPort.Comando(grupoId, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }
}
