package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.categoriajustificacion;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.ConsultasTransversalesInputPort;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.CategoriaJustificacionDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * HU005/HU016: motivos posibles para justificar una inasistencia o llegada tarde. Catalogo base de solo consulta
 * (regla de John, 2026-11-04), sin datos personales; cubierto por {@code /api/v1/**} (autenticado) de
 * {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1")
public final class CategoriaJustificacionController {

    private final ConsultasTransversalesInputPort inputPort;

    public CategoriaJustificacionController(final ConsultasTransversalesInputPort inputPort) {
        this.inputPort = Objects.requireNonNull(inputPort, "ConsultasTransversalesInputPort es obligatorio.");
    }

    @GetMapping("/categorias-justificacion")
    public ResponseEntity<ApiListResponse<CategoriaJustificacionDTO>> consultarCategoriasJustificacion() {
        final List<CategoriaJustificacionDTO> categorias = inputPort.consultarCategoriasJustificacion();
        return ResponseEntity.ok(new ApiListResponse<>(true, categorias, categorias.size()));
    }
}
