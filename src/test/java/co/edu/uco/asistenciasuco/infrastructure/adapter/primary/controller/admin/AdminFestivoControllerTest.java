package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarfestivos.primaryports.ConsultarFestivosAnioInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarsesionesfestivo.primaryports.ConsultarSesionesFestivoConAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.exception.FestivoErrorCode;
import co.edu.uco.asistenciasuco.application.features.festivos.recargarfestivos.primaryports.RecargarFestivosAnioInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CULM-001 B.1 (CONTRACT 3.2.2): forma exacta de las respuestas de festivos del administrador y errores 400. */
class AdminFestivoControllerTest {

    private static final UUID ADMIN = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    private final ConsultarFestivosAnioInputPort consultar = mock(ConsultarFestivosAnioInputPort.class);
    private final RecargarFestivosAnioInputPort recargar = mock(RecargarFestivosAnioInputPort.class);
    private final ConsultarSesionesFestivoConAsistenciaInputPort sesiones = mock(ConsultarSesionesFestivoConAsistenciaInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ADMIN;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AdminFestivoController(consultar, recargar, sesiones, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void get_festivos_entrega_la_carga_la_atribucion_y_los_festivos() throws Exception {
        when(consultar.execute(new ConsultarFestivosAnioInputPort.Comando(2026, false, ADMIN))).thenReturn(
                new ConsultarFestivosAnioInputPort.Resultado(2026, "CALENDARIOS_NACIONALES", LocalDateTime.of(2026, 11, 7, 5, 0), 0,
                        "Fuente: Calendarios Nacionales", "https://calendariosnacionales.com",
                        List.of(new ConsultarFestivosAnioInputPort.Festivo(LocalDate.of(2026, 1, 1), "Año Nuevo",
                                "CALENDARIOS_NACIONALES", true)), 19));

        mvc.perform(get("/api/v1/admin/festivos").param("anio", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.anio").value(2026))
                .andExpect(jsonPath("$.datos.fuenteUltimaCarga").value("CALENDARIOS_NACIONALES"))
                .andExpect(jsonPath("$.datos.fechaUltimaCargaUtc").value("2026-11-07T05:00:00Z"))
                .andExpect(jsonPath("$.datos.diferenciasCalculoLocal").value(0))
                .andExpect(jsonPath("$.datos.atribucion.texto").value("Fuente: Calendarios Nacionales"))
                .andExpect(jsonPath("$.datos.atribucion.url").value("https://calendariosnacionales.com"))
                .andExpect(jsonPath("$.datos.festivos[0].fecha").value("2026-01-01"))
                .andExpect(jsonPath("$.datos.festivos[0].nombre").value("Año Nuevo"))
                .andExpect(jsonPath("$.datos.festivos[0].fuente").value("CALENDARIOS_NACIONALES"))
                .andExpect(jsonPath("$.datos.festivos[0].estaActivo").value(true))
                .andExpect(jsonPath("$.datos.totalFestivos").value(19));
    }

    @Test
    void get_festivos_sin_carga_serializa_nulos_explicitos() throws Exception {
        when(consultar.execute(new ConsultarFestivosAnioInputPort.Comando(2030, true, ADMIN))).thenReturn(
                new ConsultarFestivosAnioInputPort.Resultado(2030, null, null, null, "t", "u",
                        List.of(new ConsultarFestivosAnioInputPort.Festivo(null, "x", "CALCULO_LOCAL", false)), 0));

        mvc.perform(get("/api/v1/admin/festivos").param("anio", "2030").param("incluirInactivos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.fuenteUltimaCarga").value(nullValue()))
                .andExpect(jsonPath("$.datos.fechaUltimaCargaUtc").value(nullValue()))
                .andExpect(jsonPath("$.datos.festivos[0].fecha").value(nullValue()));
    }

    @Test
    void get_festivos_sin_anio_o_fuera_de_rango_responde_400() throws Exception {
        mvc.perform(get("/api/v1/admin/festivos")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/festivos").param("anio", "abc")).andExpect(status().isBadRequest());
        when(consultar.execute(any())).thenThrow(new ValidationException(FestivoErrorCode.FES_001));
        mvc.perform(get("/api/v1/admin/festivos").param("anio", "1800"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FES_001"));
    }

    @Test
    void post_recarga_entrega_la_fuente_los_conteos_y_la_aplicacion() throws Exception {
        when(recargar.execute(new RecargarFestivosAnioInputPort.Comando(2026, ADMIN))).thenReturn(
                new RecargarFestivosAnioInputPort.Resultado(2026, "CALENDARIOS_NACIONALES", false, 19, 0, 19, 0, 0,
                        new RecargarFestivosAnioInputPort.Aplicacion(12, 3, 1)));

        mvc.perform(post("/api/v1/admin/festivos/{anio}/recarga", 2026))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.anio").value(2026))
                .andExpect(jsonPath("$.datos.fuente").value("CALENDARIOS_NACIONALES"))
                .andExpect(jsonPath("$.datos.usoRespaldo").value(false))
                .andExpect(jsonPath("$.datos.totalFestivos").value(19))
                .andExpect(jsonPath("$.datos.festivosInsertados").value(0))
                .andExpect(jsonPath("$.datos.festivosActualizados").value(19))
                .andExpect(jsonPath("$.datos.festivosDesactivados").value(0))
                .andExpect(jsonPath("$.datos.diferenciasCalculoLocal").value(0))
                .andExpect(jsonPath("$.datos.aplicacion.gruposProcesados").value(12))
                .andExpect(jsonPath("$.datos.aplicacion.sesionesCanceladas").value(3))
                .andExpect(jsonPath("$.datos.aplicacion.sesionesConAsistencia").value(1));
    }

    @Test
    void get_sesiones_con_asistencia_es_un_api_page_response() throws Exception {
        final UUID sesion = UUID.randomUUID();
        when(sesiones.execute(new ConsultarSesionesFestivoConAsistenciaInputPort.Comando(2026, 2, 20, ADMIN))).thenReturn(
                new ConsultarSesionesFestivoConAsistenciaInputPort.Resultado(List.of(
                        new ConsultarSesionesFestivoConAsistenciaInputPort.SesionFestivo(sesion, UUID.randomUUID(), "G01 de Calculo",
                                "Calculo", "Ana Docente", LocalDateTime.of(2026, 3, 23, 13, 0), LocalDate.of(2026, 3, 23),
                                "San José", 1, 30),
                        new ConsultarSesionesFestivoConAsistenciaInputPort.SesionFestivo(UUID.randomUUID(), UUID.randomUUID(),
                                "G02", "Fisica", "Otro", null, null, "San José", 0, 1)), 2, 20, 41));

        mvc.perform(get("/api/v1/admin/festivos/{anio}/sesiones-con-asistencia", 2026).param("pagina", "2").param("tamano", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.numeroPagina").value(2))
                .andExpect(jsonPath("$.tamanoPagina").value(20))
                .andExpect(jsonPath("$.totalElementos").value(41))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.elementos[0].idSesion").value(sesion.toString()))
                .andExpect(jsonPath("$.elementos[0].fechaHoraInicioUtc").value("2026-03-23T13:00:00Z"))
                .andExpect(jsonPath("$.elementos[0].fecha").value("2026-03-23"))
                .andExpect(jsonPath("$.elementos[0].nombreFestivo").value("San José"))
                .andExpect(jsonPath("$.elementos[0].totalTomas").value(1))
                .andExpect(jsonPath("$.elementos[0].totalRegistros").value(30))
                .andExpect(jsonPath("$.elementos[1].fechaHoraInicioUtc").value(nullValue()))
                .andExpect(jsonPath("$.elementos[1].fecha").value(nullValue()));
    }

    @Test
    void get_sesiones_sin_paginacion_delega_los_nulos_a_application() throws Exception {
        when(sesiones.execute(new ConsultarSesionesFestivoConAsistenciaInputPort.Comando(2026, null, null, ADMIN))).thenReturn(
                new ConsultarSesionesFestivoConAsistenciaInputPort.Resultado(List.of(), 1, 20, 0));

        mvc.perform(get("/api/v1/admin/festivos/{anio}/sesiones-con-asistencia", 2026))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaginas").value(0))
                .andExpect(jsonPath("$.elementos").isEmpty());
    }

    @Test
    void el_constructor_exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new AdminFestivoController(null, recargar, sesiones, identity));
        assertThrows(NullPointerException.class, () -> new AdminFestivoController(consultar, null, sesiones, identity));
        assertThrows(NullPointerException.class, () -> new AdminFestivoController(consultar, recargar, null, identity));
        assertThrows(NullPointerException.class, () -> new AdminFestivoController(consultar, recargar, sesiones, null));
        verifyNoInteractions(consultar, recargar, sesiones);
    }
}
