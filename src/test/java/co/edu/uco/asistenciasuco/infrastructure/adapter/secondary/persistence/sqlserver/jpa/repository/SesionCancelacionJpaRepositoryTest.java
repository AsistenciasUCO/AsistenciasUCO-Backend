package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.DestinatarioSesionCancelada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.SesionCancelada;
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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
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

/** CULM-001 B.1 (HU047): adaptador JPA de {@code usp_cancelar_sesion} y de los destinatarios del aviso. */
class SesionCancelacionJpaRepositoryTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DOCENTE = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final JpaProcedureExecutor executor = mock(JpaProcedureExecutor.class);
    private final Query query = mock(Query.class);
    private final SesionCancelacionJpaRepository repository = new SesionCancelacionJpaRepository(entityManager, executor);
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
    void cancelar_envia_los_parametros_del_sp_y_lee_sus_cuatro_columnas() {
        final LocalDateTime cancelada = LocalDateTime.of(2026, 11, 7, 14, 0);
        when(executor.executeWithExtras(eq("cancelarSesion"), anyString(), anyMap(), eq(correlacion), eq(4)))
                .thenReturn(new ProcedureRow(null, List.of(SESION.toString(), GRUPO, cancelada, 38)));

        assertEquals(new SesionCancelada(SESION, GRUPO, cancelada, 38), repository.cancelar(SESION, "Motivo valido", DOCENTE));

        final ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.forClass(Map.class);
        verify(executor).executeWithExtras(eq("cancelarSesion"), eq(SesionCancelacionJpaRepository.SQL_CANCELAR),
                parametros.capture(), eq(correlacion), eq(4));
        assertEquals(Map.of("idSesion", SESION, "motivo", "Motivo valido", "idCorrelacion", correlacion,
                "idUsuarioEjecutor", DOCENTE), parametros.getValue());
    }

    @Test
    void cancelar_tolera_conteo_nulo_y_fechas_como_timestamp_o_texto() {
        when(executor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(4)))
                .thenReturn(new ProcedureRow(null, Arrays.asList(SESION, GRUPO, Timestamp.valueOf("2026-11-07 14:00:00"), null)),
                        new ProcedureRow(null, Arrays.asList(SESION, GRUPO, "2026-11-07 14:00:00", 1)),
                        new ProcedureRow(null, Arrays.asList(SESION, GRUPO, null, 1)));

        assertEquals(0, repository.cancelar(SESION, "m", DOCENTE).totalEstudiantesActivos());
        assertEquals(LocalDateTime.of(2026, 11, 7, 14, 0), repository.cancelar(SESION, "m", DOCENTE).fechaCancelacionUtc());
        assertNull(repository.cancelar(SESION, "m", DOCENTE).fechaCancelacionUtc());
    }

    @Test
    void destinatarios_mapea_las_diez_columnas_del_sp() {
        final UUID estudiante = UUID.randomUUID();
        final UUID usuario = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 13, 0);
        when(query.getResultList()).thenReturn(List.<Object>of(new Object[]{estudiante, usuario.toString(), "Ana Perez",
                "ana@uco.edu.co", "Calculo", "G01 de Calculo", "Maria Docente", inicio, inicio.plusHours(2), "Motivo"},
                new Object[]{estudiante, usuario, null, null, null, null, null, null, null, null}));

        final List<DestinatarioSesionCancelada> destinatarios = repository.consultarDestinatarios(SESION, DOCENTE);

        assertEquals(new DestinatarioSesionCancelada(estudiante, usuario, "Ana Perez", "ana@uco.edu.co", "Calculo",
                "G01 de Calculo", "Maria Docente", inicio, inicio.plusHours(2), "Motivo"), destinatarios.getFirst());
        assertNull(destinatarios.get(1).nombreCompleto());
        verify(entityManager).createNativeQuery(SesionCancelacionJpaRepository.SQL_DESTINATARIOS);
        verify(query).setParameter("idSesion", SESION);
        verify(query).setParameter("idUsuarioEjecutor", DOCENTE);
        verify(query).setParameter("idCorrelacion", correlacion);
    }

    @Test
    void destinatarios_traduce_el_dbcode_de_la_consulta() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_001|sin perfil"));
        assertThrows(ForbiddenException.class, () -> repository.consultarDestinatarios(SESION, DOCENTE));
    }

    @Test
    void el_constructor_exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new SesionCancelacionJpaRepository(null, executor));
        assertThrows(NullPointerException.class, () -> new SesionCancelacionJpaRepository(entityManager, null));
    }
}
