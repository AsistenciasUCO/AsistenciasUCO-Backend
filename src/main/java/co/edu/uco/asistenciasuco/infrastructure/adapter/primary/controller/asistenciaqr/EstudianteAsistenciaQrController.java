package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistenciaqr;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.primaryports.RegistrarAsistenciaPorTokenInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistenciaqr.request.RegistrarAsistenciaQrRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistenciaqr.response.AsistenciaQrHttpResponses.RegistroAsistenciaQrResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistenciaqr.validation.RegistrarAsistenciaQrRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.NetworkClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * CULM-001 B.2 (HU176, contrato 3.2.7): el estudiante autenticado registra su asistencia con el token del QR o con el PIN
 * desde la red del campus. 201 si el registro es nuevo y 200 si ya existia (idempotente). Reemplaza al antiguo
 * {@code AsistenciaQrController} (codigo estatico de la sesion).
 */
@RestController
@RequestMapping("/api/v1/estudiante")
public final class EstudianteAsistenciaQrController {

    public static final String RUTA = "/api/v1/estudiante/asistencia-qr";
    private static final RegistrarAsistenciaQrRequestValidator VALIDATOR = new RegistrarAsistenciaQrRequestValidator();

    private final RegistrarAsistenciaPorTokenInputPort registrar;
    private final NetworkClientIpResolver ipResolver;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EstudianteAsistenciaQrController(
            final RegistrarAsistenciaPorTokenInputPort registrar,
            final NetworkClientIpResolver ipResolver,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.registrar = Objects.requireNonNull(registrar, "RegistrarAsistenciaPorTokenInputPort es obligatorio.");
        this.ipResolver = Objects.requireNonNull(ipResolver, "NetworkClientIpResolver es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping("/asistencia-qr")
    @AuditableOperation(action = "REGISTRAR_ASISTENCIA_POR_TOKEN", resourceType = "TOMA_ASISTENCIA")
    public ResponseEntity<ApiDataResponse<RegistroAsistenciaQrResponse>> registrar(
            @RequestBody(required = false) final RegistrarAsistenciaQrRequest request,
            final HttpServletRequest http
    ) {
        RequestValidationGuard.validate(VALIDATOR.validar(request));
        final RegistrarAsistenciaPorTokenInputPort.Resultado resultado = registrar.execute(new RegistrarAsistenciaPorTokenInputPort.Comando(
                request.getToken(), request.getPin(), ipResolver.resolve(http), authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.status(resultado.yaRegistrado() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(new ApiDataResponse<>(true, RegistroAsistenciaQrResponse.from(resultado)));
    }
}
