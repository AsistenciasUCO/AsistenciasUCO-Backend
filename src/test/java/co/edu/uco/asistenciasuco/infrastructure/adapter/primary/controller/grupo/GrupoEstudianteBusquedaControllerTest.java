package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.BuscarEstudiantePorDocumentoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.dto.BuscarEstudiantePorDocumentoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.dto.EstudianteBusquedaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CULM-001 A.2-15 (A2-B-17, DEC-027): GET /api/v1/grupos/{grupoId}/estudiantes/busqueda. 200 con los datos minimos (sin
 * correo real) o datos null; 403 SEC_002 del SP; 400 FIELD_* en el request si falta o es invalido el documento.
 */
class GrupoEstudianteBusquedaControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID TIPO = UUID.randomUUID();
    private static final String RUTA = "/api/v1/grupos/{grupoId}/estudiantes/busqueda";

    private final BuscarEstudiantePorDocumentoInputPort buscar = mock(BuscarEstudiantePorDocumentoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new GrupoEstudianteBusquedaController(buscar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void encontrado_devuelve_los_campos_minimos_sin_correo_real() throws Exception {
        final UUID estudiante = UUID.randomUUID();
        when(buscar.execute(new BuscarEstudiantePorDocumentoDTO(GRUPO, TIPO, 1017123456, ACTOR))).thenReturn(Optional.of(
                new EstudianteBusquedaDTO(estudiante, "Ana", "Maria", "Perez", "Gomez", "ANA MARIA PEREZ GOMEZ", 1017123456,
                        "a***@uco.edu.co", null)));

        mvc.perform(get(RUTA, GRUPO).param("tipoIdentificacionId", TIPO.toString()).param("documento", "1017123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idEstudiante").value(estudiante.toString()))
                .andExpect(jsonPath("$.datos.primerNombre").value("Ana"))
                .andExpect(jsonPath("$.datos.correoEnmascarado").value("a***@uco.edu.co"))
                .andExpect(jsonPath("$.datos.codigoEstadoMatriculaGrupo").doesNotExist())
                .andExpect(jsonPath("$.datos.correo").doesNotExist());
    }

    @Test
    void sin_resultado_responde_200_con_datos_null() throws Exception {
        when(buscar.execute(any())).thenReturn(Optional.empty());

        mvc.perform(get(RUTA, GRUPO).param("tipoIdentificacionId", TIPO.toString()).param("documento", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos").doesNotExist());
        verify(buscar).execute(new BuscarEstudiantePorDocumentoDTO(GRUPO, TIPO, 123456, ACTOR));
    }

    @Test
    void docente_no_titular_recibe_403() throws Exception {
        when(buscar.execute(any())).thenThrow(new ForbiddenException("SEC_002", "No tiene titularidad sobre el grupo."));

        mvc.perform(get(RUTA, GRUPO).param("tipoIdentificacionId", TIPO.toString()).param("documento", "123456"))
                .andExpect(status().isForbidden());
    }

    @Test
    void documento_vacio_no_numerico_o_no_positivo_y_tipo_ausente_responden_400_sin_consultar() throws Exception {
        mvc.perform(get(RUTA, GRUPO).param("tipoIdentificacionId", TIPO.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("documento"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_REQUIRED"));
        mvc.perform(get(RUTA, GRUPO).param("tipoIdentificacionId", TIPO.toString()).param("documento", "abc"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(RUTA, GRUPO).param("tipoIdentificacionId", TIPO.toString()).param("documento", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("documento"));
        mvc.perform(get(RUTA, GRUPO).param("documento", "123456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("tipoIdentificacionId"));
        verifyNoInteractions(buscar);
    }
}
