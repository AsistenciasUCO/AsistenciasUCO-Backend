package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario;


import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.ProvisionarUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.dto.ProvisionarUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditRequestAttributes;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.mapper.CrearUsuarioHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.request.CrearUsuarioRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.response.CrearUsuarioResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.usuario.validation.CrearUsuarioRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Adaptador primario REST para operaciones relacionadas con usuarios. La consulta del perfil propio (HU006) vive en
 * {@code UsuarioPerfilController}.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
public final class UsuarioController {

    private static final CrearUsuarioRequestValidator CREATE_USER_VALIDATOR = new CrearUsuarioRequestValidator();

    private final ProvisionarUsuarioInputPort provisionarUsuarioInputPort;

    public UsuarioController(final ProvisionarUsuarioInputPort provisionarUsuarioInputPort) {
        this.provisionarUsuarioInputPort = Objects.requireNonNull(
                provisionarUsuarioInputPort,
                "ProvisionarUsuarioInputPort es obligatorio."
        );
    }

    // CULM-001 A.2-14: el stub 501 PUT /perfil se retira; el perfil propio es de solo consulta (HU006, GET).

    @PostMapping
    @AuditableOperation(
            action = "CREAR_USUARIO",
            resourceType = "USUARIO"
    )
    public ResponseEntity<CrearUsuarioResponse> crearUsuario(@RequestBody final CrearUsuarioRequest request) {
        RequestValidationGuard.validate(CREATE_USER_VALIDATOR.validate(request));
        final ProvisionarUsuarioDTO dto = CrearUsuarioHttpMapper.toApplicationDTO(request);
        final ProvisionarUsuarioResultadoDTO resultado = provisionarUsuarioInputPort.execute(dto);
        if (!ObjectHelper.isNull(resultado.usuarioId())) {
            AuditRequestAttributes.storeResourceId(resultado.usuarioId().toString());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(CrearUsuarioResponse.from(resultado));
    }
}
