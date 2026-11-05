package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.ActualizarDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.dto.ActualizarDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports.CambiarEstadoDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports.dto.CambiarEstadoDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports.dto.DocenteCandidatoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.dto.DocentesProgramaPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports.ConsultarCandidatosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.ConsultarDocentesProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.dto.ConsultarDocentesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.VincularDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.dto.VincularDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.ActualizarDocenteProgramaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.CambiarEstadoDocenteProgramaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.VincularDocenteProgramaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation.CambiarEstadoDocenteProgramaRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation.ActualizarDocenteProgramaRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation.VincularDocenteProgramaRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** HU062/HU063/HU089/HU091: directorio, candidatos y vinculacion de docentes del programa del coordinador. */
@RestController
@RequestMapping("/api/v1/coordinador/docentes")
public final class CoordinadorDocentesController {

    private static final int TAMANO_MAXIMO = 100;
    private static final int BUSQUEDA_MAXIMA = 50;
    private static final int BUSQUEDA_CANDIDATOS_MINIMA = 3;
    private static final VincularDocenteProgramaRequestValidator VINCULAR_VALIDATOR = new VincularDocenteProgramaRequestValidator();
    private static final ActualizarDocenteProgramaRequestValidator ACTUALIZAR_VALIDATOR = new ActualizarDocenteProgramaRequestValidator();
    private static final CambiarEstadoDocenteProgramaRequestValidator CAMBIAR_ESTADO_VALIDATOR = new CambiarEstadoDocenteProgramaRequestValidator();

    private final ConsultarDocentesProgramaInputPort consultarDocentesProgramaInputPort;
    private final ConsultarCandidatosDocenteInputPort consultarCandidatosDocenteInputPort;
    private final VincularDocenteProgramaInputPort vincularDocenteProgramaInputPort;
    private final ActualizarDocenteProgramaInputPort actualizarDocenteProgramaInputPort;
    private final CambiarEstadoDocenteProgramaInputPort cambiarEstadoDocenteProgramaInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CoordinadorDocentesController(
            final ConsultarDocentesProgramaInputPort consultarDocentesProgramaInputPort,
            final ConsultarCandidatosDocenteInputPort consultarCandidatosDocenteInputPort,
            final VincularDocenteProgramaInputPort vincularDocenteProgramaInputPort,
            final ActualizarDocenteProgramaInputPort actualizarDocenteProgramaInputPort,
            final CambiarEstadoDocenteProgramaInputPort cambiarEstadoDocenteProgramaInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarDocentesProgramaInputPort = Objects.requireNonNull(consultarDocentesProgramaInputPort);
        this.consultarCandidatosDocenteInputPort = Objects.requireNonNull(consultarCandidatosDocenteInputPort);
        this.vincularDocenteProgramaInputPort = Objects.requireNonNull(vincularDocenteProgramaInputPort);
        this.actualizarDocenteProgramaInputPort = Objects.requireNonNull(actualizarDocenteProgramaInputPort);
        this.cambiarEstadoDocenteProgramaInputPort = Objects.requireNonNull(cambiarEstadoDocenteProgramaInputPort);
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver);
    }

    @GetMapping
    public ResponseEntity<ApiListResponse<DocenteProgramaDTO>> consultar(
            @RequestParam(name = "pagina", defaultValue = "1") final int pagina,
            @RequestParam(name = "tamano", defaultValue = "10") final int tamano,
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "estaActivo", required = false) final Boolean estaActivo,
            @RequestParam(name = "idTipoVinculacion", required = false) final UUID idTipoVinculacion
    ) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (pagina < 1) {
            builder.add("pagina", ValidationErrorType.OUT_OF_RANGE, "La pagina debe ser mayor o igual a 1.");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            builder.add("tamano", ValidationErrorType.OUT_OF_RANGE, "El tamano de pagina debe estar entre 1 y " + TAMANO_MAXIMO + ".");
        }
        if (busqueda != null && busqueda.length() > BUSQUEDA_MAXIMA) {
            builder.add("busqueda", ValidationErrorType.INVALID_LENGTH, "La busqueda excede la longitud permitida.");
        }
        RequestValidationGuard.validate(builder.build());

        final DocentesProgramaPaginaDTO resultado = consultarDocentesProgramaInputPort.execute(
                authenticatedUserResolver.requireAuthenticatedUserId(),
                new ConsultarDocentesProgramaDTO(pagina, tamano, busqueda, estaActivo, idTipoVinculacion));
        return ResponseEntity.ok(new ApiListResponse<>(true, resultado.elementos(), (int) resultado.total()));
    }

    @GetMapping("/candidatos")
    public ResponseEntity<ApiListResponse<DocenteCandidatoDTO>> consultarCandidatos(
            @RequestParam(name = "busqueda", required = false) final String busqueda
    ) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        final String texto = busqueda == null ? "" : busqueda.trim();
        if (texto.length() < BUSQUEDA_CANDIDATOS_MINIMA) {
            builder.add("busqueda", ValidationErrorType.INVALID_LENGTH, "La busqueda de candidatos requiere al menos 3 caracteres.");
        } else if (texto.length() > BUSQUEDA_MAXIMA) {
            builder.add("busqueda", ValidationErrorType.INVALID_LENGTH, "La busqueda excede la longitud permitida.");
        }
        RequestValidationGuard.validate(builder.build());

        final List<DocenteCandidatoDTO> lista = consultarCandidatosDocenteInputPort.execute(
                authenticatedUserResolver.requireAuthenticatedUserId(), texto);
        return ResponseEntity.ok(new ApiListResponse<>(true, lista, lista.size()));
    }

    @PostMapping
    @AuditableOperation(action = "VINCULAR_DOCENTE_PROGRAMA", resourceType = "DOCENTE")
    public ResponseEntity<ApiDataResponse<DocenteProgramaDTO>> vincular(@RequestBody final VincularDocenteProgramaRequest request) {
        RequestValidationGuard.validate(VINCULAR_VALIDATOR.validate(request));
        final DocenteProgramaDTO resultado = vincularDocenteProgramaInputPort.execute(new VincularDocenteProgramaDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(),
                request.getIdDocente(),
                request.getIdUsuario(),
                request.getIdTipoIdentificacion(),
                request.getNumeroIdentificacion() == null ? null : Integer.valueOf(request.getNumeroIdentificacion().trim()),
                trim(request.getPrimerNombre()),
                trim(request.getSegundoNombre()),
                trim(request.getPrimerApellido()),
                trim(request.getSegundoApellido()),
                trim(request.getCorreo()),
                request.getPassword(),
                request.getIdTipoVinculacion()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, resultado));
    }

    @PutMapping("/{idDocente}")
    @AuditableOperation(action = "ACTUALIZAR_DOCENTE_PROGRAMA", resourceType = "DOCENTE")
    public ResponseEntity<ApiDataResponse<DocenteProgramaDTO>> actualizar(
            @PathVariable("idDocente") final UUID idDocente,
            @RequestBody final ActualizarDocenteProgramaRequest request
    ) {
        RequestValidationGuard.validate(ACTUALIZAR_VALIDATOR.validate(request));
        final DocenteProgramaDTO resultado = actualizarDocenteProgramaInputPort.execute(new ActualizarDocenteProgramaDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(),
                idDocente,
                request.getIdTipoVinculacion(),
                trim(request.getPrimerNombre()),
                trim(request.getSegundoNombre()),
                trim(request.getPrimerApellido()),
                trim(request.getSegundoApellido()),
                request.getIdTipoIdentificacion(),
                request.getNumeroIdentificacion(),
                trim(request.getMotivo())));
        return ResponseEntity.ok(new ApiDataResponse<>(true, resultado));
    }

    @PatchMapping("/{idDocente}/estado")
    @AuditableOperation(action = "CAMBIAR_ESTADO_DOCENTE_PROGRAMA", resourceType = "DOCENTE")
    public ResponseEntity<ApiDataResponse<DocenteProgramaDTO>> cambiarEstado(
            @PathVariable("idDocente") final UUID idDocente,
            @RequestBody final CambiarEstadoDocenteProgramaRequest request
    ) {
        Objects.requireNonNull(request, "request");
        RequestValidationGuard.validate(CAMBIAR_ESTADO_VALIDATOR.validate(request));
        final DocenteProgramaDTO resultado = cambiarEstadoDocenteProgramaInputPort.execute(new CambiarEstadoDocenteProgramaDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), idDocente, Boolean.TRUE.equals(request.getEstaActivo())));
        return ResponseEntity.ok(new ApiDataResponse<>(true, resultado));
    }

    private static String trim(final String valor) {
        return valor == null ? null : valor.trim();
    }
}
