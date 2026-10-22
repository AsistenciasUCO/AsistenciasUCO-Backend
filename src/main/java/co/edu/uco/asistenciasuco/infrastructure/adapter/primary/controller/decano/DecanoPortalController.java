package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.ConsultarCoordinadoresInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto.CoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.ConsultarProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports.ConsultarTiposProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports.dto.TipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.CrearCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.CrearProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.dto.CrearProgramaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.CrearCoordinadorRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request.CrearProgramaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/decano")
public final class DecanoPortalController {

    private final ConsultarCoordinadoresInputPort consultarCoordinadoresInputPort;
    private final CrearCoordinadorInputPort crearCoordinadorInputPort;
    private final ConsultarProgramasInputPort consultarProgramasInputPort;
    private final ConsultarTiposProgramaInputPort consultarTiposProgramaInputPort;
    private final CrearProgramaInputPort crearProgramaInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DecanoPortalController(
            final ConsultarCoordinadoresInputPort consultarCoordinadoresInputPort,
            final CrearCoordinadorInputPort crearCoordinadorInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this(consultarCoordinadoresInputPort, crearCoordinadorInputPort, null, null, null, authenticatedUserResolver);
    }

    @Autowired
    public DecanoPortalController(
            final ConsultarCoordinadoresInputPort consultarCoordinadoresInputPort,
            final CrearCoordinadorInputPort crearCoordinadorInputPort,
            final ConsultarProgramasInputPort consultarProgramasInputPort,
            final ConsultarTiposProgramaInputPort consultarTiposProgramaInputPort,
            final CrearProgramaInputPort crearProgramaInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarCoordinadoresInputPort = consultarCoordinadoresInputPort;
        this.crearCoordinadorInputPort = crearCoordinadorInputPort;
        this.consultarProgramasInputPort = consultarProgramasInputPort;
        this.consultarTiposProgramaInputPort = consultarTiposProgramaInputPort;
        this.crearProgramaInputPort = crearProgramaInputPort;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/coordinadores")
    public ResponseEntity<ApiListResponse<CoordinadorDTO>> consultarCoordinadores() {
        final var data = consultarCoordinadoresInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @PatchMapping("/coordinadores/{id}/estado")
    public ResponseEntity<ApiDataResponse<Void>> toggleCoordinador(@PathVariable final UUID id) {
        throw unavailable("cambiar estado de coordinador");
    }

    @PostMapping("/coordinadores")
    public ResponseEntity<ApiDataResponse<Void>> crearCoordinador(@RequestBody final CrearCoordinadorRequest request) {
        crearCoordinadorInputPort.execute(new CrearCoordinadorDTO(
                request == null ? null : request.getNumeroIdentificacion(),
                request == null ? null : request.getPrimerNombre(),
                request == null ? null : request.getSegundoNombre(),
                request == null ? null : request.getPrimerApellido(),
                request == null ? null : request.getSegundoApellido(),
                request == null ? null : request.getCorreo(),
                request == null ? null : request.getIdPrograma(),
                request == null ? null : request.getPassword(),
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }

    @GetMapping("/programas")
    public ResponseEntity<ApiListResponse<ProgramaDTO>> consultarProgramas() {
        final var data = consultarProgramasInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @GetMapping("/tipos-programa")
    public ResponseEntity<ApiListResponse<TipoProgramaDTO>> consultarTiposPrograma() {
        final var data = consultarTiposProgramaInputPort.execute();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @PostMapping("/programas")
    public ResponseEntity<ApiDataResponse<ProgramaDTO>> crearPrograma(@RequestBody final CrearProgramaRequest request) {
        final UUID actorId = authenticatedUserResolver.requireAuthenticatedUserId();
        final UUID programaId = UUID.randomUUID();
        final String nombre = request == null ? null : request.getNombre();
        final Integer codigo = request == null ? null : request.getCodigo();
        final UUID idCoordinador = request == null ? null : request.getIdCoordinador();
        final UUID idTipoPrograma = request == null ? null : request.getIdTipoPrograma();

        crearProgramaInputPort.execute(new CrearProgramaDTO(
                programaId,
                codigo,
                nombre,
                idCoordinador,
                idTipoPrograma,
                actorId
        ));

        final ProgramaDTO dto = consultarProgramasInputPort != null
                ? consultarProgramasInputPort.execute(actorId).stream()
                        .filter(p -> p.id().equals(programaId))
                        .findFirst()
                        .orElseGet(() -> new ProgramaDTO(
                                programaId,
                                nombre,
                                null,
                                null,
                                null,
                                null,
                                idCoordinador,
                                null,
                                true,
                                "SI",
                                idTipoPrograma,
                                null
                        ))
                : new ProgramaDTO(
                        programaId,
                        nombre,
                        null,
                        null,
                        null,
                        null,
                        idCoordinador,
                        null,
                        true,
                        "SI",
                        idTipoPrograma,
                        null
                );

        return ResponseEntity.ok(new ApiDataResponse<>(true, dto));
    }

    private static FeatureUnavailableException unavailable(final String operation) {
        return new FeatureUnavailableException("La funcionalidad " + operation + " no esta disponible en esta version.");
    }
}
