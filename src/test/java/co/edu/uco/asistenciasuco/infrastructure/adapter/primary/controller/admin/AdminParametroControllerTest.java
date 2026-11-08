package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarparametro.primaryports.ActualizarParametroInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcambiosparametro.primaryports.ConsultarCambiosParametroInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.ConsultarParametrosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.exception.ParametroErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CULM-001 D.3 (CONTRACT v2 §4.3, HU168): JSON exacto de GET/PUT de parametros, errores y auditoria con anterior y nuevo. */
class AdminParametroControllerTest {

    private static final UUID ADMIN = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID PARAMETRO = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    private final ConsultarParametrosInputPort consultar = mock(ConsultarParametrosInputPort.class);
    private final ActualizarParametroInputPort actualizar = mock(ActualizarParametroInputPort.class);
    private final ConsultarCambiosParametroInputPort cambios = mock(ConsultarCambiosParametroInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ADMIN;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AdminParametroController(consultar, actualizar, cambios, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    private static ConsultarParametrosInputPort.Parametro parametro(final BigDecimal minimo, final BigDecimal maximo,
                                                                    final List<String> permitidos, final Instant ultimoCambio) {
        return new ConsultarParametrosInputPort.Parametro(PARAMETRO, "ASISTENCIA", "TOLERANCIA_MINUTOS", "15", "15", "INT", true,
                minimo, maximo, permitidos, "Tolerancia de apertura", "Minutos antes del inicio.", "minutos", "Asistencia", 10,
                Instant.parse("2026-11-07T15:04:05.007Z"), ultimoCambio, ultimoCambio == null ? null : "Ana Admin");
    }

    @Test
    void get_parametros_es_un_api_page_response_con_los_campos_legibles() throws Exception {
        when(consultar.execute(new ConsultarParametrosInputPort.Comando(2, 20, "ASISTENCIA", "tol", true, ADMIN))).thenReturn(
                new ConsultarParametrosInputPort.Resultado(List.of(
                        parametro(new BigDecimal("0.0000"), new BigDecimal("60.0000"), List.of(), Instant.parse("2026-11-07T16:00:00Z")),
                        parametro(new BigDecimal("1.5000"), null, List.of("true", "false"), null)), 2, 20, 41));

        mvc.perform(get("/api/v1/admin/parametros").param("pagina", "2").param("tamano", "20").param("grupo", "ASISTENCIA")
                        .param("texto", "tol").param("soloEditables", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.numeroPagina").value(2))
                .andExpect(jsonPath("$.tamanoPagina").value(20))
                .andExpect(jsonPath("$.totalElementos").value(41))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.elementos[0].id").value(PARAMETRO.toString()))
                .andExpect(jsonPath("$.elementos[0].grupo").value("ASISTENCIA"))
                .andExpect(jsonPath("$.elementos[0].clave").value("TOLERANCIA_MINUTOS"))
                .andExpect(jsonPath("$.elementos[0].valor").value("15"))
                .andExpect(jsonPath("$.elementos[0].valorDefecto").value("15"))
                .andExpect(jsonPath("$.elementos[0].tipoDato").value("INT"))
                .andExpect(jsonPath("$.elementos[0].esEditable").value(true))
                .andExpect(jsonPath("$.elementos[0].valorMinimo").value(0))
                .andExpect(jsonPath("$.elementos[0].valorMaximo").value(60))
                .andExpect(jsonPath("$.elementos[0].valoresPermitidos").isEmpty())
                .andExpect(jsonPath("$.elementos[0].etiqueta").value("Tolerancia de apertura"))
                .andExpect(jsonPath("$.elementos[0].descripcion").value("Minutos antes del inicio."))
                .andExpect(jsonPath("$.elementos[0].unidad").value("minutos"))
                .andExpect(jsonPath("$.elementos[0].categoriaVisual").value("Asistencia"))
                .andExpect(jsonPath("$.elementos[0].orden").value(10))
                .andExpect(jsonPath("$.elementos[0].fechaModificacion").value("2026-11-07T15:04:05.007Z"))
                .andExpect(jsonPath("$.elementos[0].fechaUltimoCambioUtc").value("2026-11-07T16:00:00.000Z"))
                .andExpect(jsonPath("$.elementos[0].nombreUltimoActor").value("Ana Admin"))
                .andExpect(jsonPath("$.elementos[1].valorMinimo").value(1.5))
                .andExpect(jsonPath("$.elementos[1].valorMaximo").value(nullValue()))
                .andExpect(jsonPath("$.elementos[1].valoresPermitidos[0]").value("true"))
                .andExpect(jsonPath("$.elementos[1].valoresPermitidos[1]").value("false"))
                .andExpect(jsonPath("$.elementos[1].fechaUltimoCambioUtc").value(nullValue()))
                .andExpect(jsonPath("$.elementos[1].nombreUltimoActor").value(nullValue()));
    }

    @Test
    void get_parametros_sin_filtros_pasa_nulos_y_soloEditables_nulo() throws Exception {
        when(consultar.execute(new ConsultarParametrosInputPort.Comando(null, null, null, null, null, ADMIN))).thenReturn(
                new ConsultarParametrosInputPort.Resultado(List.of(), 1, 20, 0));

        mvc.perform(get("/api/v1/admin/parametros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos").isEmpty())
                .andExpect(jsonPath("$.totalPaginas").value(0));
        when(consultar.execute(new ConsultarParametrosInputPort.Comando(null, null, null, null, false, ADMIN))).thenReturn(
                new ConsultarParametrosInputPort.Resultado(List.of(), 1, 20, 0));
        mvc.perform(get("/api/v1/admin/parametros").param("soloEditables", "false"))
                .andExpect(status().isOk());
        verify(consultar).execute(new ConsultarParametrosInputPort.Comando(null, null, null, null, false, ADMIN));
    }

    @Test
    void get_parametros_con_una_base_con_dos_instituciones_responde_409() throws Exception {
        when(consultar.execute(any())).thenThrow(new ConflictException(ParametroErrorCode.PRM_012));

        mvc.perform(get("/api/v1/admin/parametros"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRM_012"));
    }

    @Test
    void put_parametro_devuelve_anterior_nuevo_y_la_version_con_milisegundos_y_reenvia_la_fecha_tal_como_llego() throws Exception {
        when(actualizar.execute(any())).thenReturn(new ActualizarParametroInputPort.Resultado(PARAMETRO, "ASISTENCIA",
                "TOLERANCIA_MINUTOS", "15", "20", Instant.parse("2026-11-07T16:00:00.003Z"), false));

        mvc.perform(put("/api/v1/admin/parametros/{id}", PARAMETRO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":\"20\",\"fechaModificacionEsperada\":\"2026-11-07T15:04:05.007Z\",\"motivo\":\"Ajuste de prueba\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.id").value(PARAMETRO.toString()))
                .andExpect(jsonPath("$.datos.grupo").value("ASISTENCIA"))
                .andExpect(jsonPath("$.datos.clave").value("TOLERANCIA_MINUTOS"))
                .andExpect(jsonPath("$.datos.valorAnterior").value("15"))
                .andExpect(jsonPath("$.datos.valorNuevo").value("20"))
                .andExpect(jsonPath("$.datos.fechaModificacion").value("2026-11-07T16:00:00.003Z"))
                .andExpect(jsonPath("$.datos.sinCambios").value(false));

        final ArgumentCaptor<ActualizarParametroInputPort.Comando> comando = ArgumentCaptor.forClass(ActualizarParametroInputPort.Comando.class);
        verify(actualizar).execute(comando.capture());
        assertEquals(new ActualizarParametroInputPort.Comando(PARAMETRO, "20", Instant.parse("2026-11-07T15:04:05.007Z"),
                "Ajuste de prueba", ADMIN), comando.getValue());
    }

    @Test
    void put_parametro_sin_cambios_serializa_sinCambios_true() throws Exception {
        when(actualizar.execute(any())).thenReturn(new ActualizarParametroInputPort.Resultado(PARAMETRO, "ASISTENCIA",
                "TOLERANCIA_MINUTOS", "20", "20", Instant.parse("2026-11-07T15:04:05Z"), true));

        mvc.perform(put("/api/v1/admin/parametros/{id}", PARAMETRO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":\"20\",\"fechaModificacionEsperada\":\"2026-11-07T15:04:05Z\",\"motivo\":\"Mismo valor de prueba\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.sinCambios").value(true))
                .andExpect(jsonPath("$.datos.fechaModificacion").value("2026-11-07T15:04:05.000Z"));
    }

    @Test
    void put_parametro_con_cuerpo_invalido_responde_400_sin_llegar_al_caso_de_uso() throws Exception {
        for (final String cuerpo : List.of(
                "{}",
                "{\"valor\":\"20\",\"motivo\":\"Ajuste de prueba\"}",
                "{\"valor\":\"20\",\"fechaModificacionEsperada\":\"ayer\",\"motivo\":\"Ajuste de prueba\"}",
                "{\"valor\":\"20\",\"fechaModificacionEsperada\":\"2026-11-07T15:04:05.007Z\"}",
                "{\"valor\":\"20\",\"fechaModificacionEsperada\":\"2026-11-07T15:04:05.007Z\",\"motivo\":\"" + "x".repeat(501) + "\"}",
                "{\"fechaModificacionEsperada\":\"2026-11-07T15:04:05.007Z\",\"motivo\":\"Ajuste de prueba\"}")) {
            mvc.perform(put("/api/v1/admin/parametros/{id}", PARAMETRO).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/v1/admin/parametros/{id}", PARAMETRO)).andExpect(status().isBadRequest());
        verifyNoInteractions(actualizar);
    }

    @Test
    void put_parametro_traduce_los_rechazos_de_la_base_de_datos_a_su_http() throws Exception {
        final String cuerpo = "{\"valor\":\"20\",\"fechaModificacionEsperada\":\"2026-11-07T15:04:05.007Z\",\"motivo\":\"Ajuste de prueba\"}";
        when(actualizar.execute(any()))
                .thenThrow(new ValidationException(ParametroErrorCode.PRM_005))
                .thenThrow(new ForbiddenException(ParametroErrorCode.PRM_003))
                .thenThrow(new ConflictException(ParametroErrorCode.PRM_008))
                .thenThrow(new ConflictException(ParametroErrorCode.PRM_010));

        mvc.perform(put("/api/v1/admin/parametros/{id}", PARAMETRO).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PRM_005"));
        mvc.perform(put("/api/v1/admin/parametros/{id}", PARAMETRO).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PRM_003"));
        mvc.perform(put("/api/v1/admin/parametros/{id}", PARAMETRO).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PRM_008"));
        mvc.perform(put("/api/v1/admin/parametros/{id}", PARAMETRO).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PRM_010"));
    }

    @Test
    void get_cambios_es_un_api_page_response_del_historial() throws Exception {
        final UUID correlacion = UUID.randomUUID();
        when(cambios.execute(new ConsultarCambiosParametroInputPort.Comando(PARAMETRO, 1, 10, ADMIN))).thenReturn(
                new ConsultarCambiosParametroInputPort.Resultado(List.of(new ConsultarCambiosParametroInputPort.Cambio(
                        UUID.fromString("cccccccc-0000-0000-0000-000000000001"), "15", "20", "Ajuste de prueba", ADMIN, "Ana Admin",
                        correlacion, Instant.parse("2026-11-07T16:00:00.123Z"))), 1, 10, 1));

        mvc.perform(get("/api/v1/admin/parametros/{id}/cambios", PARAMETRO).param("pagina", "1").param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.elementos[0].valorAnterior").value("15"))
                .andExpect(jsonPath("$.elementos[0].valorNuevo").value("20"))
                .andExpect(jsonPath("$.elementos[0].motivo").value("Ajuste de prueba"))
                .andExpect(jsonPath("$.elementos[0].idUsuarioEjecutor").value(ADMIN.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreEjecutor").value("Ana Admin"))
                .andExpect(jsonPath("$.elementos[0].idCorrelacion").value(correlacion.toString()))
                .andExpect(jsonPath("$.elementos[0].fechaCambioUtc").value("2026-11-07T16:00:00.123Z"));
    }

    @Test
    void la_edicion_se_audita_con_el_valor_anterior_y_el_nuevo_y_el_motivo() throws NoSuchMethodException {
        final Method metodo = AdminParametroController.class.getMethod("actualizarParametro", UUID.class,
                AdminParametroController.ActualizarParametroRequest.class);
        final AuditableOperation auditoria = metodo.getAnnotation(AuditableOperation.class);

        assertNotNull(auditoria);
        assertEquals("ACTUALIZAR_PARAMETRO", auditoria.action());
        assertEquals("PARAMETRO", auditoria.resourceType());
        assertEquals("id", auditoria.resourceIdPathVariable());
        assertArrayEquals(new String[]{"grupo", "clave", "valorAnterior", "valorNuevo"}, auditoria.metadataResponseFields());
        assertArrayEquals(new String[]{"motivo"}, auditoria.metadataRequestFields());
    }
}
