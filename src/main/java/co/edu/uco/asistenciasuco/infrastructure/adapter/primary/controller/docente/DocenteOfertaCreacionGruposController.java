package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.ConsultarOfertaCreacionGruposDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.dto.OfertaCreacionGruposDocenteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * F3: datos del formulario "Nuevo grupo" del docente (su idDocente y las asignaturas de los programas con los que tiene
 * vinculo activo). El alcance sale del token (Usuario -> Docente) y la DB lo vuelve a validar.
 */
@RestController
@RequestMapping("/api/v1/docente")
public final class DocenteOfertaCreacionGruposController {

    private final ConsultarOfertaCreacionGruposDocenteInputPort inputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DocenteOfertaCreacionGruposController(
            final ConsultarOfertaCreacionGruposDocenteInputPort inputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.inputPort = inputPort;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/oferta-creacion-grupos")
    public ResponseEntity<ApiDataResponse<OfertaCreacionGruposDocenteDTO>> consultarOferta() {
        return ResponseEntity.ok(new ApiDataResponse<>(
                true, inputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId())));
    }
}
