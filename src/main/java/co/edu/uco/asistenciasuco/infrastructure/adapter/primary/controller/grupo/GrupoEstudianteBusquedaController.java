package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo;

import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.BuscarEstudiantePorDocumentoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.dto.BuscarEstudiantePorDocumentoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.dto.EstudianteBusquedaDTO;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpValidationMessages;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.UuidFieldValidationHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 A.2-15 (DEC-027): busqueda del estudiante a matricular por documento exacto en el grupo del docente titular.
 *
 * <p>{@code GET /api/v1/grupos/{grupoId}/estudiantes/busqueda?tipoIdentificacionId=&documento=}: 200 con los datos minimos
 * (sin el correo real) o {@code datos: null} si no hay resultado (el modal pasa a alta nueva y no hay 404 que revele nada);
 * 403 si el ejecutor no es el titular (SEC_002); 400 si el documento falta o no es un entero positivo. Solo DOCENTE en el
 * filtro HTTP. No pertenece al golden path.</p>
 */
@RestController
@RequestMapping("/api/v1/grupos")
public final class GrupoEstudianteBusquedaController {

    private final BuscarEstudiantePorDocumentoInputPort buscarEstudiantePorDocumentoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public GrupoEstudianteBusquedaController(
            final BuscarEstudiantePorDocumentoInputPort buscarEstudiantePorDocumentoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.buscarEstudiantePorDocumentoInputPort = Objects.requireNonNull(
                buscarEstudiantePorDocumentoInputPort, "BuscarEstudiantePorDocumentoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/{grupoId}/estudiantes/busqueda")
    public ResponseEntity<ApiDataResponse<EstudianteBusquedaDTO>> buscarEstudiantePorDocumento(
            @PathVariable final UUID grupoId,
            @RequestParam(name = "tipoIdentificacionId", required = false) final UUID tipoIdentificacionId,
            @RequestParam(name = "documento", required = false) final Integer documento
    ) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        UuidFieldValidationHelper.validateRequiredUuid(builder, "tipoIdentificacionId", tipoIdentificacionId,
                "El tipo de identificacion es obligatorio.", HttpValidationMessages.INVALID_UUID);
        if (documento == null) {
            builder.add("documento", ValidationErrorType.REQUIRED, "El numero de documento es obligatorio.");
        } else if (documento <= 0) {
            builder.add("documento", ValidationErrorType.OUT_OF_RANGE, HttpValidationMessages.INVALID_IDENTIFICATION_NUMBER);
        }
        RequestValidationGuard.validate(builder.build());
        final EstudianteBusquedaDTO estudiante = buscarEstudiantePorDocumentoInputPort.execute(new BuscarEstudiantePorDocumentoDTO(
                        grupoId, tipoIdentificacionId, documento, authenticatedUserResolver.requireAuthenticatedUserId()))
                .orElse(null);
        return ResponseEntity.ok(new ApiDataResponse<>(true, estudiante));
    }
}
