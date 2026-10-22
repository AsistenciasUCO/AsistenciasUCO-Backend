package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.ActualizarFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.dto.ActualizarFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.CambiarEstadoFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.dto.CambiarEstadoFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.ConsultarAreasInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.dto.AreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.ConsultarDecanosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.dto.DecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.ConsultarFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.dto.FacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.ConsultarInstitucionesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.dto.InstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.ConsultarParametrosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.dto.ParametroDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.CrearDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.CrearDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.CrearFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.dto.CrearFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.EjecutarCierreMasivoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.dto.EjecutarCierreMasivoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/admin")
public final class AdminPortalController {

    private final ConsultarDecanosInputPort consultarDecanosInputPort;
    private final CrearDecanoInputPort crearDecanoInputPort;
    private final ConsultarParametrosInputPort consultarParametrosInputPort;
    private final EjecutarCierreMasivoInputPort ejecutarCierreMasivoInputPort;
    private final ConsultarInstitucionesInputPort consultarInstitucionesInputPort;
    private final ConsultarFacultadesInputPort consultarFacultadesInputPort;
    private final CrearFacultadInputPort crearFacultadInputPort;
    private final ActualizarFacultadInputPort actualizarFacultadInputPort;
    private final CambiarEstadoFacultadInputPort cambiarEstadoFacultadInputPort;
    private final ConsultarAreasInputPort consultarAreasInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminPortalController(
            final ConsultarDecanosInputPort consultarDecanosInputPort,
            final CrearDecanoInputPort crearDecanoInputPort,
            final ConsultarParametrosInputPort consultarParametrosInputPort,
            final EjecutarCierreMasivoInputPort ejecutarCierreMasivoInputPort,
            final ConsultarInstitucionesInputPort consultarInstitucionesInputPort,
            final ConsultarFacultadesInputPort consultarFacultadesInputPort,
            final CrearFacultadInputPort crearFacultadInputPort,
            final ActualizarFacultadInputPort actualizarFacultadInputPort,
            final CambiarEstadoFacultadInputPort cambiarEstadoFacultadInputPort,
            final ConsultarAreasInputPort consultarAreasInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarDecanosInputPort = consultarDecanosInputPort;
        this.crearDecanoInputPort = crearDecanoInputPort;
        this.consultarParametrosInputPort = consultarParametrosInputPort;
        this.ejecutarCierreMasivoInputPort = ejecutarCierreMasivoInputPort;
        this.consultarInstitucionesInputPort = consultarInstitucionesInputPort;
        this.consultarFacultadesInputPort = consultarFacultadesInputPort;
        this.crearFacultadInputPort = crearFacultadInputPort;
        this.actualizarFacultadInputPort = actualizarFacultadInputPort;
        this.cambiarEstadoFacultadInputPort = cambiarEstadoFacultadInputPort;
        this.consultarAreasInputPort = consultarAreasInputPort;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/decanos")
    public ResponseEntity<ApiListResponse<DecanoDTO>> consultarDecanos() {
        final var data = consultarDecanosInputPort.execute();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @PostMapping("/decanos")
    public ResponseEntity<ApiMessageResponse> crearDecano(@RequestBody final CrearDecanoRequest request) {
        crearDecanoInputPort.execute(new CrearDecanoDTO(
                request.tipoIdentificacionId(), request.numeroIdentificacion(), request.primerNombre(),
                request.segundoNombre(), request.primerApellido(), request.segundoApellido(), request.correo(),
                request.password(), request.idFacultad(), authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiMessageResponse(true, "Decano creado correctamente."));
    }

    @PatchMapping("/decanos/{id}/toggle")
    public ResponseEntity<ApiDataResponse<Void>> toggleDecano(@PathVariable final UUID id) {
        throw unavailable("cambiar estado de decano");
    }

    @GetMapping("/sedes")
    public ResponseEntity<ApiListResponse<Void>> consultarSedes() { throw unavailable("consultar sedes"); }
    @PostMapping("/sedes")
    public ResponseEntity<ApiDataResponse<Void>> crearSede() { throw unavailable("crear sede"); }
    @PutMapping("/sedes/{id}")
    public ResponseEntity<ApiDataResponse<Void>> actualizarSede(@PathVariable final UUID id) { throw unavailable("actualizar sede"); }
    @PatchMapping("/sedes/{id}/estado")
    public ResponseEntity<ApiDataResponse<Void>> cambiarEstadoSede(@PathVariable final UUID id) { throw unavailable("cambiar estado de sede"); }

    @GetMapping("/espacios-fisicos")
    public ResponseEntity<ApiListResponse<Void>> consultarEspaciosFisicos() { throw unavailable("consultar espacios fisicos"); }
    @PostMapping("/espacios-fisicos")
    public ResponseEntity<ApiDataResponse<Void>> crearEspacioFisico() { throw unavailable("crear espacio fisico"); }
    @PutMapping("/espacios-fisicos/{id}")
    public ResponseEntity<ApiDataResponse<Void>> actualizarEspacioFisico(@PathVariable final UUID id) { throw unavailable("actualizar espacio fisico"); }
    @PatchMapping("/espacios-fisicos/{id}/estado")
    public ResponseEntity<ApiDataResponse<Void>> cambiarEstadoEspacioFisico(@PathVariable final UUID id) { throw unavailable("cambiar estado de espacio fisico"); }

    @GetMapping("/parametros")
    public ResponseEntity<ApiListResponse<ParametroDTO>> consultarParametros() {
        final var data = consultarParametrosInputPort.execute();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
    @PutMapping("/parametros/{id}")
    public ResponseEntity<ApiDataResponse<Void>> actualizarParametro(@PathVariable final UUID id) { throw unavailable("actualizar parametro"); }
    @GetMapping("/auditoria")
    public ResponseEntity<ApiListResponse<Void>> consultarAuditoria() { throw unavailable("consultar auditoria"); }

    @PostMapping("/cierre-masivo")
    public ResponseEntity<ApiMessageResponse> ejecutarCierreMasivo(@RequestBody final CierreMasivoRequest request) {
        ejecutarCierreMasivoInputPort.execute(new EjecutarCierreMasivoDTO(
                request.idPeriodoAcademico(),
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiMessageResponse(true, "Cierre masivo solicitado correctamente."));
    }

    @GetMapping("/instituciones")
    public ResponseEntity<ApiListResponse<InstitucionDTO>> consultarInstituciones() {
        final var data = consultarInstitucionesInputPort.execute();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
    @PostMapping("/instituciones")
    public ResponseEntity<ApiDataResponse<Void>> crearInstitucion() { throw unavailable("crear institucion"); }
    @PutMapping("/instituciones/{id}")
    public ResponseEntity<ApiDataResponse<Void>> actualizarInstitucion(@PathVariable final UUID id) { throw unavailable("actualizar institucion"); }
    @PatchMapping("/instituciones/{id}/toggle-estado")
    public ResponseEntity<ApiDataResponse<Void>> cambiarEstadoInstitucion(@PathVariable final UUID id) { throw unavailable("cambiar estado de institucion"); }

    @GetMapping("/facultades")
    public ResponseEntity<ApiListResponse<FacultadDTO>> consultarFacultades() {
        final var data = consultarFacultadesInputPort.execute();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
    @PostMapping("/facultades")
    public ResponseEntity<ApiDataResponse<FacultadDTO>> crearFacultad(@RequestBody final CrearFacultadRequest request) {
        final UUID actorId = authenticatedUserResolver.requireAuthenticatedUserId();
        final UUID facultadId = UUID.randomUUID();
        final String nombre = request != null ? request.nombre() : null;
        final UUID idInstitucion = request != null ? request.idInstitucion() : null;
        final UUID idDecano = request != null ? request.idDecano() : null;

        crearFacultadInputPort.execute(new CrearFacultadDTO(
                facultadId,
                nombre,
                idInstitucion,
                idDecano,
                actorId
        ));

        final FacultadDTO dto = consultarFacultadesInputPort.execute().stream()
                .filter(f -> f.id().equals(facultadId))
                .findFirst()
                .orElseGet(() -> new FacultadDTO(
                        facultadId,
                        nombre,
                        idInstitucion,
                        null,
                        idDecano,
                        null,
                        true,
                        "SI"
                ));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, dto));
    }
    @PutMapping("/facultades/{id}")
    public ResponseEntity<ApiDataResponse<FacultadDTO>> actualizarFacultad(
            @PathVariable final UUID id,
            @RequestBody final ActualizarFacultadRequest request
    ) {
        actualizarFacultadInputPort.execute(new ActualizarFacultadDTO(
                id,
                request != null ? request.nombre() : null,
                request != null ? request.idDecano() : null,
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, releerFacultad(id)));
    }
    @PatchMapping("/facultades/{id}/estado")
    public ResponseEntity<ApiDataResponse<FacultadDTO>> cambiarEstadoFacultad(
            @PathVariable final UUID id,
            @RequestBody final CambiarEstadoFacultadRequest request
    ) {
        cambiarEstadoFacultadInputPort.execute(new CambiarEstadoFacultadDTO(
                id,
                request != null ? request.activa() : null,
                authenticatedUserResolver.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, releerFacultad(id)));
    }

    @GetMapping("/areas")
    public ResponseEntity<ApiListResponse<AreaDTO>> consultarAreas() {
        final var data = consultarAreasInputPort.execute();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
    @PostMapping("/areas")
    public ResponseEntity<ApiDataResponse<Void>> crearArea() { throw unavailable("crear area"); }
    @PutMapping("/areas/{id}")
    public ResponseEntity<ApiDataResponse<Void>> actualizarArea(@PathVariable final UUID id) { throw unavailable("actualizar area"); }
    @PatchMapping("/areas/{id}/estado")
    public ResponseEntity<ApiDataResponse<Void>> cambiarEstadoArea(@PathVariable final UUID id) { throw unavailable("cambiar estado de area"); }

    private FacultadDTO releerFacultad(final UUID id) {
        return consultarFacultadesInputPort.execute().stream()
                .filter(f -> id.equals(f.id()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("La facultad especificada no existe en el sistema."));
    }

    private FeatureUnavailableException unavailable(final String operation) {
        return new FeatureUnavailableException("La operacion de administrador no esta disponible: " + operation + ".");
    }

    public record CrearDecanoRequest(UUID tipoIdentificacionId, Integer numeroIdentificacion, String primerNombre,
                                     String segundoNombre, String primerApellido, String segundoApellido, String correo,
                                     String password, UUID idFacultad) {
    }

    public record CrearFacultadRequest(String nombre, String codigo, UUID idInstitucion, UUID idDecano) {
    }

    public record ActualizarFacultadRequest(String nombre, UUID idDecano) {
    }

    public record CambiarEstadoFacultadRequest(Boolean activa) {
    }

    public record CierreMasivoRequest(UUID idPeriodoAcademico) {
    }
}
