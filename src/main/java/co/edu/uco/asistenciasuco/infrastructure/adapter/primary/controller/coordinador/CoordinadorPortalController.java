package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.AsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.EstudianteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.PeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.PlanEstudioDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.primaryports.ConsultarAsignaturasInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.primaryports.ConsultarAsignaturasPlanInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.ConsultarEstudiantesProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.ConsultarPeriodosAcademicosInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.ConsultarPlanesEstudioInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.GestionarAsignaturaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto.GuardarAsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.GestionarPlanEstudioInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.dto.GuardarPlanEstudioDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.GuardarAsignaturaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.GuardarPlanEstudioRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coordinador")
public final class CoordinadorPortalController {

    private final ConsultarPlanesEstudioInputPort consultarPlanesEstudioInputPort;
    private final ConsultarAsignaturasPlanInputPort consultarAsignaturasPlanInputPort;
    private final ConsultarAsignaturasInputPort consultarAsignaturasInputPort;
    private final ConsultarPeriodosAcademicosInputPort consultarPeriodosAcademicosInputPort;
    private final ConsultarEstudiantesProgramaInputPort consultarEstudiantesProgramaInputPort;
    private final GestionarPlanEstudioInputPort gestionarPlanEstudioInputPort;
    private final GestionarAsignaturaInputPort gestionarAsignaturaInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CoordinadorPortalController(
            final ConsultarPlanesEstudioInputPort consultarPlanesEstudioInputPort,
            final ConsultarAsignaturasPlanInputPort consultarAsignaturasPlanInputPort,
            final ConsultarAsignaturasInputPort consultarAsignaturasInputPort,
            final ConsultarPeriodosAcademicosInputPort consultarPeriodosAcademicosInputPort,
            final ConsultarEstudiantesProgramaInputPort consultarEstudiantesProgramaInputPort,
            final GestionarPlanEstudioInputPort gestionarPlanEstudioInputPort,
            final GestionarAsignaturaInputPort gestionarAsignaturaInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarPlanesEstudioInputPort = consultarPlanesEstudioInputPort;
        this.consultarAsignaturasPlanInputPort = consultarAsignaturasPlanInputPort;
        this.consultarAsignaturasInputPort = consultarAsignaturasInputPort;
        this.consultarPeriodosAcademicosInputPort = consultarPeriodosAcademicosInputPort;
        this.consultarEstudiantesProgramaInputPort = consultarEstudiantesProgramaInputPort;
        this.gestionarPlanEstudioInputPort = gestionarPlanEstudioInputPort;
        this.gestionarAsignaturaInputPort = gestionarAsignaturaInputPort;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/docentes")
    public ResponseEntity<ApiListResponse<Void>> consultarDocentesPrograma() { throw unavailable(); }
    @PostMapping("/docentes")
    public ResponseEntity<ApiDataResponse<Void>> crearDocente() { throw unavailable(); }
    @PatchMapping("/docentes/{id}/estado")
    public ResponseEntity<ApiDataResponse<Void>> toggleEstadoDocente(@PathVariable final UUID id) { throw unavailable(); }

    @GetMapping("/planes-estudio")
    public ResponseEntity<ApiListResponse<PlanEstudioDTO>> consultarPlanesEstudio() {
        final var data = consultarPlanesEstudioInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
    @PostMapping("/planes-estudio")
    public ResponseEntity<ApiDataResponse<Void>> crearPlanEstudio(@RequestBody final GuardarPlanEstudioRequest request) {
        gestionarPlanEstudioInputPort.guardar(new GuardarPlanEstudioDTO(
                null,
                request == null ? null : request.getInp(),
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }
    @PutMapping("/planes-estudio/{id}")
    public ResponseEntity<ApiDataResponse<Void>> actualizarPlanEstudio(
            @PathVariable final UUID id,
            @RequestBody final GuardarPlanEstudioRequest request
    ) {
        gestionarPlanEstudioInputPort.guardar(new GuardarPlanEstudioDTO(
                id,
                request == null ? null : request.getInp(),
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }
    @PatchMapping("/planes-estudio/{id}/toggle-estado")
    public ResponseEntity<ApiDataResponse<Void>> toggleEstadoPlanEstudio(@PathVariable final UUID id) { throw unavailable(); }
    @PostMapping("/planes-estudio/{id}/semestres")
    public ResponseEntity<ApiDataResponse<Void>> agregarSemestrePlan(@PathVariable final UUID id) { throw unavailable(); }
    @DeleteMapping("/planes-estudio/{id}/semestres/{numero}")
    public ResponseEntity<ApiDataResponse<Void>> eliminarSemestrePlan(@PathVariable final UUID id, @PathVariable final Integer numero) { throw unavailable(); }

    @GetMapping("/planes-estudio/{id}/asignaturas")
    public ResponseEntity<ApiListResponse<AsignaturaDTO>> consultarAsignaturasPlan(@PathVariable final UUID id) {
        final var data = consultarAsignaturasPlanInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId(), id);
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
    @PostMapping("/planes-estudio/{id}/asignaturas")
    public ResponseEntity<ApiDataResponse<Void>> crearAsignaturaPlan(
            @PathVariable final UUID id,
            @RequestBody final GuardarAsignaturaRequest request
    ) {
        gestionarAsignaturaInputPort.crear(
                toAsignaturaDTO(null, id, request, authenticatedUserResolver.requireAuthenticatedUserId())
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }
    @PutMapping("/planes-estudio/{planId}/asignaturas/{asigId}")
    public ResponseEntity<ApiDataResponse<Void>> actualizarAsignaturaPlan(
            @PathVariable final UUID planId,
            @PathVariable final UUID asigId,
            @RequestBody final GuardarAsignaturaRequest request
    ) {
        gestionarAsignaturaInputPort.actualizar(toAsignaturaDTO(
                asigId, planId, request, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }
    @DeleteMapping("/planes-estudio/{planId}/asignaturas/{asigId}")
    public ResponseEntity<ApiDataResponse<Void>> eliminarAsignaturaPlan(@PathVariable final UUID planId, @PathVariable final UUID asigId) { throw unavailable(); }

    @GetMapping("/asignaturas")
    public ResponseEntity<ApiListResponse<AsignaturaDTO>> listarTodasAsignaturas() {
        final var data = consultarAsignaturasInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
    @PatchMapping("/asignaturas/{id}/estado")
    public ResponseEntity<ApiDataResponse<Void>> toggleEstadoAsignatura(@PathVariable final UUID id) {
        gestionarAsignaturaInputPort.toggleEstado(id, authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }

    @GetMapping("/periodos-academicos")
    public ResponseEntity<ApiListResponse<PeriodoAcademicoDTO>> consultarPeriodosAcademicos() {
        final var data = consultarPeriodosAcademicosInputPort.execute();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
    @PostMapping("/periodos-academicos")
    public ResponseEntity<ApiDataResponse<Void>> crearPeriodoAcademico() { throw unavailable(); }
    @PutMapping("/periodos-academicos/{id}")
    public ResponseEntity<ApiDataResponse<Void>> actualizarPeriodoAcademico(@PathVariable final UUID id) { throw unavailable(); }
    @PatchMapping("/periodos-academicos/{id}/estado")
    public ResponseEntity<ApiDataResponse<Void>> toggleEstadoPeriodoAcademico(@PathVariable final UUID id) { throw unavailable(); }

    @GetMapping("/estudiantes")
    public ResponseEntity<ApiListResponse<EstudianteProgramaDTO>> consultarEstudiantesPrograma() {
        final var data = consultarEstudiantesProgramaInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
    @GetMapping("/solicitudes-matricula")
    public ResponseEntity<ApiListResponse<Void>> consultarSolicitudesMatricula() { throw unavailable(); }
    @PatchMapping("/solicitudes-matricula/{id}")
    public ResponseEntity<ApiDataResponse<Void>> resolverSolicitudMatricula(@PathVariable final UUID id) { throw unavailable(); }
    @PostMapping("/grupos/{grupoId}/estudiantes")
    public ResponseEntity<ApiDataResponse<Void>> matricularEstudianteEnGrupo(@PathVariable final UUID grupoId) { throw unavailable(); }
    @DeleteMapping("/grupos/{grupoId}/estudiantes/{estudianteId}")
    public ResponseEntity<ApiDataResponse<Void>> retirarEstudianteDeGrupo(@PathVariable final UUID grupoId, @PathVariable final UUID estudianteId) { throw unavailable(); }

    private static FeatureUnavailableException unavailable() {
        return new FeatureUnavailableException("La operacion de coordinador no esta disponible con el contrato DB publico actual.");
    }

    private static GuardarAsignaturaDTO toAsignaturaDTO(
            final UUID asignaturaId,
            final UUID planId,
            final GuardarAsignaturaRequest request,
            final UUID usuarioEjecutor
    ) {
        return new GuardarAsignaturaDTO(
                asignaturaId,
                planId,
                request == null ? null : request.getCodigo(),
                request == null ? null : request.getNombre(),
                request == null ? null : request.getCreditos(),
                request == null ? null : request.getSemestreNumero(),
                request == null ? null : request.getNombreArea(),
                request == null ? null : request.getNombreComponente(),
                usuarioEjecutor
        );
    }
}
