package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.AdmitirEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.dto.AdmisionEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.dto.AdmitirEstudianteCommandDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.ConsultarProgramasInstitucionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.dto.ProgramaInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.AdmitirEstudianteRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation.AdmitirEstudianteRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Admision de estudiantes por el administrador, HU156 (CULM-001 D.4, CONTRACT §5.3). {@code POST /admin/estudiantes/admision}
 * crea o reutiliza la persona y la vincula a un programa de su institucion sin matricularla; {@code GET /admin/programas}
 * alimenta el selector. La correccion de identidad del estudiante vive en {@code CorreccionIdentidadController}. El
 * ambito y las reglas los decide la base de datos; la clave de la peticion nunca se registra (OBS-12).
 */
@RestController
public final class AdminAdmisionEstudianteController {

    private static final AdmitirEstudianteRequestValidator VALIDATOR = new AdmitirEstudianteRequestValidator();

    private final AdmitirEstudianteInputPort admitirEstudianteInputPort;
    private final ConsultarProgramasInstitucionInputPort consultarProgramasInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminAdmisionEstudianteController(
            final AdmitirEstudianteInputPort admitirEstudianteInputPort,
            final ConsultarProgramasInstitucionInputPort consultarProgramasInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.admitirEstudianteInputPort = Objects.requireNonNull(admitirEstudianteInputPort, "AdmitirEstudianteInputPort es obligatorio.");
        this.consultarProgramasInputPort = Objects.requireNonNull(consultarProgramasInputPort, "ConsultarProgramasInstitucionInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping("/api/v1/admin/estudiantes/admision")
    public ResponseEntity<ApiDataResponse<AdmisionEstudianteDTO>> admitir(@RequestBody final AdmitirEstudianteRequest request) {
        RequestValidationGuard.validate(VALIDATOR.validate(request));
        final AdmisionEstudianteDTO resultado = admitirEstudianteInputPort.execute(new AdmitirEstudianteCommandDTO(
                request.getTipoIdentificacionId(),
                request.getNumeroIdentificacion(),
                trim(request.getPrimerNombre()),
                trim(request.getSegundoNombre()),
                trim(request.getPrimerApellido()),
                trim(request.getSegundoApellido()),
                request.getCorreo().trim().toLowerCase(Locale.ROOT),
                blankToNull(request.getPassword()),
                request.getIdPrograma(),
                authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, resultado));
    }

    @GetMapping("/api/v1/admin/programas")
    public ResponseEntity<ApiPageResponse<ProgramaInstitucionResponse>> programas(
            @RequestParam(name = "texto", required = false) final String texto,
            @RequestParam(name = "idFacultad", required = false) final UUID idFacultad,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final PaginaResultado<ProgramaInstitucionDTO> resultado = consultarProgramasInputPort.execute(
                authenticatedUserResolver.requireAuthenticatedUserId(), texto, idFacultad, pagina, tamano);
        return ResponseEntity.ok(ApiPageResponse.of(resultado.elementos().stream().map(ProgramaInstitucionResponse::de).toList(), resultado.numeroPagina(), resultado.tamanoPagina(),
                resultado.totalElementos()));
    }

    /** Forma del contrato D §5.3: {@code codigo} es nulo porque el modelo vigente de Programa no tiene codigo. */
    public record ProgramaInstitucionResponse(UUID id, String codigo, String nombre, UUID idFacultad, String nombreFacultad) {

        static ProgramaInstitucionResponse de(final ProgramaInstitucionDTO programa) {
            return new ProgramaInstitucionResponse(programa.id(), null, programa.nombre(), programa.idFacultad(), programa.nombreFacultad());
        }
    }

    private static String trim(final String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String blankToNull(final String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
