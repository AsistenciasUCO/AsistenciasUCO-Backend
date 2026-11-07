package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.ActualizarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto.ActualizarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto.CerrarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.ConsultarSesionesPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.dto.ConsultarSesionesPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.GenerarSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto.GenerarSesionesGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.mapper.SesionHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CerrarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.response.GeneracionSesionesGrupoResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.ConsultarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CrearSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.ActualizarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation.CerrarSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation.ConsultarSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation.CrearSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador primario REST para operaciones relacionadas con sesiones.
 */
@RestController
@RequestMapping("/api/v1/sesiones")
public final class SesionController {

    private static final CrearSesionRequestValidator CREATE_SESSION_VALIDATOR = new CrearSesionRequestValidator();
    private static final ConsultarSesionRequestValidator CONSULT_SESSION_VALIDATOR = new ConsultarSesionRequestValidator();
    private static final CerrarSesionRequestValidator CLOSE_SESSION_VALIDATOR = new CerrarSesionRequestValidator();

    private final CrearSesionInputPort crearSesionInputPort;
    private final ConsultarSesionInputPort consultarSesionInputPort;
    private final ConsultarSesionesPorGrupoInputPort consultarSesionesPorGrupoInputPort;
    private final CerrarSesionInputPort cerrarSesionInputPort;
    private final ActualizarSesionInputPort actualizarSesionInputPort;
    private final GenerarSesionesGrupoInputPort generarSesionesGrupoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public SesionController(
            final CrearSesionInputPort crearSesionInputPort,
            final ConsultarSesionInputPort consultarSesionInputPort,
            final ConsultarSesionesPorGrupoInputPort consultarSesionesPorGrupoInputPort,
            final CerrarSesionInputPort cerrarSesionInputPort,
            final ActualizarSesionInputPort actualizarSesionInputPort,
            final GenerarSesionesGrupoInputPort generarSesionesGrupoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.crearSesionInputPort = Objects.requireNonNull(crearSesionInputPort, "CrearSesionInputPort es obligatorio.");
        this.consultarSesionInputPort = Objects.requireNonNull(consultarSesionInputPort, "ConsultarSesionInputPort es obligatorio.");
        this.consultarSesionesPorGrupoInputPort = Objects.requireNonNull(
                consultarSesionesPorGrupoInputPort,
                "ConsultarSesionesPorGrupoInputPort es obligatorio."
        );
        this.cerrarSesionInputPort = Objects.requireNonNull(cerrarSesionInputPort, "CerrarSesionInputPort es obligatorio.");
        this.actualizarSesionInputPort = Objects.requireNonNull(actualizarSesionInputPort, "ActualizarSesionInputPort es obligatorio.");
        this.generarSesionesGrupoInputPort = Objects.requireNonNull(generarSesionesGrupoInputPort, "GenerarSesionesGrupoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping
    @AuditableOperation(action = "CREAR_SESION", resourceType = "GRUPO", resourceIdRequestField = "grupo")
    public ResponseEntity<ApiMessageResponse> crearSesion(@RequestBody final CrearSesionRequest request) {
        RequestValidationGuard.validate(CREATE_SESSION_VALIDATOR.validate(request));
        final CrearSesionDTO dto = SesionHttpMapper.toApplicationDTO(request, authenticatedUserResolver.requireAuthenticatedUserId());
        crearSesionInputPort.execute(dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessageResponse(true, "Sesion creada correctamente."));
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<ApiListResponse<SesionConsultadaDTO>> consultarSesionesPorGrupo(@PathVariable final UUID grupoId) {
        final ConsultarSesionesPorGrupoDTO dto = new ConsultarSesionesPorGrupoDTO(
                grupoId,
                authenticatedUserResolver.requireAuthenticatedUserId()
        );
        final List<SesionConsultadaDTO> sesiones = consultarSesionesPorGrupoInputPort.execute(dto);
        return ResponseEntity.ok(new ApiListResponse<>(true, sesiones, sesiones.size()));
    }

    @PostMapping("/grupo/{grupoId}/generacion")
    @AuditableOperation(action = "GENERAR_SESIONES_GRUPO", resourceType = "GRUPO", resourceIdPathVariable = "grupoId")
    public ResponseEntity<GeneracionSesionesGrupoResponse> generarSesionesGrupo(@PathVariable final UUID grupoId) {
        final var generacion = generarSesionesGrupoInputPort.execute(
                new GenerarSesionesGrupoDTO(grupoId, authenticatedUserResolver.requireAuthenticatedUserId())
        );
        final int canceladas = generacion == null ? 0 : generacion.sesionesCanceladasPorFestivo();
        final int conAsistencia = generacion == null ? 0 : generacion.sesionesFestivoConAsistencia();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GeneracionSesionesGrupoResponse(true, "Sesiones de grupo generadas correctamente.",
                        canceladas, conAsistencia));
    }

    @GetMapping("/{sesionId}")
    public ResponseEntity<ApiDataResponse<SesionConsultadaDTO>> consultarSesion(@PathVariable final UUID sesionId) {
        final ConsultarSesionRequest request = new ConsultarSesionRequest();
        request.setSesion(sesionId);
        return executeConsultarSesion(request);
    }

    @Deprecated(forRemoval = false)
    @PostMapping("/consultas")
    public ResponseEntity<ApiDataResponse<SesionConsultadaDTO>> consultarSesionLegacy(
            @RequestBody final ConsultarSesionRequest request
    ) {
        return executeConsultarSesion(request);
    }

    @PostMapping("/cierres")
    @AuditableOperation(action = "CERRAR_SESION", resourceType = "SESION", resourceIdRequestField = "sesion")
    public ResponseEntity<ApiMessageResponse> cerrarSesion(@RequestBody final CerrarSesionRequest request) {
        RequestValidationGuard.validate(CLOSE_SESSION_VALIDATOR.validate(request));
        final CerrarSesionDTO dto = SesionHttpMapper.toApplicationDTO(request, authenticatedUserResolver.requireAuthenticatedUserId());
        cerrarSesionInputPort.execute(dto);

        return ResponseEntity.ok(new ApiMessageResponse(true, "Sesion cerrada correctamente."));
    }

    @PatchMapping("/{sesionId}")
    public ResponseEntity<ApiDataResponse<Void>> actualizarSesion(
            @PathVariable final UUID sesionId,
            @RequestBody final ActualizarSesionRequest request
    ) {
        return executeActualizarSesion(sesionId, request);
    }

    /** Endpoint de compatibilidad; los consumidores deben migrar a PATCH. Mismo caso de uso. */
    @Deprecated(forRemoval = false)
    @PutMapping("/{sesionId}")
    public ResponseEntity<ApiDataResponse<Void>> actualizarSesionLegacy(
            @PathVariable final UUID sesionId,
            @RequestBody final ActualizarSesionRequest request
    ) {
        return executeActualizarSesion(sesionId, request);
    }

    private ResponseEntity<ApiDataResponse<Void>> executeActualizarSesion(
            final UUID sesionId,
            final ActualizarSesionRequest request
    ) {
        final ActualizarSesionDTO dto = SesionHttpMapper.toApplicationDTO(
                sesionId,
                request,
                authenticatedUserResolver.requireAuthenticatedUserId()
        );
        actualizarSesionInputPort.execute(dto);
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }

    private ResponseEntity<ApiDataResponse<SesionConsultadaDTO>> executeConsultarSesion(
            final ConsultarSesionRequest request
    ) {
        RequestValidationGuard.validate(CONSULT_SESSION_VALIDATOR.validate(request));
        final ConsultarSesionDTO dto = SesionHttpMapper.toApplicationDTO(request, authenticatedUserResolver.requireAuthenticatedUserId());
        final SesionConsultadaDTO sesion = consultarSesionInputPort.execute(dto);
        return ResponseEntity.ok(new ApiDataResponse<>(true, sesion));
    }

}
