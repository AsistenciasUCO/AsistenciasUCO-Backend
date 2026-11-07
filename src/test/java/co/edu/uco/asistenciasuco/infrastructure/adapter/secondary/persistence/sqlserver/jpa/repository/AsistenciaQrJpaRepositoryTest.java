package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaQrRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvTomaAsistenciaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.HydratedViewRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 B.2: adaptador JPA del QR de asistencia (solo el SHA-256 del token llega a la DB; interbloqueo reintentable). */
class AsistenciaQrJpaRepositoryTest {

    private static final UUID TOMA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID GRUPO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USUARIO = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID TOKEN_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-AbCde";

    private final EntityManager entityManager = mock(EntityManager.class);
    private final JpaProcedureExecutor executor = mock(JpaProcedureExecutor.class);
    private final Query query = mock(Query.class);
    private final AsistenciaQrJpaRepository repository = new AsistenciaQrJpaRepository(entityManager, executor);
    private UUID correlacion;

    @BeforeEach
    void preparar() {
        correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(entityManager.createNativeQuery(anyString(), eq(Tuple.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] filaRegistro(final boolean estado, final String tecnico) {
        return new Object[]{"%s", estado ? "¡Listo! Tu asistencia quedó registrada." : "Error", tecnico, estado,
                estado ? TOMA.toString() : null, estado ? SESION : null, estado ? GRUPO : null, estado ? "Arquitectura" : null,
                estado ? "Grupo 1" : null, estado ? 12 : null, estado ? (short) 2 : null, estado ? false : null,
                estado ? Timestamp.valueOf("2026-11-07 13:20:00") : null};
    }

    private Object[] conCorrelacion(final Object[] fila) {
        fila[0] = correlacion.toString();
        return fila;
    }

    @Test
    @SuppressWarnings("unchecked")
    void abrir_envia_solo_el_hash_del_token_y_lee_siete_columnas() {
        when(executor.executeWithExtras(eq("abrirQrToma"), eq(AsistenciaQrJpaRepository.SQL_ABRIR), anyMap(), eq(correlacion), eq(7)))
                .thenReturn(new ProcedureRow(null, Arrays.asList(TOKEN_ID, TOMA, SESION, GRUPO, Timestamp.valueOf("2026-11-07 13:10:00"),
                        LocalDateTime.of(2026, 11, 7, 15, 30), 60)));

        final AsistenciaQrRepositoryPort.QrEmitido emitido = repository.abrir(TOMA, TOKEN, "482913", USUARIO);

        assertEquals(new AsistenciaQrRepositoryPort.QrEmitido(TOKEN_ID, TOMA, SESION, GRUPO, Instant.parse("2026-11-07T13:10:00Z"),
                Instant.parse("2026-11-07T15:30:00Z"), 60), emitido);
        final ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.forClass(Map.class);
        verify(executor).executeWithExtras(anyString(), anyString(), parametros.capture(), any(), eq(7));
        assertArrayEquals(TokenHashHelper.sha256(TOKEN), (byte[]) parametros.getValue().get("tokenHash"));
        assertFalse(parametros.getValue().containsValue(TOKEN));
    }

    @Test
    void rotar_manual_y_automatico_leen_el_flag_rotado_y_aqr_005_no_es_error_del_job() {
        final List<Object> rotada = Arrays.asList(TOKEN_ID, TOMA, GRUPO, USUARIO, "2026-11-07 13:11:00", "2026-11-07 15:30:00", true);
        when(executor.executeWithExtras(eq("rotarTokenAsistenciaToma"), anyString(), anyMap(), any(), eq(7))).thenReturn(new ProcedureRow(null, rotada));
        assertEquals(USUARIO, repository.rotarManual(TOMA, TOKEN, "123456", USUARIO).idDocenteEmisor());

        when(executor.executeWithExtras(eq("rotarTokenAsistenciaAutomatico"), anyString(), anyMap(), any(), eq(7)))
                .thenReturn(new ProcedureRow(null, rotada));
        assertTrue(repository.rotarAutomatico(TOMA, TOKEN, "123456").isPresent());

        final List<Object> noRotada = Arrays.asList(TOKEN_ID, TOMA, GRUPO, USUARIO, "2026-11-07 13:11:00", "2026-11-07 15:30:00", 0);
        when(executor.executeWithExtras(eq("rotarTokenAsistenciaAutomatico"), anyString(), anyMap(), any(), eq(7)))
                .thenReturn(new ProcedureRow(null, noRotada));
        assertEquals(Optional.empty(), repository.rotarAutomatico(TOMA, TOKEN, "123456"));

        when(executor.executeWithExtras(eq("rotarTokenAsistenciaAutomatico"), anyString(), anyMap(), any(), eq(7)))
                .thenThrow(new ConflictException("AQR_005", "cerrada"));
        assertEquals(Optional.empty(), repository.rotarAutomatico(TOMA, TOKEN, "123456"));
    }

    @Test
    void rotar_automatico_propaga_otros_errores() {
        when(executor.executeWithExtras(eq("rotarTokenAsistenciaAutomatico"), anyString(), anyMap(), any(), eq(7)))
                .thenThrow(new ConflictException("AQR_006", "pin"));
        assertThrows(ConflictException.class, () -> repository.rotarAutomatico(TOMA, TOKEN, "123456"));
    }

    @Test
    void por_rotar_y_cerrar() {
        final Tuple tupla = TomaAsistenciaJpaRepositoryTest.tupla(Map.of("idToma", TOMA.toString(), "idGrupo", GRUPO.toString(),
                "idDocenteEmisor", USUARIO.toString()));
        when(query.getResultList()).thenReturn(List.of(tupla));
        assertEquals(List.of(new AsistenciaQrRepositoryPort.TomaPorRotar(TOMA, GRUPO, USUARIO)), repository.consultarTomasPorRotar());

        when(executor.executeWithExtras(eq("cerrarQrToma"), eq(AsistenciaQrJpaRepository.SQL_CERRAR), anyMap(), eq(correlacion), eq(3)))
                .thenReturn(new ProcedureRow(new CanonicalProcedureResult(correlacion, "Se dejó de proyectar el código QR.", "t", true),
                        Arrays.asList(TOMA, SESION, GRUPO)));
        assertEquals("Se dejó de proyectar el código QR.", repository.cerrar(TOMA, USUARIO));
    }

    @Test
    void registrar_por_token_lee_nueve_columnas_y_el_mensaje_del_sp() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(conCorrelacion(filaRegistro(true, "ok"))));

        final AsistenciaQrRepositoryPort.AsistenciaPorToken registro = repository.registrarPorToken(TOKEN, null, USUARIO);

        assertEquals(new AsistenciaQrRepositoryPort.AsistenciaPorToken(TOMA, SESION, GRUPO, "Arquitectura", "Grupo 1", 12, 2, false,
                Instant.parse("2026-11-07T13:20:00Z"), "¡Listo! Tu asistencia quedó registrada."), registro);
        verify(entityManager).createNativeQuery(AsistenciaQrJpaRepository.SQL_REGISTRAR_TOKEN);
        verify(query, never()).setParameter(eq("pin"), any());
    }

    @Test
    void registrar_por_pin_omite_el_hash_y_un_rechazo_del_sp_se_traduce() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(conCorrelacion(filaRegistro(false, "DBCODE=AQR_001|sin coincidencia"))));

        assertEquals("AQR_001", assertThrows(ResourceNotFoundException.class,
                () -> repository.registrarPorToken(null, "482913", USUARIO)).getCode());
        verify(entityManager).createNativeQuery(AsistenciaQrJpaRepository.SQL_REGISTRAR_PIN);
        verify(query, never()).setParameter(eq("tokenHash"), any());
    }

    @Test
    void un_interbloqueo_en_la_tupla_o_en_el_driver_es_reintentable_y_otro_fallo_es_tecnico() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(conCorrelacion(filaRegistro(false,
                "[CORRELATION_ID=X][ORIGEN=BASE_DATOS][TYPE=ERROR][NUMBER=1205][MESSAGE=Transaction was deadlocked]"))));
        assertThrows(AsistenciaQrRepositoryPort.ConflictoConcurrenciaException.class,
                () -> repository.registrarPorToken(TOKEN, null, USUARIO));

        when(query.getResultList()).thenThrow(new PersistenceException("x", new RuntimeException("Transaction (Process ID 61) was deadlocked")));
        assertThrows(AsistenciaQrRepositoryPort.ConflictoConcurrenciaException.class,
                () -> repository.registrarPorToken(TOKEN, null, USUARIO));
    }

    @Test
    void un_fallo_tecnico_del_driver_es_database_operation_exception() {
        when(query.getResultList()).thenThrow(new PersistenceException("timeout"));
        assertThrows(DatabaseOperationException.class, () -> repository.registrarPorToken(TOKEN, null, USUARIO));
        assertFalse(AsistenciaQrJpaRepository.esInterbloqueo(new IllegalStateException((String) null)));
        assertTrue(AsistenciaQrJpaRepository.esInterbloqueo(new LockAcquisitionFake()));
        assertFalse(AsistenciaQrJpaRepository.contieneInterbloqueo(null));
        assertThrows(NullPointerException.class, () -> new AsistenciaQrJpaRepository(null, executor));
        assertThrows(NullPointerException.class, () -> new AsistenciaQrJpaRepository(entityManager, null));
    }

    @Test
    void la_entidad_de_la_vista_se_mapea_con_horas_de_2_decimales_y_fechas_utc() {
        final Map<String, Object> columnas = new HashMap<>();
        columnas.put("idToma", TOMA);
        columnas.put("idSesion", SESION);
        columnas.put("idGrupo", GRUPO);
        columnas.put("numero", (short) 2);
        columnas.put("codigoModo", "QR");
        columnas.put("codigoEstado", "ABIERTA");
        columnas.put("estaAbierta", true);
        columnas.put("esImplicita", false);
        columnas.put("fechaAperturaUtc", LocalDateTime.of(2026, 11, 7, 13, 5));
        columnas.put("horasToma", new BigDecimal("0.666667"));
        columnas.put("totalRegistros", 3);
        columnas.put("totalPresentes", 3);
        columnas.put("totalAusentes", 0);
        columnas.put("totalExcusas", 0);
        columnas.put("tieneQrActivo", true);
        final TomaAsistenciaResumen resumen = TomaAsistenciaJpaRepository.aResumen(HydratedViewRow.of(UvTomaAsistenciaEntity.class, columnas));

        assertEquals(new TomaAsistenciaResumen(TOMA, SESION, GRUPO, 2, "QR", "ABIERTA", true, false, Instant.parse("2026-11-07T13:05:00Z"),
                null, new BigDecimal("0.67"), 3, 3, 0, 0, true), resumen);
        columnas.put("fechaCierreUtc", LocalDateTime.of(2026, 11, 7, 14, 0));
        assertEquals(Instant.parse("2026-11-07T14:00:00Z"),
                TomaAsistenciaJpaRepository.aResumen(HydratedViewRow.of(UvTomaAsistenciaEntity.class, columnas)).fechaCierreUtc());
    }

    /** Simula la excepcion de Hibernate para un 1205 traducido por el dialecto. */
    private static final class LockAcquisitionFake extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
