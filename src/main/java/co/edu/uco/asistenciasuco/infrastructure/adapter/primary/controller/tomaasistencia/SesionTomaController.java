package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia;

import co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.primaryports.AbrirTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultartomassesion.primaryports.ConsultarTomasSesionInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.request.TomaAsistenciaRequests;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.response.TomaAsistenciaHttpResponses.TomaAsistenciaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.response.TomaAsistenciaHttpResponses.TomasSesionResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 B.2 (DEC-013, contrato 3.2.5): el docente titular abre tomas de asistencia en una sesion y consulta sus tomas
 * con la ventana de registro. La titularidad se valida en Application y en la base de datos.
 */
@RestController
@RequestMapping("/api/v1/sesiones/{sesionId}/tomas")
public final class SesionTomaController {

    private final AbrirTomaAsistenciaInputPort abrirToma;
    private final ConsultarTomasSesionInputPort consultarTomas;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public SesionTomaController(
            final AbrirTomaAsistenciaInputPort abrirToma,
            final ConsultarTomasSesionInputPort consultarTomas,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.abrirToma = Objects.requireNonNull(abrirToma, "AbrirTomaAsistenciaInputPort es obligatorio.");
        this.consultarTomas = Objects.requireNonNull(consultarTomas, "ConsultarTomasSesionInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping
    @AuditableOperation(action = "ABRIR_TOMA_ASISTENCIA", resourceType = "SESION", resourceIdPathVariable = "sesionId")
    public ResponseEntity<ApiDataResponse<TomaAsistenciaResponse>> abrir(
            @PathVariable final UUID sesionId,
            @RequestBody(required = false) final TomaAsistenciaRequests.AbrirToma request
    ) {
        final TomaAsistenciaResumen toma = abrirToma.execute(new AbrirTomaAsistenciaInputPort.Comando(
                sesionId, request == null ? null : request.modo(), authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.created(URI.create("/api/v1/tomas/" + toma.idToma()))
                .body(new ApiDataResponse<>(true, TomaAsistenciaResponse.from(toma)));
    }

    @GetMapping
    public ResponseEntity<ApiDataResponse<TomasSesionResponse>> consultar(@PathVariable final UUID sesionId) {
        return ResponseEntity.ok(new ApiDataResponse<>(true, TomasSesionResponse.from(consultarTomas.execute(
                new ConsultarTomasSesionInputPort.Consulta(sesionId, authenticatedUserResolver.requireAuthenticatedUserId())))));
    }
}
