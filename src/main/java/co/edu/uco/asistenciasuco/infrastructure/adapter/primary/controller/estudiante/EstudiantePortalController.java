package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
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
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EstudiantePortalController(
            final ConsultarMateriasEstudianteInputPort consultarMateriasEstudianteInputPort,
            final ConsultarHorariosEstudianteInputPort consultarHorariosEstudianteInputPort,
            final ConsultarSesionesMateriaEstudianteInputPort consultarSesionesMateriaEstudianteInputPort,
            final ConsultarReclamosEstudianteInputPort consultarReclamosEstudianteInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarMateriasEstudianteInputPort = consultarMateriasEstudianteInputPort;
        this.consultarHorariosEstudianteInputPort = consultarHorariosEstudianteInputPort;
        this.consultarSesionesMateriaEstudianteInputPort = consultarSesionesMateriaEstudianteInputPort;
        this.consultarReclamosEstudianteInputPort = consultarReclamosEstudianteInputPort;
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
            @PathVariable final UUID materiaId
    ) {
        final var data = consultarSesionesMateriaEstudianteInputPort.execute(
                authenticatedUserResolver.requireAuthenticatedUserId(),
                materiaId
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

    @PostMapping("/reclamos")
    public ResponseEntity<ApiDataResponse<Void>> crearReclamo() { throw unavailable("crear reclamo"); }
    @DeleteMapping("/reclamos/{id}")
    public ResponseEntity<ApiDataResponse<Void>> eliminarReclamo(@PathVariable final UUID id) { throw unavailable("eliminar reclamo"); }
    // HU176B: POST /matricular-grupo ya no es 501; lo atiende EstudianteMatriculaQrController.

    private FeatureUnavailableException unavailable(final String operation) {
        return new FeatureUnavailableException("La operacion de estudiante no esta disponible: " + operation + ".");
    }
}
