package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.RegistrarEstudiantePorTokenInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.ValidarTokenMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.NetworkClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.request.MatriculaQrRequests;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.validation.MatriculaQrRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU176B: endpoints PUBLICOS (sin JWT) de la matricula por QR/PIN. Son la unica apertura publica de negocio y se declaran
 * una por una en {@code SecurityConfig}. Antes de llegar aqui actua el filtro de limite de intentos; la red del campus y el
 * interruptor los aplica el caso de uso. Los cuerpos jamas se registran en logs.
 */
@RestController
@RequestMapping("/api/v1/public/matricula-qr")
public final class MatriculaQrPublicaController {

    public static final String RUTA_VALIDAR = "/api/v1/public/matricula-qr/validar";
    public static final String RUTA_REGISTRO = "/api/v1/public/matricula-qr/registro";

    public record CursoMatriculaQrResponse(
            UUID idGrupo,
            String nombreAsignatura,
            String nombreGrupo,
            String nombreDocente,
            Instant fechaExpiracionUtc,
            int cupoDisponible,
            List<TipoIdentificacionDTO> tiposIdentificacion
    ) {
    }

    /** {@code usuario} es el numero de identificacion con el que inicia sesion; no se devuelve ningun token de sesion. */
    public record MatriculaQrResultadoResponse(UUID idGrupo, String usuario) {
    }

    private static final MatriculaQrRequestValidator VALIDATOR = new MatriculaQrRequestValidator();

    private final ValidarTokenMatriculaInputPort validarToken;
    private final RegistrarEstudiantePorTokenInputPort registrarPorToken;
    private final ConsultarTiposIdentificacionInputPort tiposIdentificacion;
    private final NetworkClientIpResolver ipResolver;

    public MatriculaQrPublicaController(
            final ValidarTokenMatriculaInputPort validarToken,
            final RegistrarEstudiantePorTokenInputPort registrarPorToken,
            final ConsultarTiposIdentificacionInputPort tiposIdentificacion,
            final NetworkClientIpResolver ipResolver
    ) {
        this.validarToken = Objects.requireNonNull(validarToken, "ValidarTokenMatriculaInputPort es obligatorio.");
        this.registrarPorToken = Objects.requireNonNull(registrarPorToken, "RegistrarEstudiantePorTokenInputPort es obligatorio.");
        this.tiposIdentificacion = Objects.requireNonNull(tiposIdentificacion, "ConsultarTiposIdentificacionInputPort es obligatorio.");
        this.ipResolver = Objects.requireNonNull(ipResolver, "NetworkClientIpResolver es obligatorio.");
    }

    @PostMapping("/validar")
    public ResponseEntity<ApiDataResponse<CursoMatriculaQrResponse>> validar(
            @RequestBody final MatriculaQrRequests.Credencial request,
            final HttpServletRequest http
    ) {
        RequestValidationGuard.validate(VALIDATOR.validarCredencial(request));
        final var curso = validarToken.execute(
                new ValidarTokenMatriculaInputPort.Comando(request.token(), request.pin(), ipResolver.resolve(http)));
        return ResponseEntity.ok(new ApiDataResponse<>(true, new CursoMatriculaQrResponse(
                curso.idGrupo(), curso.nombreAsignatura(), curso.nombreGrupo(), curso.nombreDocente(),
                curso.fechaExpiracionUtc(), curso.cupoDisponible(),
                // El catalogo de tipos de documento es autenticado; el formulario publico lo necesita para el alta.
                tiposIdentificacion.execute())));
    }

    @PostMapping("/registro")
    public ResponseEntity<ApiDataResponse<MatriculaQrResultadoResponse>> registrar(
            @RequestBody final MatriculaQrRequests.Registro request,
            final HttpServletRequest http
    ) {
        RequestValidationGuard.validate(VALIDATOR.validarRegistro(request));
        final var resultado = registrarPorToken.execute(new RegistrarEstudiantePorTokenInputPort.Comando(
                request.token(), request.pin(), ipResolver.resolve(http),
                request.idTipoIdentificacion(), request.numeroIdentificacion().intValue(),
                request.primerNombre(), request.segundoNombre(), request.primerApellido(), request.segundoApellido(),
                request.correo(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(
                true, new MatriculaQrResultadoResponse(resultado.idGrupo(), resultado.usuario())));
    }
}
