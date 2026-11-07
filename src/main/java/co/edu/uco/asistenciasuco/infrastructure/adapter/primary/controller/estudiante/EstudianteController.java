package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.ConsultarEstudiantePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.ConsultarEstudiantePorIdDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.EstudianteDetalleDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.ConsultarEstudiantesInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.ConsultarEstudiantesDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.mapper.EstudianteHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.request.ConsultarEstudiantesRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.validation.ConsultarEstudiantesRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/estudiantes")
public final class EstudianteController {

    private static final ConsultarEstudiantesRequestValidator CONSULT_STUDENTS_VALIDATOR =
            new ConsultarEstudiantesRequestValidator();

    private final ConsultarEstudiantesInputPort consultarEstudiantesInputPort;
    private final ConsultarEstudiantePorIdInputPort consultarEstudiantePorIdInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EstudianteController(
            final ConsultarEstudiantesInputPort consultarEstudiantesInputPort,
            final ConsultarEstudiantePorIdInputPort consultarEstudiantePorIdInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarEstudiantesInputPort = Objects.requireNonNull(consultarEstudiantesInputPort, "El puerto de entrada ConsultarEstudiantesInputPort es obligatorio.");
        this.consultarEstudiantePorIdInputPort = Objects.requireNonNull(consultarEstudiantePorIdInputPort, "El puerto de entrada ConsultarEstudiantePorIdInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<EstudiantePaginaDTO> consultarEstudiantes(
            @RequestParam(required = false) final UUID tipoIdentificacionId,
            @RequestParam(required = false) final Integer numeroIdentificacion,
            @RequestParam(required = false) final String nombre,
            @RequestParam(required = false) final String correo,
            @RequestParam(required = false) final UUID institucionId,
            @RequestParam(required = false) final UUID facultadId,
            @RequestParam(required = false) final UUID programaId,
            @RequestParam(required = false) final UUID grupoId,
            @RequestParam(required = false) final Boolean activo,
            @RequestParam(defaultValue = "0") final Integer page,
            @RequestParam(defaultValue = "20") final Integer size
    ) {
        final ConsultarEstudiantesRequest request = new ConsultarEstudiantesRequest(
                tipoIdentificacionId,
                numeroIdentificacion,
                nombre,
                correo,
                institucionId,
                facultadId,
                programaId,
                grupoId,
                activo,
                page,
                size
        );
        RequestValidationGuard.validate(CONSULT_STUDENTS_VALIDATOR.validate(request));
        final ConsultarEstudiantesDTO dto = EstudianteHttpMapper.toApplicationDTO(
                request, authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(consultarEstudiantesInputPort.execute(dto));
    }

    @GetMapping("/{estudianteId}")
    public ResponseEntity<EstudianteDetalleDTO> consultarEstudiantePorId(@PathVariable final UUID estudianteId) {
        RequestValidationGuard.validate(CONSULT_STUDENTS_VALIDATOR.validateEstudianteId(estudianteId));
        return ResponseEntity.ok(consultarEstudiantePorIdInputPort.execute(
                new ConsultarEstudiantePorIdDTO(estudianteId, authenticatedUserResolver.requireAuthenticatedUserId())));
    }
}
