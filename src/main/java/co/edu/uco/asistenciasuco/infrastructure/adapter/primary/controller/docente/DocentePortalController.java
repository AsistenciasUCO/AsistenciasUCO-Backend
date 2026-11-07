package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.ResolverSolicitudRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.dto.ResolverSolicitudRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.ConsultarDetalleReclamoDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.ConsultarDetalleReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.DetalleReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.ConsultarReclamosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ConsultarReclamosDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamosDocentePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto.AsignaturaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto.HorarioDocenteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.mapper.DetalleReclamoDocenteHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.mapper.ReclamoDocenteHttpMapper;
import co.edu.uco.asistenciasuco.application.features.sesioncancelacion.cancelarsesion.primaryports.CancelarSesionInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.CancelarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ResolverReclamoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response.SesionCanceladaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.validation.CancelarSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response.DetalleReclamoDocenteResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response.ReclamoDocenteResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/docente")
public final class DocentePortalController {

    private final ConsultarHorariosDocenteInputPort consultarHorariosDocenteInputPort;
    private final ConsultarAsignaturasDocenteInputPort consultarAsignaturasDocenteInputPort;
    private final ResolverSolicitudRevisionAsistenciaInputPort resolverSolicitudRevisionAsistenciaInputPort;
    private final ConsultarReclamosDocenteInputPort consultarReclamosDocenteInputPort;
    private final ConsultarDetalleReclamoDocenteInputPort consultarDetalleReclamoDocenteInputPort;
    private final CancelarSesionInputPort cancelarSesionInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    private static final CancelarSesionRequestValidator CANCELAR_SESION_VALIDATOR = new CancelarSesionRequestValidator();

    public DocentePortalController(
            final ConsultarHorariosDocenteInputPort consultarHorariosDocenteInputPort,
            final ConsultarAsignaturasDocenteInputPort consultarAsignaturasDocenteInputPort,
            final ResolverSolicitudRevisionAsistenciaInputPort resolverSolicitudRevisionAsistenciaInputPort,
            final ConsultarReclamosDocenteInputPort consultarReclamosDocenteInputPort,
            final ConsultarDetalleReclamoDocenteInputPort consultarDetalleReclamoDocenteInputPort,
            final CancelarSesionInputPort cancelarSesionInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarHorariosDocenteInputPort = consultarHorariosDocenteInputPort;
        this.consultarAsignaturasDocenteInputPort = consultarAsignaturasDocenteInputPort;
        this.resolverSolicitudRevisionAsistenciaInputPort = resolverSolicitudRevisionAsistenciaInputPort;
        this.consultarReclamosDocenteInputPort = consultarReclamosDocenteInputPort;
        this.consultarDetalleReclamoDocenteInputPort = consultarDetalleReclamoDocenteInputPort;
        this.cancelarSesionInputPort = cancelarSesionInputPort;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    /**
     * Solicitudes de revision del docente titular (paginadas en SQL) con los datos minimos del soporte. El
     * soporte se descarga por {@code GET /api/v1/archivos/{fileId}} con Bearer; nunca por un enlace del storage.
     */
    @GetMapping("/reclamos")
    public ResponseEntity<ApiListResponse<ReclamoDocenteResponse>> consultarReclamos(
            @RequestParam(name = "estado", required = false) final String estado,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final ReclamosDocentePaginaDTO resultado = consultarReclamosDocenteInputPort.execute(
                new ConsultarReclamosDocenteDTO(authenticatedUserResolver.requireAuthenticatedUserId(), estado, pagina, tamano));
        return ResponseEntity.ok(new ApiListResponse<>(
                true,
                resultado.reclamos().stream().map(ReclamoDocenteHttpMapper::toResponse).toList(),
                resultado.totalRegistros()));
    }

    /**
     * Detalle de una solicitud de revision del docente titular, con su causal y el soporte adjunto (metadatos).
     * Una solicitud inexistente o de otro docente responde 404 sin distinguir ambos casos.
     */
    @GetMapping("/reclamos/{id}")
    public ResponseEntity<ApiDataResponse<DetalleReclamoDocenteResponse>> consultarDetalleReclamo(
            @PathVariable final UUID id
    ) {
        final DetalleReclamoDocenteDTO detalle = consultarDetalleReclamoDocenteInputPort.execute(
                new ConsultarDetalleReclamoDocenteDTO(id, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, DetalleReclamoDocenteHttpMapper.toResponse(detalle)));
    }

    @PatchMapping("/reclamos/{id}")
    public ResponseEntity<ApiDataResponse<Void>> resolverReclamo(
            @PathVariable final UUID id,
            @RequestBody final ResolverReclamoRequest request
    ) {
        resolverSolicitudRevisionAsistenciaInputPort.execute(new ResolverSolicitudRevisionAsistenciaDTO(
                id,
                request == null ? null : request.getAccion(),
                request == null ? null : request.getRespuestaDocente(),
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }

    @GetMapping("/horarios")
    public ResponseEntity<ApiListResponse<HorarioDocenteDTO>> consultarHorarios() {
        final var data = consultarHorariosDocenteInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    /**
     * CULM-001 B.1 (HU047): el docente titular cancela una sesion futura o sin asistencia, con un motivo. La titularidad
     * se valida en Application y en la DB; se avisa a los estudiantes activos y al canal SSE del grupo.
     */
    @PatchMapping("/sesiones/{sesionId}/cancelar")
    @AuditableOperation(action = "CANCELAR_SESION", resourceType = "SESION", resourceIdPathVariable = "sesionId")
    public ResponseEntity<ApiDataResponse<SesionCanceladaResponse>> cancelarSesion(
            @PathVariable final UUID sesionId,
            @RequestBody(required = false) final CancelarSesionRequest request
    ) {
        RequestValidationGuard.validate(CANCELAR_SESION_VALIDATOR.validate(request));
        final CancelarSesionInputPort.Resultado resultado = cancelarSesionInputPort.execute(new CancelarSesionInputPort.Comando(
                sesionId, request.getMotivo(), authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, SesionCanceladaResponse.from(resultado)));
    }

    @GetMapping("/asignaturas")
    public ResponseEntity<ApiListResponse<AsignaturaDocenteDTO>> consultarAsignaturas() {
        final var data = consultarAsignaturasDocenteInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
}
