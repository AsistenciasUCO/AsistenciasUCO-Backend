package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadCommandPort.CrearFacultadCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FacultadProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvFacultadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FacultadJpaRepositoryTest {

    private static final UUID CORRELATION_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID FACULTY_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    @Test
    void crearFacultad_delega_el_comando_con_correlacion_y_ejecutor() {
        final EntityManager entityManager = mock(EntityManager.class);
        final JpaProcedureExecutor procedureExecutor = mock(JpaProcedureExecutor.class);
        final FacultadJpaRepository repository = new FacultadJpaRepository(entityManager, procedureExecutor);
        final UUID actorId = UUID.fromString("99999999-8888-7777-6666-555555555555");
        final UUID institutionId = UUID.fromString("12121212-3434-5656-7878-909090909090");
        final UUID deanId = UUID.fromString("abababab-cdcd-efef-0101-232323232323");
        CorrelationIdContext.set(CORRELATION_ID);

        repository.crearFacultad(new CrearFacultadCommand(
                FACULTY_ID, "Ingenieria", institutionId, deanId, actorId));

        verify(procedureExecutor).execute(
                eq(FacultadJpaRepository.OP_CREAR_FACULTAD),
                eq(FacultadJpaRepository.SQL_CREAR_FACULTAD),
                eq(Map.of(
                        "id", FACULTY_ID,
                        "nombre", "Ingenieria",
                        "idInstitucion", institutionId,
                        "idDecano", deanId,
                        "idCorrelacion", CORRELATION_ID,
                        "idUsuarioEjecutor", actorId)),
                eq(CORRELATION_ID));
    }

    @Test
    void consultarFacultades_mapea_las_filas_de_la_vista() {
        final EntityManager entityManager = mock(EntityManager.class);
        final TypedQuery<UvFacultadEntity> query = mock(TypedQuery.class);
        final UvFacultadEntity row = mock(UvFacultadEntity.class);
        final UUID institutionId = UUID.randomUUID();
        final UUID deanId = UUID.randomUUID();
        when(entityManager.createQuery(anyString(), eq(UvFacultadEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(row));
        when(row.id()).thenReturn(FACULTY_ID);
        when(row.nombreFacultad()).thenReturn("Ingenieria");
        when(row.idInstitucion()).thenReturn(institutionId);
        when(row.nombreInstitucion()).thenReturn("UCO");
        when(row.idDecano()).thenReturn(deanId);
        when(row.nombreCompletoDecano()).thenReturn("Ada Lovelace");
        when(row.estaActivaFacultad()).thenReturn(true);
        when(row.estaActivaTextoFacultad()).thenReturn("ACTIVA");

        final FacultadProjection result = new FacultadJpaRepository(entityManager).consultarFacultades().get(0);

        assertEquals(FACULTY_ID, result.id());
        assertEquals("Ingenieria", result.nombreFacultad());
        assertTrue(result.estaActivaFacultad());
    }

    @Test
    void consultarFacultadPorId_aplica_el_parametro_y_devuelve_vacio_si_no_hay_fila() {
        final EntityManager entityManager = mock(EntityManager.class);
        final TypedQuery<UvFacultadEntity> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(UvFacultadEntity.class))).thenReturn(query);
        when(query.setParameter("idFacultad", FACULTY_ID)).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        final var result = new FacultadJpaRepository(entityManager).consultarFacultadPorId(FACULTY_ID);

        assertTrue(result.isEmpty());
        verify(query).setParameter("idFacultad", FACULTY_ID);
        verify(query).setMaxResults(1);
    }

}
