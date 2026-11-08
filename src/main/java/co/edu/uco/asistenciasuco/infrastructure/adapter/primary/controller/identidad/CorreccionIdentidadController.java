package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad;

import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.CorreccionIdentidadInputPort;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.ConfiguracionCorreccionIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.CorregirIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.ResultadoCorreccionIdentidadDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request.CorregirIdentidadRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.validation.CorregirIdentidadRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * HU159 (decision de John 2026-11-05): correccion de identidad por jerarquia. El estudiante lo corrige el coordinador
 * de su programa ({@code /coordinador/**}) y el decano el administrador ({@code /admin/**}); el SP revalida la
 * jerarquia y deja la auditoria. {@code /configuracion/identidad} (autenticado) indica si el documento es corregible.
 * Coordinador y docente se corrigen en sus endpoints de edicion (HU121 y HU092).
 */
@RestController
public final class CorreccionIdentidadController {

    static final String TIPO_ESTUDIANTE = "ESTUDIANTE";
    static final String TIPO_DECANO = "DECANO";
    private static final CorregirIdentidadRequestValidator VALIDATOR = new CorregirIdentidadRequestValidator();

    private final CorreccionIdentidadInputPort inputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CorreccionIdentidadController(
            final CorreccionIdentidadInputPort inputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.inputPort = Objects.requireNonNull(inputPort, "CorreccionIdentidadInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PutMapping("/api/v1/coordinador/estudiantes/{idEstudiante}/identidad")
    public ResponseEntity<ApiDataResponse<ResultadoCorreccionIdentidadDTO>> corregirEstudiante(
            @PathVariable final UUID idEstudiante,
            @RequestBody final CorregirIdentidadRequest request
    ) {
        return corregir(TIPO_ESTUDIANTE, idEstudiante, request);
    }

    @AuditableOperation(action = "CORREGIR_IDENTIDAD_DECANO", resourceType = "DECANO", resourceIdPathVariable = "id", metadataRequestFields = {"motivo"})
    @PutMapping("/api/v1/admin/decanos/{id}/identidad")
    public ResponseEntity<ApiDataResponse<ResultadoCorreccionIdentidadDTO>> corregirDecano(
            @PathVariable final UUID id,
            @RequestBody final CorregirIdentidadRequest request
    ) {
        return corregir(TIPO_DECANO, id, request);
    }

    @GetMapping("/api/v1/configuracion/identidad")
    public ResponseEntity<ApiDataResponse<ConfiguracionCorreccionIdentidadDTO>> configuracion() {
        return ResponseEntity.ok(new ApiDataResponse<>(true, inputPort.configuracion()));
    }

    private ResponseEntity<ApiDataResponse<ResultadoCorreccionIdentidadDTO>> corregir(
            final String tipoUsuario, final UUID idRegistro, final CorregirIdentidadRequest request) {
        RequestValidationGuard.validate(VALIDATOR.validate(request));
        return ResponseEntity.ok(new ApiDataResponse<>(true, inputPort.corregir(new CorregirIdentidadDTO(
                tipoUsuario,
                idRegistro,
                request.getIdTipoIdentificacion(),
                request.getNumeroIdentificacion(),
                trim(request.getPrimerNombre()),
                trim(request.getSegundoNombre()),
                trim(request.getPrimerApellido()),
                trim(request.getSegundoApellido()),
                trim(request.getMotivo()),
                authenticatedUserResolver.requireAuthenticatedUserId()))));
    }

    private static String trim(final String valor) {
        return valor == null ? null : valor.trim();
    }
}
