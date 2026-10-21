package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.RegistrarAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.dto.RegistrarAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.RegistrarAsistenciasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistrarAsistenciasSesionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.SolicitarRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.mapper.AsistenciaHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.ConsultarAsistenciasPorGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciaQrRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciasSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AsistenciaControllerContractTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID GROUP = UUID.randomUUID();
    private static final UUID SESSION = UUID.randomUUID();
    private static final UUID SUPPORT = UUID.randomUUID();
    private final RegistrarAsistenciaInputPort register = mock(RegistrarAsistenciaInputPort.class);
    private final RegistrarAsistenciasSesionInputPort registerBatch = mock(RegistrarAsistenciasSesionInputPort.class);
    private final SolicitarRevisionAsistenciaInputPort review = mock(SolicitarRevisionAsistenciaInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AsistenciaController(
            register, registerBatch, review, identity)).setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void registrationPreservesBodyFieldsAndReturnsCreated() throws Exception {
        mvc.perform(post("/api/v1/asistencias").contentType("application/json")
                        .content("""
                                {"estudiante":"%s","grupo":"%s","sesion":"%s",
                                 "presente":false,"observacion":"Excusa médica"}
                                """.formatted(STUDENT, GROUP, SESSION)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.exitoso").value(true));
        final var captor = ArgumentCaptor.forClass(RegistrarAsistenciaDTO.class);
        verify(register).execute(captor.capture());
        assertEquals(STUDENT, captor.getValue().getEstudiante());
        assertEquals(GROUP, captor.getValue().getGrupo());
        assertEquals(SESSION, captor.getValue().getSesion());
        assertEquals(false, captor.getValue().getPresente());
        assertEquals("Excusa médica", captor.getValue().getObservacion());
    }

    @Test
    void reviewAcceptsLegacyAliasesAndUsesAuthenticatedActor() throws Exception {
        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json")
                        .content("""
                                {"sesion":"%s","categoria":"SALUD","motivo":"Cita médica",
                                 "soporteArchivoId":"%s",
                                 "soporteNombre":"excusa.pdf","soporteUrl":"https://example.com/excusa"}
                                """.formatted(SESSION, SUPPORT)))
                .andExpect(status().isAccepted());
        final var captor = ArgumentCaptor.forClass(SolicitarRevisionAsistenciaDTO.class);
        verify(review).execute(captor.capture());
        assertEquals(SESSION, captor.getValue().getSesion());
        assertEquals(ACTOR, captor.getValue().getUsuario());
        assertEquals("SALUD", captor.getValue().getCategoria());
        assertEquals("Cita médica", captor.getValue().getJustificacion());
        // El unico dato de soporte que viaja a Application es el fileId; soporteNombre/soporteUrl legacy se ignoran.
        assertEquals(SUPPORT, captor.getValue().getSoporteArchivoId());
    }

    @Test
    void batchMapsEachStudentAndStatusAndPropagatesAuthenticatedUser() throws Exception {
        mvc.perform(post("/api/v1/asistencias/lote").contentType("application/json")
                        .content("""
                                {"sesionId":"%s","registros":[{"estudianteId":"%s","estado":"AN"}]}
                                """.formatted(SESSION, STUDENT)))
                .andExpect(status().isCreated());
        final var captor = ArgumentCaptor.forClass(RegistrarAsistenciasSesionDTO.class);
        verify(registerBatch).execute(captor.capture());
        assertEquals(SESSION, captor.getValue().getSesion());
        assertEquals(1, captor.getValue().getRegistros().size());
        assertEquals(STUDENT, captor.getValue().getRegistros().getFirst().getEstudiante());
        assertEquals("AN", captor.getValue().getRegistros().getFirst().getEstado());
        assertEquals(ACTOR, captor.getValue().getUsuarioEjecutor());
    }

    @Test
    void invalidRequestsAreRejectedBeforePorts() throws Exception {
        mvc.perform(post("/api/v1/asistencias").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(register, review);
    }

    @Test
    void mapperHandlesEmptyAndNullBatchAndQrAliases() {
        assertThrows(NullPointerException.class,
                () -> AsistenciaHttpMapper.toApplicationDTO((RegistrarAsistenciasSesionRequest) null, ACTOR));
        final RegistrarAsistenciasSesionRequest empty = new RegistrarAsistenciasSesionRequest();
        empty.setSesionId(SESSION);
        assertTrue(AsistenciaHttpMapper.toApplicationDTO(empty, ACTOR).getRegistros().isEmpty());
        assertEquals(ACTOR, AsistenciaHttpMapper.toApplicationDTO(empty, ACTOR).getUsuarioEjecutor());
        empty.setRegistros(java.util.Collections.singletonList(null));
        assertEquals(null, AsistenciaHttpMapper.toApplicationDTO(empty, ACTOR).getRegistros().getFirst().getEstudiante());

        final RegistrarAsistenciaQrRequest qr = json.readValue(
                "{\"sesionId\":\"" + SESSION + "\",\"codigoAcceso\":\"ABC123\"}",
                RegistrarAsistenciaQrRequest.class);
        final var mappedQr = AsistenciaHttpMapper.toApplicationDTO(qr, ACTOR);
        assertEquals(SESSION, mappedQr.getSesion());
        assertEquals("ABC123", mappedQr.getCodigoVerificacion());
        assertEquals(ACTOR, mappedQr.getUsuario());

        final ConsultarAsistenciasPorGrupoRequest query = json.readValue(
                "{\"grupo\":\"" + GROUP + "\",\"sesion\":\"" + SESSION + "\"}",
                ConsultarAsistenciasPorGrupoRequest.class);
        assertEquals(GROUP, AsistenciaHttpMapper.toApplicationDTO(query, ACTOR).getGrupo());
        assertEquals(SESSION, AsistenciaHttpMapper.toApplicationDTO(query, ACTOR).getSesion());
        assertEquals(ACTOR, AsistenciaHttpMapper.toApplicationDTO(query, ACTOR).getUsuarioEjecutor());
    }
}
