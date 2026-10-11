package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.ActualizarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto.ActualizarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.ContratoHorarioSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Escrituras v2 de sesiones (UTC-D01/D02/D03/D05/D08/D09): instantes RFC3339 con offset obligatorio,
 * normalizados una sola vez al instante UTC que se persiste en DATETIME2(7) con procedencia UTC_V2.
 *
 * <p>Reutiliza los mismos puertos de entrada que v1; el actor sale del JWT y no existe PUT v2.
 * Las lecturas v2 viven en {@link SesionV2ConsultaController}.</p>
 */
@RestController
@RequestMapping("/api/v2/sesiones")
public final class SesionV2Controller {

    private final CrearSesionInputPort crearSesionInputPort;
    private final ActualizarSesionInputPort actualizarSesionInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public SesionV2Controller(
            final CrearSesionInputPort crearSesionInputPort,
            final ActualizarSesionInputPort actualizarSesionInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.crearSesionInputPort = Objects.requireNonNull(crearSesionInputPort, "CrearSesionInputPort es obligatorio.");
        this.actualizarSesionInputPort = Objects.requireNonNull(actualizarSesionInputPort, "ActualizarSesionInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping
    @AuditableOperation(action = "CREAR_SESION", resourceType = "GRUPO", resourceIdRequestField = "grupo")
    public ResponseEntity<ApiMessageResponse> crearSesion(@RequestBody final Map<String, Object> body) {
        final SesionV2RequestParser.CrearSesionV2 request = SesionV2RequestParser.parseCrear(body);
        crearSesionInputPort.execute(new CrearSesionDTO(
                request.grupo(),
                request.nombre(),
                request.inicioUtc(),
                request.finUtc(),
                authenticatedUserResolver.requireAuthenticatedUserId(),
                ContratoHorarioSesion.UTC_CONFIRMADO_V2
        ));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessageResponse(true, "Sesion creada correctamente."));
    }

    /** Reemplazo completo de nombre y ambos instantes (sin PUT en v2, UTC-D09). */
    @PatchMapping("/{sesionId}")
    public ResponseEntity<ApiDataResponse<Void>> actualizarSesion(
            @PathVariable final UUID sesionId,
            @RequestBody final Map<String, Object> body
    ) {
        final SesionV2RequestParser.ActualizarSesionV2 request = SesionV2RequestParser.parseActualizar(body);
        actualizarSesionInputPort.execute(new ActualizarSesionDTO(
                sesionId,
                request.nombre(),
                request.inicioUtc(),
                request.finUtc(),
                authenticatedUserResolver.requireAuthenticatedUserId(),
                ContratoHorarioSesion.UTC_CONFIRMADO_V2
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }
}
