package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.AdmitirEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.dto.AdmisionEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.dto.AdmitirEstudianteCommandDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.ConsultarProgramasInstitucionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.dto.ProgramaInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.exception.AdmisionEstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CULM-001 D.4 (CONTRACT §5.3, HU156): {@code POST /api/v1/admin/estudiantes/admision} (201, 400 con {@code details}, 403, 409)
 * y {@code GET /api/v1/admin/programas}. OBS-12: la clave de la peticion nunca llega a los logs del
 * {@code GlobalExceptionHandler} ni en el 400 ni en el 500.
 */
class AdminAdmisionEstudianteControllerTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID TIPO = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final String CLAVE = "ClaveSecretaUnica123!";
    private static final String CUERPO = "{\"tipoIdentificacionId\":\"" + TIPO + "\",\"numeroIdentificacion\":1017123456,"
            + "\"primerNombre\":\" Ana \",\"segundoNombre\":\"\",\"primerApellido\":\"Perez\",\"correo\":\" Ana.Perez@uco.edu.co \","
            + "\"password\":\"" + CLAVE + "\",\"idPrograma\":\"" + PROGRAMA + "\"}";

    private final AdmitirEstudianteInputPort admitir = mock(AdmitirEstudianteInputPort.class);
    private final ConsultarProgramasInstitucionInputPort programas = mock(ConsultarProgramasInstitucionInputPort.class);
    private final AuthenticatedUserResolver identidad = () -> ADMIN;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminAdmisionEstudianteController(admitir, programas, identidad))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();
    private ListAppender<ILoggingEvent> logs;
    private Logger raiz;

    @BeforeEach
    void capturarLogs() {
        raiz = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logs = new ListAppender<>();
        logs.start();
        raiz.addAppender(logs);
        raiz.setLevel(Level.DEBUG);
    }

    @AfterEach
    void liberarLogs() {
        raiz.detachAppender(logs);
    }

    private static AdmisionEstudianteDTO admision(final String estado, final boolean reparada) {
        return new AdmisionEstudianteDTO(UUID.fromString("00000000-0000-0000-0000-0000000000e1"),
                UUID.fromString("00000000-0000-0000-0000-0000000000a1"), true, estado, reparada);
    }

    @Test
    void admision_responde_201_con_la_forma_del_contrato_y_normaliza_el_cuerpo() throws Exception {
        when(admitir.execute(any())).thenReturn(admision("PENDIENTE_VERIFICACION", false));

        mvc.perform(post("/api/v1/admin/estudiantes/admision").contentType("application/json").content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idEstudiante").value("00000000-0000-0000-0000-0000000000e1"))
                .andExpect(jsonPath("$.datos.idUsuario").value("00000000-0000-0000-0000-0000000000a1"))
                .andExpect(jsonPath("$.datos.esUsuarioNuevo").value(true))
                .andExpect(jsonPath("$.datos.estadoCuenta").value("PENDIENTE_VERIFICACION"))
                .andExpect(jsonPath("$.datos.cuentaReparada").value(false));

        final ArgumentCaptor<AdmitirEstudianteCommandDTO> captor = ArgumentCaptor.forClass(AdmitirEstudianteCommandDTO.class);
        verify(admitir).execute(captor.capture());
        assertEquals(TIPO, captor.getValue().tipoIdentificacionId());
        assertEquals(1017123456, captor.getValue().numeroIdentificacion());
        assertEquals("Ana", captor.getValue().primerNombre());
        assertEquals(null, captor.getValue().segundoNombre());
        assertEquals("ana.perez@uco.edu.co", captor.getValue().correo());
        assertEquals(CLAVE, captor.getValue().password());
        assertEquals(PROGRAMA, captor.getValue().idPrograma());
        assertEquals(ADMIN, captor.getValue().usuarioEjecutor());
    }

    @Test
    void cuenta_reparada_se_informa_en_la_respuesta() throws Exception {
        when(admitir.execute(any())).thenReturn(admision("ACTIVA", true));

        mvc.perform(post("/api/v1/admin/estudiantes/admision").contentType("application/json")
                        .content(CUERPO.replace("\"password\":\"" + CLAVE + "\",", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.estadoCuenta").value("ACTIVA"))
                .andExpect(jsonPath("$.datos.cuentaReparada").value(true));
    }

    @Test
    void campos_obligatorios_faltantes_son_400_con_details_y_no_llegan_al_caso_de_uso() throws Exception {
        mvc.perform(post("/api/v1/admin/estudiantes/admision").contentType("application/json").content("{\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field=='tipoIdentificacionId')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='numeroIdentificacion')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='primerNombre')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='primerApellido')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='correo')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='idPrograma')]").exists());
        verify(admitir, never()).execute(any());
    }

    @Test
    void correo_con_formato_invalido_y_documento_no_positivo_son_400() throws Exception {
        mvc.perform(post("/api/v1/admin/estudiantes/admision").contentType("application/json")
                        .content(CUERPO.replace("Ana.Perez@uco.edu.co", "no-es-correo").replace("1017123456", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field=='correo')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='numeroIdentificacion')]").exists());
    }

    @Test
    void sin_cuerpo_es_400() throws Exception {
        mvc.perform(post("/api/v1/admin/estudiantes/admision").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void programa_ajeno_es_403_y_ya_admitido_o_personas_distintas_son_409() throws Exception {
        when(admitir.execute(any())).thenThrow(new ForbiddenException(AdmisionEstudianteErrorCode.ADS_003.code(),
                AdmisionEstudianteErrorCode.ADS_003.defaultMessage()));
        mvc.perform(post("/api/v1/admin/estudiantes/admision").contentType("application/json").content(CUERPO))
                .andExpect(status().isForbidden());

        doThrow(new ConflictException(AdmisionEstudianteErrorCode.ADS_002)).when(admitir).execute(any());
        mvc.perform(post("/api/v1/admin/estudiantes/admision").contentType("application/json").content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADS_002"));

        doThrow(new ConflictException(AdmisionEstudianteErrorCode.ADS_004)).when(admitir).execute(any());
        mvc.perform(post("/api/v1/admin/estudiantes/admision").contentType("application/json").content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADS_004"));
    }

    /** OBS-12: la clave no aparece en ningun evento de log ni en el 400 ni en el 500. */
    @Test
    void la_clave_no_llega_a_los_logs_ni_en_el_400_ni_en_el_500() throws Exception {
        mvc.perform(post("/api/v1/admin/estudiantes/admision").contentType("application/json")
                        .content(CUERPO.replace("Ana.Perez@uco.edu.co", "no-es-correo")))
                .andExpect(status().isBadRequest());
        when(admitir.execute(any())).thenThrow(new IllegalStateException("fallo inesperado"));
        mvc.perform(post("/api/v1/admin/estudiantes/admision").contentType("application/json").content(CUERPO))
                .andExpect(status().isInternalServerError());

        assertFalse(logs.list.isEmpty());
        for (final ILoggingEvent evento : logs.list) {
            assertFalse(evento.getFormattedMessage().contains(CLAVE), evento.getFormattedMessage());
            assertFalse(String.valueOf(evento.getThrowableProxy()).contains(CLAVE));
            assertFalse(String.valueOf(evento.getArgumentArray() == null ? "" : java.util.Arrays.toString(evento.getArgumentArray())).contains(CLAVE));
        }
    }

    @Test
    void la_peticion_no_expone_la_clave_en_to_string() {
        final var request = new co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.AdmitirEstudianteRequest();
        request.setPassword(CLAVE);
        request.setPrimerNombre("Ana");
        assertFalse(request.toString().contains(CLAVE));
        assertTrue(request.toString().contains("Ana") || !request.toString().isEmpty());
    }

    @Test
    void programas_responde_la_pagina_con_los_filtros_y_el_actor() throws Exception {
        final UUID facultad = UUID.randomUUID();
        when(programas.execute(ADMIN, "sist", facultad, 2, 10)).thenReturn(new PaginaResultado<>(
                List.of(new ProgramaInstitucionDTO(PROGRAMA, "Ingenieria de Sistemas", facultad, "Ingenieria")), 2, 10, 11));

        mvc.perform(get("/api/v1/admin/programas").param("texto", "sist").param("idFacultad", facultad.toString())
                        .param("pagina", "2").param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.elementos[0].id").value(PROGRAMA.toString()))
                .andExpect(jsonPath("$.elementos[0].nombre").value("Ingenieria de Sistemas"))
                .andExpect(jsonPath("$.elementos[0].idFacultad").value(facultad.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreFacultad").value("Ingenieria"))
                .andExpect(jsonPath("$.numeroPagina").value(2))
                .andExpect(jsonPath("$.tamanoPagina").value(10))
                .andExpect(jsonPath("$.totalElementos").value(11))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void programas_sin_parametros_usa_los_valores_por_defecto_del_caso_de_uso() throws Exception {
        when(programas.execute(ADMIN, null, null, null, null)).thenReturn(new PaginaResultado<>(List.of(), 1, 50, 0));

        mvc.perform(get("/api/v1/admin/programas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
        verify(programas).execute(ADMIN, null, null, null, null);
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new AdminAdmisionEstudianteController(null, programas, identidad));
        assertThrows(NullPointerException.class, () -> new AdminAdmisionEstudianteController(admitir, null, identidad));
        assertThrows(NullPointerException.class, () -> new AdminAdmisionEstudianteController(admitir, programas, null));
    }
}
