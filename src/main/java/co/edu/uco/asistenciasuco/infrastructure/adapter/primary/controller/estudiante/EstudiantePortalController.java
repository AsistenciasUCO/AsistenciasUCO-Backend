package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.primaryports.RetirarReclamoInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.primaryports.dto.RetirarReclamoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.ConsultarHorariosEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.ConsultarMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.ConsultarSesionesMateriaEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.dto.HorarioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.ConsultarReclamosEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ConsultarReclamosEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ReclamosEstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.mapper.ReclamoEstudianteHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.response.ReclamoEstudianteResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/estudiante")
public final class EstudiantePortalController {

    private final ConsultarMateriasEstudianteInputPort consultarMateriasEstudianteInputPort;
    private final ConsultarHorariosEstudianteInputPort consultarHorariosEstudianteInputPort;
    private final ConsultarSesionesMateriaEstudianteInputPort consultarSesionesMateriaEstudianteInputPort;
    private final ConsultarReclamosEstudianteInputPort consultarReclamosEstudianteInputPort;
    private final RetirarReclamoInputPort retirarReclamoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EstudiantePortalController(
            final ConsultarMateriasEstudianteInputPort consultarMateriasEstudianteInputPort,
            final ConsultarHorariosEstudianteInputPort consultarHorariosEstudianteInputPort,
            final ConsultarSesionesMateriaEstudianteInputPort consultarSesionesMateriaEstudianteInputPort,
            final ConsultarReclamosEstudianteInputPort consultarReclamosEstudianteInputPort,
            final RetirarReclamoInputPort retirarReclamoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarMateriasEstudianteInputPort = consultarMateriasEstudianteInputPort;
        this.consultarHorariosEstudianteInputPort = consultarHorariosEstudianteInputPort;
        this.consultarSesionesMateriaEstudianteInputPort = consultarSesionesMateriaEstudianteInputPort;
        this.consultarReclamosEstudianteInputPort = consultarReclamosEstudianteInputPort;
        this.retirarReclamoInputPort = retirarReclamoInputPort;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/materias")
    public ResponseEntity<ApiListResponse<MateriaEstudianteDTO>> consultarMaterias() {
        final var data = consultarMateriasEstudianteInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @GetMapping("/horarios")
    public ResponseEntity<ApiListResponse<HorarioEstudianteDTO>> consultarHorarios() {
        final var data = consultarHorariosEstudianteInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @GetMapping("/materias/{materiaId}/sesiones")
    public ResponseEntity<ApiListResponse<SesionMateriaEstudianteResponse>> consultarSesionesMateria(
            @PathVariable final UUID materiaId,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final var data = consultarSesionesMateriaEstudianteInputPort.execute(
                authenticatedUserResolver.requireAuthenticatedUserId(),
                materiaId,
                pagina,
                tamano
        ).stream().map(SesionMateriaEstudianteResponse::from).toList();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    /**
     * HU010/HU011: solicitudes de revision del estudiante autenticado (paginadas en SQL, ownership en la DB) con los
     * datos minimos del soporte. El soporte se descarga por {@code GET /api/v1/archivos/{fileId}} con Bearer.
     */
    @GetMapping("/reclamos")
    public ResponseEntity<ApiListResponse<ReclamoEstudianteResponse>> consultarReclamos(
            @RequestParam(name = "estado", required = false) final String estado,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final ReclamosEstudiantePaginaDTO resultado = consultarReclamosEstudianteInputPort.execute(
                new ConsultarReclamosEstudianteDTO(authenticatedUserResolver.requireAuthenticatedUserId(), estado, pagina, tamano));
        return ResponseEntity.ok(new ApiListResponse<>(
                true,
                resultado.reclamos().stream().map(ReclamoEstudianteHttpMapper::toResponse).toList(),
                resultado.totalRegistros()));
    }

    /**
     * CULM-001 A.2-7 (HU010/HU012): el estudiante retira su reclamo pendiente. 404 si no existe o es ajeno (REV_006), 409
     * si ya no esta pendiente (REV_007). El reclamo se radica por {@code POST /api/v1/asistencias/revisiones}: el antiguo
     * {@code POST /estudiante/reclamos} (501) se retiro (A.2-14).
     */
    @DeleteMapping("/reclamos/{id}")
    @AuditableOperation(action = "RETIRAR_SOLICITUD_REVISION", resourceType = "SOLICITUD_REVISION", resourceIdPathVariable = "id")
    public ResponseEntity<ApiMessageResponse> retirarReclamo(@PathVariable final UUID id) {
        final String mensaje = retirarReclamoInputPort.execute(
                new RetirarReclamoDTO(id, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiMessageResponse(true, mensaje));
    }
    // HU176B: POST /matricular-grupo ya no es 501; lo atiende EstudianteMatriculaQrController.
}
