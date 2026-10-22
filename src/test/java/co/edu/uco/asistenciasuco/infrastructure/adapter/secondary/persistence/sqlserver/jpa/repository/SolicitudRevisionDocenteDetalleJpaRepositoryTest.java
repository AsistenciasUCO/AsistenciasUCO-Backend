package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DetalleSolicitudRevisionDocenteProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpDetalleSolicitudRevisionDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU036 (IT-03 a nivel de adapter, sin SQL Server): el adapter JPA envia el correlationId del contexto y los parametros
 * del SP de detalle con los nombres del contrato, mapea la fila y solo afirma UTC cuando hay procedencia temporal.
 */
class SolicitudRevisionDocenteDetalleJpaRepositoryTest {

    private static final UUID SOLICITUD = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class, RETURNS_SELF);
    private final SolicitudRevisionDocenteJpaRepository repository = new SolicitudRevisionDocenteJpaRepository(entityManager);

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void envia_el_exec_del_sp_de_detalle_con_los_parametros_del_contrato_y_la_correlacion_del_contexto() {
        final UUID correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
        when(entityManager.createNativeQuery(anyString(), eq(SpDetalleSolicitudRevisionDocenteEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        final Optional<DetalleSolicitudRevisionDocenteProjection> resultado =
                repository.consultarDetalleSolicitudDocente(SOLICITUD, USUARIO);

        assertTrue(resultado.isEmpty());
        final ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sql.capture(), eq(SpDetalleSolicitudRevisionDocenteEntity.class));
        assertTrue(sql.getValue().contains("EXEC dbo.usp_consultar_detalle_solicitud_revision_docente"));
        verify(query).setParameter("idSolicitud", SOLICITUD);
        verify(query).setParameter("idUsuarioEjecutor", USUARIO);
        verify(query).setParameter("idCorrelacion", correlacion);
    }

    @Test
    void sin_correlacion_en_el_contexto_falla_sin_generar_una_ni_consultar() {
        CorrelationIdContext.clear();

        assertThrows(CrosscuttingException.class, () -> repository.consultarDetalleSolicitudDocente(SOLICITUD, USUARIO));

        org.mockito.Mockito.verifyNoInteractions(entityManager);
    }

    @Test
    void mapea_la_fila_y_afirma_utc_solo_cuando_hay_procedencia_temporal() {
        CorrelationIdContext.set(UUID.randomUUID());
        final SpDetalleSolicitudRevisionDocenteEntity conProcedencia = fila("UTC_V2", UUID.randomUUID());
        final SpDetalleSolicitudRevisionDocenteEntity sinProcedencia = fila(null, null);
        when(entityManager.createNativeQuery(anyString(), eq(SpDetalleSolicitudRevisionDocenteEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(conProcedencia), List.of(sinProcedencia));

        final DetalleSolicitudRevisionDocenteProjection a = repository.consultarDetalleSolicitudDocente(SOLICITUD, USUARIO).orElseThrow();
        final DetalleSolicitudRevisionDocenteProjection b = repository.consultarDetalleSolicitudDocente(SOLICITUD, USUARIO).orElseThrow();

        assertEquals(SOLICITUD, a.idSolicitud());
        assertEquals("P", a.codigoEstado());
        assertEquals("pendiente", a.nombreEstado());
        assertEquals("SJC", a.codigoRazonCausa());
        assertEquals("sin justa causa", a.nombreRazonCausa());
        assertEquals(Boolean.FALSE, a.asistio());
        assertEquals(java.time.Instant.parse("2026-10-14T13:00:00.1234567Z"), a.fechaHoraInicioSesionUtc());
        assertEquals("UTC_V2", a.procedenciaTemporalSesion());
        assertEquals(java.time.Instant.parse("2026-10-15T10:00:00Z"), a.fechaRegistroSoporteUtc());
        assertNull(b.fechaHoraInicioSesionUtc());
        assertNull(b.procedenciaTemporalSesion());
        assertNull(b.idArchivoSoporte());
        assertNull(b.fechaRegistroSoporteUtc());
    }

    @Test
    void un_fallo_tecnico_de_jpa_se_traduce_a_database_operation_exception() {
        CorrelationIdContext.set(UUID.randomUUID());
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenThrow(new PersistenceException("boom"));

        assertThrows(DatabaseOperationException.class, () -> repository.consultarDetalleSolicitudDocente(SOLICITUD, USUARIO));
    }

    private static SpDetalleSolicitudRevisionDocenteEntity fila(final String procedencia, final UUID archivo) {
        final SpDetalleSolicitudRevisionDocenteEntity fila = mock(SpDetalleSolicitudRevisionDocenteEntity.class);
        when(fila.idSolicitudRevisionAsistencia()).thenReturn(SOLICITUD);
        when(fila.nombreSolicitud()).thenReturn("REV-1");
        when(fila.fechaSolicitud()).thenReturn(LocalDate.of(2026, 10, 15));
        when(fila.codigoEstado()).thenReturn("P");
        when(fila.nombreEstado()).thenReturn("pendiente");
        when(fila.justificacionSolicitud()).thenReturn("Cita");
        when(fila.justificacionRespuesta()).thenReturn("");
        when(fila.idEstudiante()).thenReturn(UUID.randomUUID());
        when(fila.nombreEstudiante()).thenReturn("Ana");
        when(fila.idSesion()).thenReturn(UUID.randomUUID());
        when(fila.numeroSesion()).thenReturn(4);
        when(fila.fechaHoraInicioSesion()).thenReturn(LocalDateTime.parse("2026-10-14T13:00:00.1234567"));
        when(fila.procedenciaTemporalSesion()).thenReturn(procedencia);
        when(fila.idGrupo()).thenReturn(UUID.randomUUID());
        when(fila.codigoGrupo()).thenReturn(1);
        when(fila.nombreGrupo()).thenReturn("Grupo 1");
        when(fila.idAsignatura()).thenReturn(UUID.randomUUID());
        when(fila.codigoAsignatura()).thenReturn("MAT101");
        when(fila.nombreAsignatura()).thenReturn("Calculo I");
        when(fila.asistio()).thenReturn(Boolean.FALSE);
        when(fila.codigoRazonCausa()).thenReturn("SJC");
        when(fila.nombreRazonCausa()).thenReturn("sin justa causa");
        when(fila.idArchivoSoporte()).thenReturn(archivo);
        when(fila.nombreSoporte()).thenReturn(archivo == null ? null : "incapacidad.pdf");
        when(fila.tipoContenidoSoporte()).thenReturn(archivo == null ? null : "application/pdf");
        when(fila.tamanioSoporteBytes()).thenReturn(archivo == null ? null : 2048L);
        when(fila.fechaRegistroSoporteUtc()).thenReturn(archivo == null ? null : LocalDateTime.parse("2026-10-15T10:00:00"));
        return fila;
    }
}
