package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.business.TooManyRequestsException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.ResumenAsistenciaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarresumenasistencia.primaryports.ConsultarResumenAsistenciaGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.abrirqrtoma.primaryports.AbrirQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.cerrarqrtoma.primaryports.CerrarQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.QrTomaResumen;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.exception.AsistenciaQrErrorCode;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.primaryports.RegistrarAsistenciaPorTokenInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotarqrtoma.primaryports.RotarQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.primaryports.AbrirTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.primaryports.CerrarTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.AsistenciaTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.FaltanteTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomasSesionResumen;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarasistenciastoma.primaryports.ConsultarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarfaltantestoma.primaryports.ConsultarFaltantesTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultartomassesion.primaryports.ConsultarTomasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.primaryports.RegistrarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistenciaqr.EstudianteAsistenciaQrController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistenciaqr.TomaQrController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.DocenteResumenAsistenciaController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.NetworkClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CULM-001 B.2 (CONTRACT 3.2.5-3.2.8, 3.11): contrato HTTP exacto de las tomas, del QR y del resumen por horas: 201 con
 * Location, formas de respuesta, cuerpo antiguo de asistencia-qr -> 400, 403/404/409/429 con su codigo y Retry-After.
 */
class TomaAsistenciaControllersTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID GRUPO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TOMA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ESTUDIANTE = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-AbCde";
    private static final Instant APERTURA = Instant.parse("2026-11-07T13:05:00Z");

    private AbrirTomaAsistenciaInputPort abrir;
    private ConsultarTomasSesionInputPort consultarTomas;
    private CerrarTomaAsistenciaInputPort cerrar;
    private RegistrarAsistenciasTomaInputPort lote;
    private ConsultarAsistenciasTomaInputPort asistencias;
    private ConsultarFaltantesTomaInputPort faltantes;
    private AbrirQrTomaInputPort abrirQr;
    private RotarQrTomaInputPort rotarQr;
    private CerrarQrTomaInputPort cerrarQr;
    private RegistrarAsistenciaPorTokenInputPort registrarQr;
    private ConsultarResumenAsistenciaGrupoInputPort resumen;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        abrir = mock(AbrirTomaAsistenciaInputPort.class);
        consultarTomas = mock(ConsultarTomasSesionInputPort.class);
        cerrar = mock(CerrarTomaAsistenciaInputPort.class);
        lote = mock(RegistrarAsistenciasTomaInputPort.class);
        asistencias = mock(ConsultarAsistenciasTomaInputPort.class);
        faltantes = mock(ConsultarFaltantesTomaInputPort.class);
        abrirQr = mock(AbrirQrTomaInputPort.class);
        rotarQr = mock(RotarQrTomaInputPort.class);
        cerrarQr = mock(CerrarQrTomaInputPort.class);
        registrarQr = mock(RegistrarAsistenciaPorTokenInputPort.class);
        resumen = mock(ConsultarResumenAsistenciaGrupoInputPort.class);
        final AuthenticatedUserResolver usuario = () -> ACTOR;
        mvc = MockMvcBuilders.standaloneSetup(
                        new SesionTomaController(abrir, consultarTomas, usuario),
                        new TomaAsistenciaController(cerrar, lote, asistencias, faltantes, usuario),
                        new TomaQrController(abrirQr, rotarQr, cerrarQr, usuario),
                        new EstudianteAsistenciaQrController(registrarQr, new NetworkClientIpResolver(Optional::empty), usuario),
                        new DocenteResumenAsistenciaController(resumen, usuario))
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty()))
                .build();
    }

    private static TomaAsistenciaResumen toma() {
        return new TomaAsistenciaResumen(TOMA, SESION, GRUPO, 2, "QR", "ABIERTA", true, false, APERTURA, null,
                new BigDecimal("1.00"), 31, 30, 1, 0, true);
    }

    @Test
    void abrir_toma_responde_201_con_location_y_la_toma() throws Exception {
        when(abrir.execute(new AbrirTomaAsistenciaInputPort.Comando(SESION, "QR", ACTOR))).thenReturn(toma());

        mvc.perform(post("/api/v1/sesiones/{s}/tomas", SESION).contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"QR\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tomas/" + TOMA))
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idToma").value(TOMA.toString()))
                .andExpect(jsonPath("$.datos.numero").value(2))
                .andExpect(jsonPath("$.datos.modo").value("QR"))
                .andExpect(jsonPath("$.datos.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.datos.esImplicita").value(false))
                .andExpect(jsonPath("$.datos.fechaAperturaUtc").value("2026-11-07T13:05:00Z"))
                .andExpect(jsonPath("$.datos.fechaCierreUtc").isEmpty())
                .andExpect(jsonPath("$.datos.horasToma").value(1.00))
                .andExpect(jsonPath("$.datos.totalPresentes").value(30))
                .andExpect(jsonPath("$.datos.tieneQrActivo").value(true));
    }

    @Test
    void abrir_sin_cuerpo_delega_modo_nulo_y_los_rechazos_conservan_su_codigo() throws Exception {
        when(abrir.execute(new AbrirTomaAsistenciaInputPort.Comando(SESION, null, ACTOR)))
                .thenThrow(new ConflictException("TOM_003", "Ya hay una toma abierta en esta sesion."));
        mvc.perform(post("/api/v1/sesiones/{s}/tomas", SESION))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TOM_003"));

        when(abrir.execute(new AbrirTomaAsistenciaInputPort.Comando(SESION, "MANUAL", ACTOR)))
                .thenThrow(new ForbiddenException("No es titular"));
        mvc.perform(post("/api/v1/sesiones/{s}/tomas", SESION).contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"MANUAL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultar_tomas_de_la_sesion_entrega_ventana_estado_y_cancelacion() throws Exception {
        when(consultarTomas.execute(new ConsultarTomasSesionInputPort.Consulta(SESION, ACTOR))).thenReturn(new ConsultarTomasSesionInputPort.Resultado(
                EstadoSesion.CANCELADA, new CancelacionSesionDTO("DOCENTE", "Calamidad domestica", null, APERTURA),
                new TomasSesionResumen(SESION, GRUPO, new BigDecimal("2.00"), true, APERTURA, APERTURA.plusSeconds(9000), false, 4, List.of(toma()))));

        mvc.perform(get("/api/v1/sesiones/{s}/tomas", SESION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.idSesion").value(SESION.toString()))
                .andExpect(jsonPath("$.datos.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.datos.cancelacion.tipo").value("DOCENTE"))
                .andExpect(jsonPath("$.datos.ventana.aperturaUtc").value("2026-11-07T13:05:00Z"))
                .andExpect(jsonPath("$.datos.ventana.cierreUtc").value("2026-11-07T15:35:00Z"))
                .andExpect(jsonPath("$.datos.ventana.abierta").value(false))
                .andExpect(jsonPath("$.datos.horasSesion").value(2.00))
                .andExpect(jsonPath("$.datos.maximoTomas").value(4))
                .andExpect(jsonPath("$.datos.tomas[0].idToma").value(TOMA.toString()));
    }

    @Test
    void cerrar_toma_responde_la_toma_y_los_totales_y_sin_cuerpo_marca_segun_el_modo() throws Exception {
        when(cerrar.execute(new CerrarTomaAsistenciaInputPort.Comando(TOMA, false, ACTOR)))
                .thenReturn(new CerrarTomaAsistenciaInputPort.Resultado(toma(), 0, 3, false));
        mvc.perform(post("/api/v1/tomas/{t}/cierre", TOMA).contentType(MediaType.APPLICATION_JSON).content("{\"marcarAusentes\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.toma.idToma").value(TOMA.toString()))
                .andExpect(jsonPath("$.datos.totalAusentesMarcados").value(0))
                .andExpect(jsonPath("$.datos.totalFaltantes").value(3))
                .andExpect(jsonPath("$.datos.yaEstabaCerrada").value(false));

        when(cerrar.execute(new CerrarTomaAsistenciaInputPort.Comando(TOMA, null, ACTOR)))
                .thenThrow(new ResourceNotFoundException("TOM_001", "La toma de asistencia no existe."));
        mvc.perform(post("/api/v1/tomas/{t}/cierre", TOMA))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TOM_001"));
    }

    @Test
    void lote_de_la_toma_mapea_los_registros_y_responde_200() throws Exception {
        when(lote.execute(new RegistrarAsistenciasTomaInputPort.Comando(TOMA,
                List.of(new RegistrarAsistenciasTomaInputPort.Registro(ESTUDIANTE, "AN")), ACTOR)))
                .thenReturn(new RegistrarAsistenciasTomaInputPort.Resultado(TOMA, SESION, 1));

        mvc.perform(post("/api/v1/tomas/{t}/asistencias", TOMA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registros\":[{\"estudianteId\":\"" + ESTUDIANTE + "\",\"estado\":\"AN\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.idToma").value(TOMA.toString()))
                .andExpect(jsonPath("$.datos.idSesion").value(SESION.toString()))
                .andExpect(jsonPath("$.datos.totalRegistros").value(1));

        mvc.perform(post("/api/v1/tomas/{t}/asistencias", TOMA));
        verify(lote).execute(new RegistrarAsistenciasTomaInputPort.Comando(TOMA, null, ACTOR));
        mvc.perform(post("/api/v1/tomas/{t}/asistencias", TOMA).contentType(MediaType.APPLICATION_JSON).content("{\"registros\":[null]}"));
        verify(lote).execute(new RegistrarAsistenciasTomaInputPort.Comando(TOMA, java.util.Collections.singletonList(null), ACTOR));
    }

    @Test
    void asistencias_y_faltantes_son_paginas_estandar() throws Exception {
        when(asistencias.execute(new ConsultarAsistenciasTomaInputPort.Consulta(TOMA, null, null, ACTOR))).thenReturn(new PaginaResultado<>(
                List.of(new AsistenciaTomaFila(ESTUDIANTE, "Ana Perez", "AN", true, APERTURA)), 1, 50, 31));
        mvc.perform(get("/api/v1/tomas/{t}/asistencias", TOMA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.elementos[0].estudianteId").value(ESTUDIANTE.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreCompleto").value("Ana Perez"))
                .andExpect(jsonPath("$.elementos[0].estado").value("AN"))
                .andExpect(jsonPath("$.elementos[0].registradoPorEstudiante").value(true))
                .andExpect(jsonPath("$.elementos[0].fechaRegistroUtc").value("2026-11-07T13:05:00Z"))
                .andExpect(jsonPath("$.numeroPagina").value(1))
                .andExpect(jsonPath("$.tamanoPagina").value(50))
                .andExpect(jsonPath("$.totalElementos").value(31))
                .andExpect(jsonPath("$.totalPaginas").value(1));

        when(faltantes.execute(new ConsultarFaltantesTomaInputPort.Consulta(TOMA, 2, 10, ACTOR))).thenReturn(new PaginaResultado<>(
                List.of(new FaltanteTomaFila(ESTUDIANTE, "Ana Perez", true, "SJC", true)), 2, 10, 11));
        mvc.perform(get("/api/v1/tomas/{t}/faltantes", TOMA).param("pagina", "2").param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos[0].tieneRegistro").value(true))
                .andExpect(jsonPath("$.elementos[0].estado").value("SJC"))
                .andExpect(jsonPath("$.elementos[0].marcadoAlCerrar").value(true))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    private static QrTomaResumen qr() {
        return new QrTomaResumen(TOMA, SESION, GRUPO, "482913", "https://a.test/app/asistencia/qr?t=" + TOKEN, "iVBORw0",
                APERTURA, APERTURA.plusSeconds(9000), 60);
    }

    @Test
    void qr_abrir_201_rotar_200_y_cerrar_con_mensaje() throws Exception {
        when(abrirQr.execute(new AbrirQrTomaInputPort.Comando(TOMA, ACTOR))).thenReturn(qr());
        when(rotarQr.execute(new RotarQrTomaInputPort.Comando(TOMA, ACTOR))).thenReturn(qr());
        when(cerrarQr.execute(new CerrarQrTomaInputPort.Comando(TOMA, ACTOR))).thenReturn("Se dejo de proyectar el codigo QR.");

        mvc.perform(post("/api/v1/tomas/{t}/qr", TOMA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.idToma").value(TOMA.toString()))
                .andExpect(jsonPath("$.datos.idSesion").value(SESION.toString()))
                .andExpect(jsonPath("$.datos.idGrupo").value(GRUPO.toString()))
                .andExpect(jsonPath("$.datos.pin").value("482913"))
                .andExpect(jsonPath("$.datos.urlAsistencia").value("https://a.test/app/asistencia/qr?t=" + TOKEN))
                .andExpect(jsonPath("$.datos.qrPngBase64").value("iVBORw0"))
                .andExpect(jsonPath("$.datos.fechaRotacionUtc").value("2026-11-07T13:05:00Z"))
                .andExpect(jsonPath("$.datos.fechaExpiracionUtc").value("2026-11-07T15:35:00Z"))
                .andExpect(jsonPath("$.datos.rotacionSegundos").value(60));
        mvc.perform(post("/api/v1/tomas/{t}/qr/rotacion", TOMA)).andExpect(status().isOk()).andExpect(jsonPath("$.datos.pin").value("482913"));
        mvc.perform(delete("/api/v1/tomas/{t}/qr", TOMA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensaje").value("Se dejo de proyectar el codigo QR."));
    }

    private static RegistrarAsistenciaPorTokenInputPort.Resultado registro(final boolean yaRegistrado) {
        return new RegistrarAsistenciaPorTokenInputPort.Resultado(TOMA, SESION, "Arquitectura", "Grupo 1", 12, 2, yaRegistrado,
                APERTURA, "Listo");
    }

    @Test
    void registro_por_qr_nuevo_201_idempotente_200_con_la_forma_exacta() throws Exception {
        when(registrarQr.execute(new RegistrarAsistenciaPorTokenInputPort.Comando(TOKEN, null, "127.0.0.1", ACTOR))).thenReturn(registro(false));
        mvc.perform(post("/api/v1/estudiante/asistencia-qr").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + TOKEN + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.idToma").value(TOMA.toString()))
                .andExpect(jsonPath("$.datos.idSesion").value(SESION.toString()))
                .andExpect(jsonPath("$.datos.nombreAsignatura").value("Arquitectura"))
                .andExpect(jsonPath("$.datos.nombreGrupo").value("Grupo 1"))
                .andExpect(jsonPath("$.datos.numeroSesion").value(12))
                .andExpect(jsonPath("$.datos.numeroToma").value(2))
                .andExpect(jsonPath("$.datos.yaRegistrado").value(false))
                .andExpect(jsonPath("$.datos.fechaRegistroUtc").value("2026-11-07T13:05:00Z"))
                .andExpect(jsonPath("$.datos.mensaje").value("Listo"));

        when(registrarQr.execute(new RegistrarAsistenciaPorTokenInputPort.Comando(null, "482913", "127.0.0.1", ACTOR))).thenReturn(registro(true));
        mvc.perform(post("/api/v1/estudiante/asistencia-qr").contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"482913\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.datos.yaRegistrado").value(true));
    }

    @Test
    void registro_por_qr_con_el_cuerpo_antiguo_o_formato_invalido_es_400_sin_llegar_al_caso_de_uso() throws Exception {
        for (final String cuerpo : List.of("{\"sesionId\":\"" + SESION + "\",\"codigoAcceso\":\"ABC123\"}",
                "{\"token\":\"corto\"}", "{\"pin\":\"12ab56\"}", "{\"pin\":\"482913\",\"codigo\":\"X\"}")) {
            mvc.perform(post("/api/v1/estudiante/asistencia-qr").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mvc.perform(post("/api/v1/estudiante/asistencia-qr")).andExpect(status().isBadRequest());
        verify(registrarQr, never()).execute(any());
    }

    @Test
    void los_rechazos_del_registro_por_qr_conservan_codigo_estado_y_retry_after() throws Exception {
        when(registrarQr.execute(any())).thenThrow(new TooManyRequestsException(AsistenciaQrErrorCode.AQR_011));
        mvc.perform(post("/api/v1/estudiante/asistencia-qr").contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"482913\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("AQR_011"));

        doThrow(new ForbiddenException(AsistenciaQrErrorCode.AQR_002)).when(registrarQr).execute(any());
        mvc.perform(post("/api/v1/estudiante/asistencia-qr").contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"482913\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AQR_002"));

        doThrow(new ResourceNotFoundException(AsistenciaQrErrorCode.AQR_010)).when(registrarQr).execute(any());
        mvc.perform(post("/api/v1/estudiante/asistencia-qr").contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"482913\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("AQR_010"));
    }

    @Test
    void resumen_del_docente_aplana_los_indicadores_por_horas() throws Exception {
        when(resumen.execute(new ConsultarResumenAsistenciaGrupoInputPort.Consulta(GRUPO, null, null, ACTOR))).thenReturn(new PaginaResultado<>(List.of(
                new ResumenAsistenciaEstudianteDTO(ESTUDIANTE, "Ana Perez", "A", new IndicadoresAsistenciaDTO(10, new BigDecimal("20.00"),
                        new BigDecimal("16.00"), new BigDecimal("3.00"), new BigDecimal("1.00"), new BigDecimal("0.00"), new BigDecimal("15.00"),
                        new BigDecimal("80.00"), "RIESGO", new BigDecimal("15.00"), new BigDecimal("20.00")))), 1, 50, 1));

        mvc.perform(get("/api/v1/docente/grupos/{g}/resumen-asistencia", GRUPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos[0].estudianteId").value(ESTUDIANTE.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreCompleto").value("Ana Perez"))
                .andExpect(jsonPath("$.elementos[0].codigoEstadoEstudiante").value("A"))
                .andExpect(jsonPath("$.elementos[0].sesionesEjecutadas").value(10))
                .andExpect(jsonPath("$.elementos[0].horasDictadas").value(20.00))
                .andExpect(jsonPath("$.elementos[0].horasFalta").value(3.00))
                .andExpect(jsonPath("$.elementos[0].porcentajeInasistencia").value(15.00))
                .andExpect(jsonPath("$.elementos[0].porcentajeAsistencia").value(80.00))
                .andExpect(jsonPath("$.elementos[0].codigoPermanencia").value("RIESGO"))
                .andExpect(jsonPath("$.elementos[0].umbralPerdida").value(20.00))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void la_ruta_retirada_del_qr_estatico_ya_no_tiene_controller() {
        // CONTRACT 3.2.10: GET /sesiones/{id}/qr-token se retira con su controller; la ruta inexistente responde 404.
        org.junit.jupiter.api.Assertions.assertThrows(ClassNotFoundException.class,
                () -> Class.forName("co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.AsistenciaQrController"));
    }
}
