package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.ConsultarFacultadDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.EstudianteFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.FacultadNodoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.FichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.SesionFacultadDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU104/HU105 (directorio, busqueda y ficha de estudiantes), HU109/HU119 (estructura Facultad -> Programa -> Plan) y
 * HU145 (cronograma de sesiones) de la facultad del decano. Solo lectura; controlador propio (no toca
 * {@link DecanoPortalController}) cubierto por el comodin {@code /api/v1/decano/**} de {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/decano")
public final class DecanoFacultadConsultaController {

    private final ConsultarFacultadDecanoInputPort inputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DecanoFacultadConsultaController(final ConsultarFacultadDecanoInputPort inputPort,
                                            final AuthenticatedUserResolver authenticatedUserResolver) {
        this.inputPort = Objects.requireNonNull(inputPort);
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver);
    }

    @GetMapping("/estudiantes")
    public ResponseEntity<ApiListResponse<EstudianteFacultadDTO>> consultarEstudiantes(
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "idPrograma", required = false) final UUID idPrograma,
            @RequestParam(name = "estaActivo", required = false) final Boolean estaActivo,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        return lista(inputPort.consultarEstudiantes(usuario(), busqueda, idPrograma, estaActivo, pagina, tamano));
    }

    @GetMapping("/estudiantes/{estudianteId}")
    public ResponseEntity<ApiDataResponse<FichaEstudianteDTO>> consultarFichaEstudiante(
            @PathVariable("estudianteId") final UUID estudianteId) {
        return ResponseEntity.ok(new ApiDataResponse<>(true, inputPort.consultarFichaEstudiante(usuario(), estudianteId)));
    }

    @GetMapping("/estructura")
    public ResponseEntity<ApiListResponse<FacultadNodoDTO>> consultarEstructura() {
        final List<FacultadNodoDTO> estructura = inputPort.consultarEstructura(usuario());
        return ResponseEntity.ok(new ApiListResponse<>(true, estructura, estructura.size()));
    }

    @GetMapping("/sesiones")
    public ResponseEntity<ApiListResponse<SesionFacultadDecanoResponse>> consultarSesiones(
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate hasta,
            @RequestParam(name = "idPrograma", required = false) final UUID idPrograma,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final PaginaDTO<SesionFacultadDTO> sesiones =
                inputPort.consultarSesiones(usuario(), desde, hasta, idPrograma, pagina, tamano);
        return ResponseEntity.ok(new ApiListResponse<>(true,
                sesiones.elementos().stream().map(SesionFacultadDecanoResponse::from).toList(), sesiones.total()));
    }

    private UUID usuario() {
        return authenticatedUserResolver.requireAuthenticatedUserId();
    }

    private static <T> ResponseEntity<ApiListResponse<T>> lista(final PaginaDTO<T> pagina) {
        return ResponseEntity.ok(new ApiListResponse<>(true, pagina.elementos(), pagina.total()));
    }
}
