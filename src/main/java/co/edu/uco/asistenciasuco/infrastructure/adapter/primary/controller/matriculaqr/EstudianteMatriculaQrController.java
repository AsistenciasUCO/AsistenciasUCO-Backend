package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.MatricularEstudianteAutenticadoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.NetworkClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.request.MatriculaQrRequests;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.validation.MatriculaQrRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * HU176B (CA-04): un estudiante con sesion iniciada se matricula con el token o PIN de la ventana. Reemplaza al antiguo
 * stub 501 de {@code POST /api/v1/estudiante/matricular-grupo}. El rol ESTUDIANTE lo exige {@code /api/v1/estudiante/**}.
 */
@RestController
@RequestMapping("/api/v1/estudiante")
public final class EstudianteMatriculaQrController {

    public record MatriculaGrupoResponse(UUID idGrupo) {
    }

    private static final MatriculaQrRequestValidator VALIDATOR = new MatriculaQrRequestValidator();

    private final MatricularEstudianteAutenticadoInputPort matricular;
    private final NetworkClientIpResolver ipResolver;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EstudianteMatriculaQrController(
            final MatricularEstudianteAutenticadoInputPort matricular,
            final NetworkClientIpResolver ipResolver,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.matricular = Objects.requireNonNull(matricular, "MatricularEstudianteAutenticadoInputPort es obligatorio.");
        this.ipResolver = Objects.requireNonNull(ipResolver, "NetworkClientIpResolver es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping("/matricular-grupo")
    public ResponseEntity<ApiDataResponse<MatriculaGrupoResponse>> matricularGrupo(
            @RequestBody final MatriculaQrRequests.Credencial request,
            final HttpServletRequest http
    ) {
        RequestValidationGuard.validate(VALIDATOR.validarCredencial(request));
        final var resultado = matricular.execute(new MatricularEstudianteAutenticadoInputPort.Comando(
                request.token(), request.pin(), ipResolver.resolve(http), authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, new MatriculaGrupoResponse(resultado.idGrupo())));
    }
}
