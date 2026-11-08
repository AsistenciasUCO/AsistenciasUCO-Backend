package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort.PaginaCambios;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort.PaginaParametros;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort.ParametroActualizado;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 D.3 (CONTRACT v2 §4.3): adaptador JPA de los tres SP de parametros; ida y vuelta de la fecha con milisegundos. */
class ParametroAdminJpaRepositoryTest {

    private static final UUID ADMIN = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID PARAMETRO = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final JpaProcedureExecutor executor = mock(JpaProcedureExecutor.class);
    private final Query query = mock(Query.class);
    private final ParametroAdminJpaRepository repository = new ParametroAdminJpaRepository(entityManager, executor);
    private UUID correlacion;

    @BeforeEach
    void preparar() {
        correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void actualizar_envia_la_fecha_esperada_con_milisegundos_y_devuelve_la_nueva_version_sin_perdida() {
        final LocalDateTime esperada = LocalDateTime.of(2026, 11, 7, 15, 4, 5, 7_000_000);
        when(executor.executeWithExtras(eq("actualizarParametro"), anyString(), anyMap(), eq(correlacion), eq(6)))
                .thenReturn(new ProcedureRow(null, List.of(PARAMETRO.toString(), "ASISTENCIA", "TOLERANCIA_MINUTOS", "15", "20",
                        Timestamp.valueOf(LocalDateTime.of(2026, 11, 7, 16, 0, 0, 3_000_000)))));

        final ParametroActualizado resultado = repository.actualizar(PARAMETRO, "20", esperada, "Ajuste de prueba", ADMIN);

        assertEquals(new ParametroActualizado(PARAMETRO, "ASISTENCIA", "TOLERANCIA_MINUTOS", "15", "20",
                LocalDateTime.of(2026, 11, 7, 16, 0, 0, 3_000_000)), resultado);
        final ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.forClass(Map.class);
        verify(executor).executeWithExtras(eq("actualizarParametro"), eq(ParametroAdminJpaRepository.SQL_ACTUALIZAR),
                parametros.capture(), eq(correlacion), eq(6));
        assertEquals(Map.of("idParametro", PARAMETRO, "valor", "20", "fechaModificacionEsperada", esperada, "motivo", "Ajuste de prueba",
                "idCorrelacion", correlacion, "idUsuarioEjecutor", ADMIN), parametros.getValue());
        assertEquals(7_000_000, ((LocalDateTime) parametros.getValue().get("fechaModificacionEsperada")).getNano());
    }

    @Test
    void consultar_mapea_las_veinte_columnas_y_el_total() {
        final Object[] fila = {PARAMETRO.toString(), "ASISTENCIA", "TOLERANCIA_MINUTOS", "15", "15", "INT", true, new BigDecimal("0.0000"),
                new BigDecimal("60.0000"), "true,false", "Tolerancia", "Minutos.", "minutos", "Asistencia", (short) 10,
                Timestamp.valueOf(LocalDateTime.of(2026, 11, 7, 15, 4, 5, 7_000_000)), null, null, false, 41};
        final Object[] sinRango = {PARAMETRO.toString(), "RED", "X", "v", "v", "STRING", 0, null, null, null, "X", "", null, "RED", (short) 0,
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)), Timestamp.valueOf(LocalDateTime.of(2026, 2, 1, 0, 0)), "Ana", true, 41L};
        when(query.getResultList()).thenReturn(List.of(fila, sinRango));

        final PaginaParametros pagina = repository.consultar(ADMIN, 2, 20, "ASISTENCIA", null, true);

        assertEquals(41, pagina.totalRegistros());
        final var primero = pagina.elementos().getFirst();
        assertEquals(List.of("true", "false"), primero.valoresPermitidos());
        assertEquals(new BigDecimal("60.0000"), primero.valorMaximo());
        assertEquals(LocalDateTime.of(2026, 11, 7, 15, 4, 5, 7_000_000), primero.fechaModificacion());
        assertNull(primero.fechaUltimoCambioUtc());
        assertEquals(List.of(), pagina.elementos().get(1).valoresPermitidos());
        assertEquals("Ana", pagina.elementos().get(1).nombreUltimoActor());
        verify(query).setParameter("grupo", "ASISTENCIA");
        verify(query).setParameter("texto", null);
        verify(query).setParameter("soloEditables", true);
    }

    @Test
    void consultar_sin_filas_devuelve_total_cero() {
        when(query.getResultList()).thenReturn(List.of());

        assertEquals(new PaginaParametros(List.of(), 0), repository.consultar(ADMIN, 1, 20, null, null, false));
    }

    @Test
    void consultar_cambios_mapea_el_historial() {
        final UUID correlacionCambio = UUID.randomUUID();
        final Object[] fila = {"cccccccc-0000-0000-0000-000000000001", PARAMETRO.toString(), "ASISTENCIA", "TOLERANCIA_MINUTOS", "15", "20",
                "Ajuste", ADMIN.toString(), "Ana", correlacionCambio.toString(), Timestamp.valueOf(LocalDateTime.of(2026, 11, 7, 16, 0)), 1};
        when(query.getResultList()).thenReturn(List.<Object>of((Object) fila));

        final PaginaCambios pagina = repository.consultarCambios(ADMIN, PARAMETRO, 1, 10);

        assertEquals(1, pagina.totalRegistros());
        assertEquals("Ana", pagina.elementos().getFirst().nombreEjecutor());
        assertEquals(correlacionCambio, pagina.elementos().getFirst().idTraza());
        assertEquals(LocalDateTime.of(2026, 11, 7, 16, 0), pagina.elementos().getFirst().fechaCambioUtc());
    }

    @Test
    void un_throw_de_la_db_en_la_consulta_se_traduce_a_su_excepcion_de_negocio() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_001|Perfil insuficiente"));

        assertThrows(ForbiddenException.class, () -> repository.consultar(ADMIN, 1, 20, null, null, true));
    }
}
