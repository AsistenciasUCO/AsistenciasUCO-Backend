package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.ResolverSolicitudRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.dto.ResolverSolicitudRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.ConsultarReclamosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ConsultarReclamosDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamosDocentePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto.AsignaturaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto.HorarioDocenteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ResolverReclamoRequest;
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
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DocentePortalController(
            final ConsultarHorariosDocenteInputPort consultarHorariosDocenteInputPort,
            final ConsultarAsignaturasDocenteInputPort consultarAsignaturasDocenteInputPort,
            final ResolverSolicitudRevisionAsistenciaInputPort resolverSolicitudRevisionAsistenciaInputPort,
            final ConsultarReclamosDocenteInputPort consultarReclamosDocenteInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarHorariosDocenteInputPort = consultarHorariosDocenteInputPort;
        this.consultarAsignaturasDocenteInputPort = consultarAsignaturasDocenteInputPort;
        this.resolverSolicitudRevisionAsistenciaInputPort = resolverSolicitudRevisionAsistenciaInputPort;
        this.consultarReclamosDocenteInputPort = consultarReclamosDocenteInputPort;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    /**
     * Solicitudes de revision del docente titular (paginadas en SQL) con los datos minimos del soporte. El
     * soporte se descarga por {@code GET /api/v1/archivos/{fileId}} con Bearer; nunca por un enlace del storage.
     */
    @GetMapping("/reclamos")
    public ResponseEntity<ApiListResponse<ReclamoDocenteDTO>> consultarReclamos(
            @RequestParam(name = "estado", required = false) final String estado,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final ReclamosDocentePaginaDTO resultado = consultarReclamosDocenteInputPort.execute(
                new ConsultarReclamosDocenteDTO(authenticatedUserResolver.requireAuthenticatedUserId(), estado, pagina, tamano));
        return ResponseEntity.ok(new ApiListResponse<>(true, resultado.reclamos(), resultado.totalRegistros()));
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

    @PatchMapping("/sesiones/{sesionId}/cancelar")
    public ResponseEntity<ApiDataResponse<Void>> cancelarSesion(@PathVariable final UUID sesionId) {
        throw unavailable("cancelar sesion");
    }

    @GetMapping("/asignaturas")
    public ResponseEntity<ApiListResponse<AsignaturaDocenteDTO>> consultarAsignaturas() {
        final var data = consultarAsignaturasDocenteInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    private FeatureUnavailableException unavailable(final String operation) {
        return new FeatureUnavailableException("La operacion docente no esta disponible: " + operation + ".");
    }
}
