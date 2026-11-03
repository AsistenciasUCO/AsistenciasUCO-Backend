package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.ConsultarAsistenciaProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.AsistenciaSesionDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.GrupoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.SesionGrupoDTO;
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
import java.util.Objects;
import java.util.UUID;

/**
 * HU064 (grupos del programa), HU060 (sesiones con registro de asistencia) y HU061 (detalle de asistencia por
 * estudiante). Solo lectura; el caso de uso valida paginacion y el provider DB aisla por programa.
 */
@RestController
@RequestMapping("/api/v1/coordinador")
public final class CoordinadorAsistenciaProgramaController {

    private final ConsultarAsistenciaProgramaInputPort inputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CoordinadorAsistenciaProgramaController(final ConsultarAsistenciaProgramaInputPort inputPort,
                                                   final AuthenticatedUserResolver authenticatedUserResolver) {
        this.inputPort = Objects.requireNonNull(inputPort);
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver);
    }

    @GetMapping("/grupos")
    public ResponseEntity<ApiListResponse<GrupoProgramaDTO>> consultarGrupos(
            @RequestParam(name = "idPeriodo", required = false) final UUID idPeriodo,
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        return respuesta(inputPort.consultarGrupos(usuario(), idPeriodo, busqueda, pagina, tamano));
    }

    @GetMapping("/grupos/{grupoId}/sesiones")
    public ResponseEntity<ApiListResponse<SesionGrupoCoordinadorResponse>> consultarSesiones(
            @PathVariable("grupoId") final UUID grupoId,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate hasta,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final PaginaDTO<SesionGrupoDTO> sesiones =
                inputPort.consultarSesionesGrupo(usuario(), grupoId, desde, hasta, pagina, tamano);
        return ResponseEntity.ok(new ApiListResponse<>(true,
                sesiones.elementos().stream().map(SesionGrupoCoordinadorResponse::from).toList(), sesiones.total()));
    }

    @GetMapping("/sesiones/{sesionId}/asistencias")
    public ResponseEntity<ApiListResponse<AsistenciaSesionDTO>> consultarAsistencias(
            @PathVariable("sesionId") final UUID sesionId,
            @RequestParam(name = "asistio", required = false) final Boolean asistio,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        return respuesta(inputPort.consultarAsistenciasSesion(usuario(), sesionId, asistio, pagina, tamano));
    }

    private UUID usuario() {
        return authenticatedUserResolver.requireAuthenticatedUserId();
    }

    private static <T> ResponseEntity<ApiListResponse<T>> respuesta(final PaginaDTO<T> pagina) {
        return ResponseEntity.ok(new ApiListResponse<>(true, pagina.elementos(), pagina.total()));
    }
}
