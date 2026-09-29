package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * LB-002.2C — CMD-ADP-001..015: contrato del candidato JPA con {@code EntityManagerFactory},
 * {@code EntityManager} y {@code StoredProcedureQuery} como fakes. NO certifica JPA ni SQL Server real
 * (eso es la paridad de LB-002.2D); solo el contrato observable del adapter: JSON, binding posicional,
 * consumo del resultado, traduccion de errores, logging y ciclo de vida del EntityManager.
 */
class AsistenciaJpaCommandPersistenceTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SESION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EST_1 = UUID.fromString("44444444-4444-4444-4444-444444444441");
    private static final UUID EST_2 = UUID.fromString("44444444-4444-4444-4444-444444444442");
    private static final UUID EST_3 = UUID.fromString("44444444-4444-4444-4444-444444444443");
    private static final String SP = "dbo.usp_registrar_asistencias_sesion";
    private static final String GENERIC_MESSAGE = "No fue posible ejecutar el procedimiento almacenado.";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final StoredProcedureQuery query = mock(StoredProcedureQuery.class);
    private final AsistenciaJpaCommandPersistence persistence =
            new AsistenciaJpaCommandPersistence(entityManagerFactory);

    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.createStoredProcedureQuery(SP)).thenReturn(query);
        when(query.getUpdateCount()).thenReturn(-1);
        stubResultSet(fila(CORRELATION.toString(), Boolean.TRUE));

        logger = (Logger) LoggerFactory.getLogger(AsistenciaJpaCommandPersistence.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void limpiar() {
        logger.detachAppender(logAppender);
        CorrelationIdContext.clear();
    }

    // ------------------------------------------------------------------ soporte

    private static Object[] fila(final Object correlacion, final Object estado) {
        return new Object[]{correlacion, "mensaje usuario", "DBCODE=X|detalle", estado};
    }

    /** execute()=true con un unico result set con las filas dadas y sin mas resultados. */
    private void stubResultSet(final Object[]... filas) {
        when(query.execute()).thenReturn(true);
        when(query.getResultList()).thenReturn(List.of((Object[][]) filas));
        when(query.hasMoreResults()).thenReturn(false);
    }

    private void stubRechazo(final String mensajeTecnico) {
        when(query.execute()).thenReturn(true);
        when(query.getResultList()).thenReturn(
                List.of((Object) new Object[]{CORRELATION.toString(), "mensaje", mensajeTecnico, Boolean.FALSE}));
        when(query.hasMoreResults()).thenReturn(false);
    }

    private static RegistrarAsistenciasSesionRepositoryDTO lote(
            final UUID ejecutor,
            final RegistroAsistenciaSesionRepositoryDTO... registros
    ) {
        return new RegistrarAsistenciasSesionRepositoryDTO(SESION, List.of(registros), ejecutor);
    }

    private static RegistrarAsistenciasSesionRepositoryDTO loteAnSjcEx() {
        return lote(EJECUTOR,
                new RegistroAsistenciaSesionRepositoryDTO(EST_1, "AN"),
                new RegistroAsistenciaSesionRepositoryDTO(EST_2, "SJC"),
                new RegistroAsistenciaSesionRepositoryDTO(EST_3, "EX"));
    }

    private String jsonEnviado() {
        final ArgumentCaptor<Object> valor = ArgumentCaptor.forClass(Object.class);
        verify(query).setParameter(eq(2), valor.capture());
        return (String) valor.getValue();
    }

    private static void assertJsonContractual(final String json, final List<UUID> estudiantes, final List<String> estados) {
        final JsonNode root = JSON.readTree(json);
        assertTrue(root.isArray());
        assertEquals(estudiantes.size(), root.size());
        for (int i = 0; i < estudiantes.size(); i++) {
            final JsonNode item = root.get(i);
            assertEquals(2, item.size(), "Cada elemento tiene EXACTAMENTE dos claves.");
            assertEquals(estudiantes.get(i).toString(), item.get("idEstudiante").asString());
            assertEquals(estados.get(i), item.get("estado").asString());
        }
    }

    private List<ILoggingEvent> eventosDeError() {
        return logAppender.list.stream().filter(event -> event.getLevel() == Level.ERROR).toList();
    }

    // ------------------------------------------------------------------ CMD-ADP-001

    @Test
    void CMD_ADP_001_dto_nulo_lanza_la_misma_excepcion_del_baseline_sin_abrir_entity_manager() {
        final CrosscuttingException exception =
                assertThrows(CrosscuttingException.class, () -> persistence.registrarAsistenciasSesion(null));

        assertEquals("El dominio para registrar asistencias por sesion es obligatorio.", exception.getMessage());
        verifyNoInteractions(entityManagerFactory);
    }

    @Test
    void sin_correlacion_en_el_contexto_falla_antes_de_abrir_entity_manager_y_no_genera_una_nueva() {
        CorrelationIdContext.clear();

        assertThrows(CrosscuttingException.class, () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        verifyNoInteractions(entityManagerFactory);
        assertNull(CorrelationIdContext.get(), "El candidato no debe generar un correlationId propio.");
    }

    @Test
    void exige_entity_manager_factory() {
        assertThrows(NullPointerException.class, () -> new AsistenciaJpaCommandPersistence(null));
    }

    // ------------------------------------------------------------------ CMD-ADP-002 / 003 / 004

    @Test
    void CMD_ADP_002_json_contractual_array_de_objetos_con_exactamente_dos_claves_en_el_orden_del_dto() {
        persistence.registrarAsistenciasSesion(loteAnSjcEx());

        assertJsonContractual(jsonEnviado(), List.of(EST_1, EST_2, EST_3), List.of("AN", "SJC", "EX"));
    }

    @Test
    void CMD_ADP_003_el_json_es_estructuralmente_igual_al_que_envia_el_baseline_jdbc() {
        final CanonicalStoredProcedureExecutor executor = mock(CanonicalStoredProcedureExecutor.class);
        when(executor.execute(anyString(), anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(new CanonicalProcedureResult(CORRELATION, "ok", "ok", true));
        final AsistenciaRepositorySqlServerAdapter baseline =
                new AsistenciaRepositorySqlServerAdapter(mock(NamedParameterJdbcOperations.class), executor);
        final RegistrarAsistenciasSesionRepositoryDTO dto = loteAnSjcEx();

        baseline.registrarAsistenciasSesion(dto);
        persistence.registrarAsistenciasSesion(dto);

        final ArgumentCaptor<MapSqlParameterSource> parametros = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(executor).execute(eq("registrarAsistenciasSesion"), anyString(), parametros.capture());
        final String jsonBaseline = (String) parametros.getValue().getValue("asistenciaJSON");
        assertEquals(JSON.readTree(jsonBaseline), JSON.readTree(jsonEnviado()),
                "Se compara el JSON PARSEADO: el orden de claves de Map.of no es determinista entre JVM.");
    }

    @Test
    void CMD_ADP_004_no_normaliza_ni_valida_el_estado_los_valores_viajan_tal_cual() {
        persistence.registrarAsistenciasSesion(lote(EJECUTOR,
                new RegistroAsistenciaSesionRepositoryDTO(EST_1, "ABC"),
                new RegistroAsistenciaSesionRepositoryDTO(EST_2, " an "),
                new RegistroAsistenciaSesionRepositoryDTO(EST_3, "ex")));

        assertJsonContractual(jsonEnviado(), List.of(EST_1, EST_2, EST_3), List.of("ABC", " an ", "ex"));
    }

    @Test
    void CMD_ADP_004_lista_vacia_viaja_como_array_vacio_y_la_decision_es_de_la_db() {
        persistence.registrarAsistenciasSesion(lote(EJECUTOR));

        assertEquals("[]", jsonEnviado());
    }

    @Test
    void estado_nulo_falla_en_serializacion_como_el_baseline_sin_abrir_entity_manager() {
        final CrosscuttingException exception = assertThrows(CrosscuttingException.class,
                () -> persistence.registrarAsistenciasSesion(
                        lote(EJECUTOR, new RegistroAsistenciaSesionRepositoryDTO(EST_1, null))));

        assertEquals("No fue posible serializar los registros de asistencia.", exception.getMessage());
        verifyNoInteractions(entityManagerFactory);
    }

    @Test
    void estudiante_nulo_falla_en_serializacion_como_el_baseline_sin_abrir_entity_manager() {
        assertThrows(CrosscuttingException.class,
                () -> persistence.registrarAsistenciasSesion(
                        lote(EJECUTOR, new RegistroAsistenciaSesionRepositoryDTO(null, "AN"))));

        verifyNoInteractions(entityManagerFactory);
    }

    // ------------------------------------------------------------------ CMD-ADP-005 / 006

    @Test
    void CMD_ADP_005_registra_y_enlaza_los_cuatro_parametros_por_posicion_en_el_orden_de_sys_parameters() {
        persistence.registrarAsistenciasSesion(loteAnSjcEx());

        final InOrder orden = inOrder(entityManager, query);
        orden.verify(entityManager).createStoredProcedureQuery(SP);
        orden.verify(query).registerStoredProcedureParameter(1, UUID.class, ParameterMode.IN);
        orden.verify(query).registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
        orden.verify(query).registerStoredProcedureParameter(3, UUID.class, ParameterMode.IN);
        orden.verify(query).registerStoredProcedureParameter(4, UUID.class, ParameterMode.IN);
        orden.verify(query).setParameter(1, SESION);
        orden.verify(query).setParameter(eq(2), any(String.class));
        orden.verify(query).setParameter(3, CORRELATION);
        orden.verify(query).setParameter(4, EJECUTOR);
        orden.verify(query).execute();
    }

    @Test
    void CMD_ADP_005_no_usa_binding_nominal_ni_registra_ningun_parametro_extra() {
        persistence.registrarAsistenciasSesion(loteAnSjcEx());

        verify(query, never()).registerStoredProcedureParameter(anyString(), any(Class.class), any(ParameterMode.class));
        verify(query, never()).setParameter(anyString(), any());
        verify(query, times(4)).registerStoredProcedureParameter(anyInt(), any(Class.class), any(ParameterMode.class));
        verify(query, times(4)).setParameter(anyInt(), any());
    }

    @Test
    void CMD_ADP_005_la_correlacion_enviada_es_la_del_contexto() {
        final UUID otra = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        CorrelationIdContext.set(otra);
        stubResultSet(fila(otra.toString(), Boolean.TRUE));

        persistence.registrarAsistenciasSesion(loteAnSjcEx());

        verify(query).setParameter(3, otra);
    }

    @Test
    void CMD_ADP_006_usuario_ejecutor_nulo_permanece_nulo_para_que_la_db_responda_GEN_002() {
        persistence.registrarAsistenciasSesion(lote(null, new RegistroAsistenciaSesionRepositoryDTO(EST_1, "AN")));

        verify(query).setParameter(4, (Object) null);
    }

    // ------------------------------------------------------------------ CMD-ADP-007

    @ParameterizedTest(name = "variante={0}")
    @ValueSource(strings = {"boolean", "uuid+numero-1", "string-true", "string-1"})
    void CMD_ADP_007_mapeo_tolerante_del_resultado_canonico(final String variante) {
        stubResultSet(switch (variante) {
            case "boolean" -> fila(CORRELATION.toString(), Boolean.TRUE);
            case "uuid+numero-1" -> fila(CORRELATION, 1);
            case "string-true" -> fila(CORRELATION.toString(), "true");
            default -> fila(CORRELATION, "1");
        });

        persistence.registrarAsistenciasSesion(loteAnSjcEx());

        verify(entityManager).close();
    }

    @ParameterizedTest(name = "estadoResultado={0}")
    @ValueSource(strings = {"false", "0", "desconocido"})
    void CMD_ADP_007_estado_no_exitoso_o_desconocido_falla_cerrado_y_no_se_trata_como_exito(final String estado) {
        final Object valor = switch (estado) {
            case "false" -> Boolean.FALSE;
            case "0" -> 0;
            default -> estado;
        };
        stubResultSet(fila(CORRELATION.toString(), valor));

        assertThrows(DatabaseOperationException.class, () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));
    }

    @Test
    void CMD_ADP_007_estado_nulo_no_se_trata_como_exito() {
        stubResultSet(fila(CORRELATION.toString(), null));

        assertThrows(DatabaseOperationException.class, () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));
    }

    @Test
    void valor_de_correlacion_no_parseable_es_fallo_tecnico_traducido_no_excepcion_cruda() {
        stubResultSet(fila("no-es-un-uuid", Boolean.TRUE));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        verify(entityManager).close();
    }

    // ------------------------------------------------------------------ CMD-ADP-008 / 009

    @Test
    void CMD_ADP_008_result_set_vacio_es_violacion_del_contrato_canonico() {
        stubResultSet();

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    @Test
    void CMD_ADP_008_sin_result_set_ni_update_count_es_violacion_del_contrato_canonico_no_fallo_tecnico() {
        when(query.execute()).thenReturn(false);
        when(query.getUpdateCount()).thenReturn(-1);

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
        assertNull(exception.getCause(), "No es una excepcion de Hibernate: es ausencia de canal canonico.");
        assertTrue(eventosDeError().isEmpty(), "La violacion del contrato no se registra como fallo tecnico de SQL.");
    }

    @Test
    void CMD_ADP_008_dos_filas_canonicas_es_violacion_del_contrato_y_no_toma_la_primera() {
        stubResultSet(fila(CORRELATION.toString(), Boolean.TRUE), fila(CORRELATION.toString(), Boolean.TRUE));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    @Test
    void CMD_ADP_008_filas_que_no_son_de_cuatro_columnas_no_forman_parte_del_canal_canonico() {
        when(query.execute()).thenReturn(true);
        when(query.getResultList()).thenReturn(List.of("escalar", new Object[]{"a", "b"}));
        when(query.hasMoreResults()).thenReturn(false);

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
    }

    @Test
    void CMD_ADP_009_correlacion_devuelta_distinta_es_violacion_del_contrato_canonico() {
        stubResultSet(fila(UUID.randomUUID().toString(), Boolean.TRUE));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
        assertEquals("La correlacion retornada por la DB no coincide con la peticion.", exception.getMessage());
    }

    // ------------------------------------------------------------------ CMD-ADP-010 / 014

    @Test
    void CMD_ADP_010_014_los_rechazos_de_negocio_se_traducen_por_dbcode_y_no_se_degradan_a_fallo_tecnico() {
        assertRechazo("DBCODE=SEC_002|no titular", ForbiddenException.class);
        assertRechazo("DBCODE=EST_004|no matriculado", ForbiddenException.class);
        assertRechazo("DBCODE=RC_001|estado invalido", ValidationException.class);
        assertRechazo("DBCODE=GEN_002|ejecutor nulo", ValidationException.class);
        assertRechazo("DBCODE=SES_001|sesion inexistente", ResourceNotFoundException.class);
        assertTrue(eventosDeError().isEmpty(), "Un rechazo de negocio no es un fallo tecnico de SQL.");
    }

    private void assertRechazo(final String mensajeTecnico, final Class<? extends RuntimeException> esperada) {
        stubRechazo(mensajeTecnico);

        assertThrows(esperada, () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));
    }

    @Test
    void CMD_ADP_010_dbcode_desconocido_es_error_db_no_clasificado() {
        stubRechazo("DBCODE=XYZ_999|x");

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        assertEquals(DatabaseErrorCode.ERR_DB_UNCLASSIFIED.code(), exception.getCode());
    }

    // ------------------------------------------------------------------ CMD-ADP-011

    @Test
    void CMD_ADP_011_el_fallo_tecnico_se_loguea_solo_con_operacion_y_correlacion_sin_payload_ni_causa() {
        final PersistenceException causa = new PersistenceException(
                "could not execute " + EST_1 + " " + EJECUTOR + " [{\"idEstudiante\":\"" + EST_1 + "\"}]");
        when(query.execute()).thenThrow(causa);

        assertThrows(DatabaseOperationException.class, () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        final List<ILoggingEvent> errores = eventosDeError();
        assertEquals(1, errores.size());
        final ILoggingEvent evento = errores.getFirst();
        assertEquals("SQL operation failed. operation={}, correlationId={}", evento.getMessage());
        assertEquals("SQL operation failed. operation=registrarAsistenciasSesion, correlationId=" + CORRELATION,
                evento.getFormattedMessage());
        assertNull(evento.getThrowableProxy(), "La causa (puede llevar SQL/valores) no se adjunta al log.");
        for (final ILoggingEvent registrado : logAppender.list) {
            final String texto = registrado.getFormattedMessage();
            assertFalse(texto.contains(EST_1.toString()) || texto.contains(EJECUTOR.toString())
                    || texto.contains("idEstudiante") || texto.contains(SESION.toString()), texto);
        }
    }

    // ------------------------------------------------------------------ CMD-ADP-012 / 015

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"PersistenceException", "IllegalStateException", "IllegalArgumentException"})
    void CMD_ADP_012_fallo_tecnico_de_execute_se_traduce_a_excepcion_generica_con_causa_y_cierra_el_em(final String tipo) {
        final RuntimeException causa = switch (tipo) {
            case "PersistenceException" -> new PersistenceException("boom");
            case "IllegalStateException" -> new IllegalStateException("boom");
            default -> new IllegalArgumentException("boom");
        };
        when(query.execute()).thenThrow(causa);

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        assertEquals(DatabaseErrorCode.DATABASE_OPERATION_ERROR.code(), exception.getCode());
        assertEquals(GENERIC_MESSAGE, exception.getMessage());
        assertSame(causa, exception.getCause());
        verify(entityManager).close();
    }

    @Test
    void CMD_ADP_012_fallo_al_crear_el_entity_manager_se_traduce_igual() {
        final IllegalStateException causa = new IllegalStateException("emf cerrado");
        when(entityManagerFactory.createEntityManager()).thenThrow(causa);

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        assertEquals(GENERIC_MESSAGE, exception.getMessage());
        assertSame(causa, exception.getCause());
    }

    @Test
    void CMD_ADP_012_fallo_de_binding_se_traduce_igual_y_cierra_el_em() {
        final IllegalArgumentException causa = new IllegalArgumentException("binding");
        when(query.setParameter(eq(2), any())).thenThrow(causa);

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        assertSame(causa, exception.getCause());
        verify(query, never()).execute();
        verify(entityManager).close();
    }

    @Test
    void CMD_ADP_012_fallo_al_leer_el_result_set_se_traduce_igual() {
        when(query.execute()).thenReturn(true);
        when(query.getResultList()).thenThrow(new PersistenceException("lectura"));

        assertThrows(DatabaseOperationException.class, () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        verify(entityManager).close();
    }

    @Test
    void CMD_ADP_015_el_entity_manager_se_crea_uno_por_invocacion_y_se_cierra_en_exito_y_en_rechazo() {
        persistence.registrarAsistenciasSesion(loteAnSjcEx());
        verify(entityManager, times(1)).close();

        assertRechazo("DBCODE=SEC_002|x", ForbiddenException.class);
        verify(entityManagerFactory, times(2)).createEntityManager();
        verify(entityManager, times(2)).close();
    }

    // ------------------------------------------------------------------ CMD-ADP-013

    @Test
    void CMD_ADP_013_no_abre_transaccion_jpa_ni_se_une_a_una_en_exito_ni_en_fallo() {
        persistence.registrarAsistenciasSesion(loteAnSjcEx());
        when(query.execute()).thenThrow(new PersistenceException("boom"));
        assertThrows(DatabaseOperationException.class, () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        verify(entityManager, never()).getTransaction();
        verify(entityManager, never()).joinTransaction();
        verify(entityManager, never()).unwrap(any());
    }

    // ------------------------------------------------------------------ CMD-ADP-014 (update counts)

    @Test
    void CMD_ADP_014_tolera_update_counts_previos_al_result_set_canonico() {
        when(query.execute()).thenReturn(false);
        when(query.getUpdateCount()).thenReturn(2, -1);
        when(query.hasMoreResults()).thenReturn(true, false);
        when(query.getResultList()).thenReturn(List.of((Object) fila(CORRELATION.toString(), Boolean.TRUE)));

        persistence.registrarAsistenciasSesion(loteAnSjcEx());

        verify(query).getResultList();
        verify(entityManager).close();
    }

    @Test
    void CMD_ADP_014_tolera_varios_update_counts_consecutivos_antes_del_result_set() {
        when(query.execute()).thenReturn(false);
        when(query.getUpdateCount()).thenReturn(1, 3, -1);
        when(query.hasMoreResults()).thenReturn(false, true, false);
        when(query.getResultList()).thenReturn(List.of((Object) fila(CORRELATION.toString(), Boolean.TRUE)));

        persistence.registrarAsistenciasSesion(loteAnSjcEx());

        verify(query, times(1)).getResultList();
    }

    @Test
    void CMD_ADP_014_el_recorrido_de_resultados_esta_acotado_ante_un_driver_que_nunca_termina() {
        when(query.execute()).thenReturn(true);
        when(query.getResultList()).thenReturn(List.of());
        when(query.hasMoreResults()).thenReturn(true);

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.registrarAsistenciasSesion(loteAnSjcEx()));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
        verify(query, atLeast(2)).hasMoreResults();
    }
}
