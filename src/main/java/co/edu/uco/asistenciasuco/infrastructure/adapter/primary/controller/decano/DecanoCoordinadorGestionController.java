package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.ActualizarCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.dto.ActualizarCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.CambiarEstadoCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.dto.CambiarEstadoCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ResultadoGestionCoordinadorDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.ActualizarCoordinadorRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.CambiarEstadoCoordinadorRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.response.ResultadoGestionCoordinadorResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.validation.ActualizarCoordinadorRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.validation.CambiarEstadoCoordinadorRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * HU121/HU122: edicion y cambio de estado del coordinador por el decano. Vive en un controlador propio (D10) con
 * la misma base de ruta que {@link DecanoPortalController} para no chocar con otros work items sobre ese archivo.
 * Cubierto por el comodin {@code /api/v1/decano/**} de {@code SecurityConfig}; el ambito lo fuerza el caso de uso.
 */
@RestController
@RequestMapping("/api/v1/decano/coordinadores")
public final class DecanoCoordinadorGestionController {

    private static final ActualizarCoordinadorRequestValidator ACTUALIZAR_VALIDATOR = new ActualizarCoordinadorRequestValidator();
    private static final CambiarEstadoCoordinadorRequestValidator CAMBIAR_ESTADO_VALIDATOR = new CambiarEstadoCoordinadorRequestValidator();

    private final ActualizarCoordinadorInputPort actualizarCoordinadorInputPort;
    private final CambiarEstadoCoordinadorInputPort cambiarEstadoCoordinadorInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DecanoCoordinadorGestionController(
            final ActualizarCoordinadorInputPort actualizarCoordinadorInputPort,
            final CambiarEstadoCoordinadorInputPort cambiarEstadoCoordinadorInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.actualizarCoordinadorInputPort = actualizarCoordinadorInputPort;
        this.cambiarEstadoCoordinadorInputPort = cambiarEstadoCoordinadorInputPort;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiDataResponse<ResultadoGestionCoordinadorResponse>> actualizarCoordinador(
            @PathVariable final UUID id,
            @RequestBody final ActualizarCoordinadorRequest request
    ) {
        RequestValidationGuard.validate(ACTUALIZAR_VALIDATOR.validate(request));
        final ResultadoGestionCoordinadorDTO resultado = actualizarCoordinadorInputPort.execute(new ActualizarCoordinadorDTO(
                id,
                request.getPrimerNombre(),
                request.getSegundoNombre(),
                request.getPrimerApellido(),
                request.getSegundoApellido(),
                request.getCorreo(),
                authenticatedUserResolver.requireAuthenticatedUserId(),
                request.getIdTipoIdentificacion(),
                request.getNumeroIdentificacion(),
                request.getMotivo()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, toResponse(resultado)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiDataResponse<ResultadoGestionCoordinadorResponse>> cambiarEstadoCoordinador(
            @PathVariable final UUID id,
            @RequestBody final CambiarEstadoCoordinadorRequest request
    ) {
        RequestValidationGuard.validate(CAMBIAR_ESTADO_VALIDATOR.validate(request));
        final ResultadoGestionCoordinadorDTO resultado = cambiarEstadoCoordinadorInputPort.execute(new CambiarEstadoCoordinadorDTO(
                id,
                request != null && Boolean.TRUE.equals(request.getEstaActivo()),
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, toResponse(resultado)));
    }

    private static ResultadoGestionCoordinadorResponse toResponse(final ResultadoGestionCoordinadorDTO dto) {
        return new ResultadoGestionCoordinadorResponse(dto.idCoordinador(), dto.sincronizacionIdentidad(), dto.mensajeUsuario());
    }
}
