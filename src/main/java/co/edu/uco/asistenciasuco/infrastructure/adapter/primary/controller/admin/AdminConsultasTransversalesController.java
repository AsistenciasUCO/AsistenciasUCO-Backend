package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.ConsultasTransversalesInputPort;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.AsignaturaInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.DocenteFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EstadoSistemaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EstudianteInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EventoAuditoriaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FichaUnificadaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FiltroAuditoriaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.EventoAuditoriaResponse;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU143 (busqueda de asignaturas en toda la institucion), HU157 (buscador de estudiantes y ficha unificada con
 * historial) y HU172 (rastro de auditoria) del administrador. Solo lectura; cubierto por {@code /api/v1/admin/**} (ADMINISTRADOR) de
 * {@code SecurityConfig}. El SP exige el perfil y limita las filas a la institucion que administra.
 */
@RestController
@RequestMapping("/api/v1/admin")
public final class AdminConsultasTransversalesController {

    private final ConsultasTransversalesInputPort inputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminConsultasTransversalesController(final ConsultasTransversalesInputPort inputPort,
                                                 final AuthenticatedUserResolver authenticatedUserResolver) {
        this.inputPort = Objects.requireNonNull(inputPort);
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver);
    }

    @GetMapping("/asignaturas")
    public ResponseEntity<ApiListResponse<AsignaturaInstitucionDTO>> buscarAsignaturas(
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "idFacultad", required = false) final UUID idFacultad,
            @RequestParam(name = "estaActiva", required = false) final Boolean estaActiva,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        return lista(inputPort.buscarAsignaturas(usuario(), busqueda, idFacultad, estaActiva, pagina, tamano));
    }

    /** HU136: docentes de la institucion del administrador (solo consulta). */
    @GetMapping("/docentes")
    public ResponseEntity<ApiListResponse<DocenteFacultadDTO>> consultarDocentes(
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "idFacultad", required = false) final UUID idFacultad,
            @RequestParam(name = "idPrograma", required = false) final UUID idPrograma,
            @RequestParam(name = "estaActivo", required = false) final Boolean estaActivo,
            @RequestParam(name = "estadoVinculo", required = false) final String estadoVinculo,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        return lista(inputPort.consultarDocentesInstitucion(usuario(), busqueda, idFacultad, idPrograma, estaActivo,
                estadoVinculo, pagina, tamano));
    }

    @GetMapping("/estudiantes")
    public ResponseEntity<ApiListResponse<EstudianteInstitucionDTO>> buscarEstudiantes(
            @RequestParam(name = "busqueda", required = false) final String busqueda,
            @RequestParam(name = "idFacultad", required = false) final UUID idFacultad,
            @RequestParam(name = "estaActivo", required = false) final Boolean estaActivo,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        return lista(inputPort.buscarEstudiantes(usuario(), busqueda, idFacultad, estaActivo, pagina, tamano));
    }

    @GetMapping("/estudiantes/{estudianteId}/historial")
    public ResponseEntity<ApiDataResponse<FichaUnificadaDTO>> consultarHistorial(
            @PathVariable("estudianteId") final UUID estudianteId) {
        return ResponseEntity.ok(new ApiDataResponse<>(true, inputPort.consultarHistorial(usuario(), estudianteId)));
    }

    /** HU155: estados del sistema de solo consulta (estados de solicitudes y del estudiante en el grupo). */
    @GetMapping("/estados")
    public ResponseEntity<ApiListResponse<EstadoSistemaDTO>> consultarEstadosSistema() {
        final List<EstadoSistemaDTO> estados = inputPort.consultarEstadosSistema();
        return ResponseEntity.ok(new ApiListResponse<>(true, estados, estados.size()));
    }

    /** HU172: rastro de auditoria; sin rango se consultan los ultimos 7 dias (maximo 90). */
    @GetMapping("/auditoria")
    public ResponseEntity<ApiListResponse<EventoAuditoriaResponse>> consultarAuditoria(
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime hasta,
            @RequestParam(name = "accion", required = false) final String accion,
            @RequestParam(name = "tipoRecurso", required = false) final String tipoRecurso,
            @RequestParam(name = "idRecurso", required = false) final String idRecurso,
            @RequestParam(name = "idUsuarioActor", required = false) final UUID idUsuarioActor,
            @RequestParam(name = "resultado", required = false) final String resultado,
            @RequestParam(name = "soloAsistencia", required = false) final Boolean soloAsistencia,
            @RequestParam(name = "idCorrelacion", required = false) final UUID idCorrelacion,
            @RequestParam(name = "soloAdministracion", required = false) final Boolean soloAdministracion,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final PaginaDTO<EventoAuditoriaDTO> eventos = inputPort.consultarAuditoria(usuario(), new FiltroAuditoriaDTO(desde, hasta, accion, tipoRecurso,
                idRecurso, idUsuarioActor, resultado, soloAsistencia, idCorrelacion, soloAdministracion), pagina, tamano);
        return lista(new PaginaDTO<>(eventos.elementos().stream().map(EventoAuditoriaResponse::from).toList(), eventos.total()));
    }

    private UUID usuario() {
        return authenticatedUserResolver.requireAuthenticatedUserId();
    }

    private static <T> ResponseEntity<ApiListResponse<T>> lista(final PaginaDTO<T> pagina) {
        return ResponseEntity.ok(new ApiListResponse<>(true, pagina.elementos(), pagina.total()));
    }
}
