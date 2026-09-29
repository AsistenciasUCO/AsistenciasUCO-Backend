package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.ActualizarGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.ConsultarGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.ConsultarEstudiantesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.ConsultarEstudiantesGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.EstudianteGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.CrearGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.mapper.GrupoHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.mapper.RegistrarEstudianteHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.ActualizarGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.CrearGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.RegistrarEstudianteRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.validation.RegistrarEstudianteRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
 * Adaptador primario REST para grupos academicos.
 */
@RestController
@RequestMapping("/api/v1/grupos")
public final class GrupoController {

    private static final RegistrarEstudianteRequestValidator REQUEST_VALIDATOR = new RegistrarEstudianteRequestValidator();

    private final CrearGrupoInputPort crearGrupoInputPort;
    private final ActualizarGrupoInputPort actualizarGrupoInputPort;
    private final RegistrarEstudianteInputPort registrarEstudianteInputPort;
    private final ConsultarGruposInputPort consultarGruposInputPort;
    private final ConsultarEstudiantesGrupoInputPort consultarEstudiantesGrupoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public GrupoController(
            final CrearGrupoInputPort crearGrupoInputPort,
            final ActualizarGrupoInputPort actualizarGrupoInputPort,
            final RegistrarEstudianteInputPort registrarEstudianteInputPort,
            final ConsultarGruposInputPort consultarGruposInputPort,
            final ConsultarEstudiantesGrupoInputPort consultarEstudiantesGrupoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.crearGrupoInputPort = Objects.requireNonNull(crearGrupoInputPort, "CrearGrupoInputPort es obligatorio.");
        this.actualizarGrupoInputPort = Objects.requireNonNull(actualizarGrupoInputPort, "ActualizarGrupoInputPort es obligatorio.");
        this.registrarEstudianteInputPort = Objects.requireNonNull(registrarEstudianteInputPort, "RegistrarEstudianteInputPort es obligatorio.");
        this.consultarGruposInputPort = Objects.requireNonNull(consultarGruposInputPort, "ConsultarGruposInputPort es obligatorio.");
        this.consultarEstudiantesGrupoInputPort = Objects.requireNonNull(
                consultarEstudiantesGrupoInputPort,
                "ConsultarEstudiantesGrupoInputPort es obligatorio."
        );
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiListResponse<GrupoDTO>> listarGrupos() {
        final List<GrupoDTO> grupos = consultarGruposInputPort.execute();
        return ResponseEntity.ok(new ApiListResponse<>(true, grupos, grupos.size()));
    }

    @PostMapping
    public ResponseEntity<ApiDataResponse<CrearGrupoResultadoDTO>> crearGrupo(
            @RequestBody final CrearGrupoRequest request
    ) {
        final CrearGrupoResultadoDTO resultado = crearGrupoInputPort.execute(
                GrupoHttpMapper.toApplicationDTO(request, authenticatedUserResolver.requireAuthenticatedUserId())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, resultado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiDataResponse<ActualizarGrupoResultadoDTO>> actualizarGrupo(
            @PathVariable final UUID id,
            @RequestBody final ActualizarGrupoRequest request
    ) {
        final ActualizarGrupoResultadoDTO resultado = actualizarGrupoInputPort.execute(
                GrupoHttpMapper.toApplicationDTO(id, request, authenticatedUserResolver.requireAuthenticatedUserId())
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, resultado));
    }

    @GetMapping("/{grupoId}/estudiantes")
    public ResponseEntity<ApiListResponse<EstudianteGrupoDTO>> listarEstudiantesGrupo(
            @PathVariable final UUID grupoId
    ) {
        final List<EstudianteGrupoDTO> estudiantes = consultarEstudiantesGrupoInputPort.execute(
                new ConsultarEstudiantesGrupoDTO(grupoId, authenticatedUserResolver.requireAuthenticatedUserId())
        );
        return ResponseEntity.ok(new ApiListResponse<>(true, estudiantes, estudiantes.size()));
    }

    @PostMapping("/{grupoId}/estudiantes")
    @AuditableOperation(action = "REGISTRAR_ESTUDIANTE_EN_GRUPO", resourceType = "GRUPO", resourceIdPathVariable = "grupoId")
    public ResponseEntity<RegistrarEstudianteResultadoDTO> matricularEstudiante(
            @PathVariable final UUID grupoId,
            @RequestBody final RegistrarEstudianteRequest request
    ) {
        RequestValidationGuard.validate(REQUEST_VALIDATOR.validate(grupoId, request));
        final RegistrarEstudianteResultadoDTO resultado = registrarEstudianteInputPort.execute(
                RegistrarEstudianteHttpMapper.toApplicationDTO(
                        grupoId,
                        request,
                        authenticatedUserResolver.requireAuthenticatedUserId()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @DeleteMapping("/{grupoId}/estudiantes/{estudianteId}")
    public ResponseEntity<ApiDataResponse<Boolean>> retirarEstudiante(
            @PathVariable final UUID grupoId,
            @PathVariable final UUID estudianteId
    ) {
        throw new FeatureUnavailableException(
                "El retiro de estudiantes del grupo requiere un command publico de DB que aun no esta disponible."
        );
    }
}
