package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.ConsultarTiposProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.dto.TipoProgramaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * HU154: consulta del catalogo de tipos de programa (rol ADMINISTRADOR por SecurityConfig). Regla de John
 * (2026-11-04): los catalogos base son de solo consulta; los tipos de programa se cargan por seed.
 */
@RestController
@RequestMapping("/api/v1/admin/tipos-programa")
public final class AdminTipoProgramaController {

    private final ConsultarTiposProgramaInputPort consultarInputPort;

    public AdminTipoProgramaController(final ConsultarTiposProgramaInputPort consultarInputPort) {
        this.consultarInputPort = Objects.requireNonNull(consultarInputPort);
    }

    @GetMapping
    public ResponseEntity<ApiListResponse<TipoProgramaDTO>> listar() {
        final var data = consultarInputPort.consultarTodos();
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }
}
