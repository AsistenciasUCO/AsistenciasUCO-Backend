package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.ConsultasTransversalesInputPort;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ComponenteFormacionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ConsolidadoGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FiltroConsolidadoDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU061: consolidado de asistencia por grupo (cohorte = periodo academico, materia y docente) y HU147: componentes de
 * formacion para el formulario de asignaturas. Solo lectura. Las rutas quedan bajo {@code /api/v1/coordinador/**}
 * (COORDINADOR, ADMINISTRADOR) y {@code /api/v1/decano/**} (DECANO, ADMINISTRADOR) de {@code SecurityConfig}; el SP
 * exige el perfil y limita los grupos al programa del coordinador, la facultad del decano o la institucion del
 * administrador.
 */
@RestController
public final class ConsolidadoAsistenciaController {

    private final ConsultasTransversalesInputPort inputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ConsolidadoAsistenciaController(final ConsultasTransversalesInputPort inputPort,
                                           final AuthenticatedUserResolver authenticatedUserResolver) {
        this.inputPort = Objects.requireNonNull(inputPort);
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver);
    }

    @GetMapping({"/api/v1/coordinador/consolidado-asistencia", "/api/v1/decano/consolidado-asistencia"})
    public ResponseEntity<ApiListResponse<ConsolidadoGrupoDTO>> consultarConsolidado(
            @RequestParam(name = "idPeriodoAcademico", required = false) final UUID idPeriodoAcademico,
            @RequestParam(name = "idPrograma", required = false) final UUID idPrograma,
            @RequestParam(name = "idAsignatura", required = false) final UUID idAsignatura,
            @RequestParam(name = "idDocente", required = false) final UUID idDocente,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final PaginaDTO<ConsolidadoGrupoDTO> resultado = inputPort.consultarConsolidadoAsistencia(usuario(),
                new FiltroConsolidadoDTO(idPeriodoAcademico, idPrograma, idAsignatura, idDocente), pagina, tamano);
        return ResponseEntity.ok(new ApiListResponse<>(true, resultado.elementos(), resultado.total()));
    }

    @GetMapping("/api/v1/coordinador/componentes-formacion")
    public ResponseEntity<ApiListResponse<ComponenteFormacionDTO>> consultarComponentes() {
        final List<ComponenteFormacionDTO> componentes = inputPort.consultarComponentes(usuario());
        return ResponseEntity.ok(new ApiListResponse<>(true, componentes, componentes.size()));
    }

    private UUID usuario() {
        return authenticatedUserResolver.requireAuthenticatedUserId();
    }
}
