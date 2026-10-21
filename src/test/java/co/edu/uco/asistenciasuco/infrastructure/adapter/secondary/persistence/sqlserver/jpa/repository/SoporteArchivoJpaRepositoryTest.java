package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort.RegistrarSoporteBorrador;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpSoporteArchivoAutorizadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpSolicitudRevisionDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LB-004D: patron JPA (createNativeQuery + EXEC con binding nombrado) de la metadata del soporte y de los reclamos del
 * docente, y semantica UTC de los DATETIME2(7) (sin zona de la JVM). La paridad real contra SQL Server la certifican
 * los IT de soporte.
 */
class SoporteArchivoJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID FILE = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID USER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private SoporteArchivoJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new SoporteArchivoJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] fila(final boolean estado, final String tecnico) {
        return new Object[]{CORRELATION, "usuario", tecnico, estado};
    }

    @Test
    void registrar_borrador_usa_exec_con_binding_nombrado_y_el_propietario_es_el_ejecutor() {
        when(query.getResultList()).thenReturn(Collections.singletonList(fila(true, "")));

        repository.registrarBorrador(new RegistrarSoporteBorrador(FILE, USER, "evidencia.pdf", "application/pdf", 1234L, "a".repeat(64)));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_registrar_archivo_soporte"));
        verify(query).setParameter("idArchivoSoporte", FILE);
        verify(query).setParameter("nombreOriginal", "evidencia.pdf");
        verify(query).setParameter("tipoContenido", "application/pdf");
        verify(query).setParameter("tamanioBytes", 1234L);
        verify(query).setParameter("checksumSha256", "a".repeat(64));
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", USER);
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void rechazo_de_negocio_de_la_db_es_rechazo_definitivo() {
        when(query.getResultList()).thenReturn(Collections.singletonList(fila(false, "DBCODE=ARC_002|conflicto")));

        assertThrows(SoporteArchivoMetadataPort.SoporteMetadataRechazadaException.class, () -> repository.registrarBorrador(
                new RegistrarSoporteBorrador(FILE, USER, "e.pdf", "application/pdf", 1L, "a".repeat(64))));
    }

    @Test
    void sys_001_de_la_db_ocurre_tras_su_rollback_y_es_rechazo_definitivo() {
        when(query.getResultList()).thenReturn(Collections.singletonList(fila(false, "DBCODE=SYS_001|error")));

        assertThrows(SoporteArchivoMetadataPort.SoporteMetadataRechazadaException.class, () -> repository.registrarBorrador(
                new RegistrarSoporteBorrador(FILE, USER, "e.pdf", "application/pdf", 1L, "a".repeat(64))));
    }

    @Test
    void fallo_de_transporte_es_resultado_desconocido_nunca_rechazo() {
        when(query.getResultList()).thenThrow(new PersistenceException("sin conexion"));

        assertThrows(SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException.class, () -> repository.registrarBorrador(
                new RegistrarSoporteBorrador(FILE, USER, "e.pdf", "application/pdf", 1L, "a".repeat(64))));
    }

    @Test
    void consultar_autorizado_sin_filas_devuelve_vacio() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        assertTrue(repository.consultarAutorizado(FILE, USER).isEmpty());

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_archivo_soporte_autorizado"), any(Class.class));
        verify(query).setParameter("idArchivoSoporte", FILE);
        verify(query).setParameter("idUsuarioEjecutor", USER);
        verify(query).setParameter("idCorrelacion", CORRELATION);
    }

    @Test
    void consultar_autorizado_convierte_datetime2_7_a_instant_utc_sin_perder_decimales() throws Exception {
        final SpSoporteArchivoAutorizadoEntity fila = entidad(SpSoporteArchivoAutorizadoEntity.class);
        poner(fila, "idArchivoSoporte", FILE);
        poner(fila, "estado", "ATTACHED");
        poner(fila, "nombreOriginal", "evidencia.pdf");
        poner(fila, "tipoContenido", "application/pdf");
        poner(fila, "tamanioBytes", 1234L);
        poner(fila, "checksumSha256", "a".repeat(64));
        poner(fila, "fechaRegistroUtc", LocalDateTime.parse("2026-10-19T15:30:45.1234567"));
        poner(fila, "fechaAdjuncionUtc", LocalDateTime.parse("2026-03-29T01:30:00.0000001"));
        poner(fila, "idSolicitudRevisionAsistencia", UUID.randomUUID());
        poner(fila, "relacionAcceso", "DOCENTE_TITULAR");
        when(query.getResultList()).thenReturn(List.of(fila));

        final Optional<SoporteArchivoMetadataPort.SoporteArchivoAutorizado> resultado = repository.consultarAutorizado(FILE, USER);

        assertTrue(resultado.isPresent());
        assertEquals(SoporteArchivoMetadataPort.RelacionAcceso.DOCENTE_TITULAR, resultado.get().relacionAcceso());
        assertEquals(SoporteArchivoMetadataPort.EstadoSoporte.ATTACHED, resultado.get().estado());
        assertEquals(Instant.parse("2026-10-19T15:30:45.1234567Z"), resultado.get().fechaRegistroUtc());
        // 01:30 UTC del cambio de hora de Londres: no existe como hora local en la JVM, pero el instante UTC es exacto.
        assertEquals(Instant.parse("2026-03-29T01:30:00.0000001Z"), resultado.get().fechaAdjuncionUtc());
    }

    @Test
    void fallo_tecnico_al_consultar_es_no_disponible() {
        when(query.getResultList()).thenThrow(new PersistenceException("sin conexion"));

        assertThrows(SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException.class,
                () -> repository.consultarAutorizado(FILE, USER));
    }

    @Test
    void reclamos_docente_solo_afirma_utc_cuando_la_sesion_tiene_procedencia() throws Exception {
        final SolicitudRevisionDocenteJpaRepository docente = new SolicitudRevisionDocenteJpaRepository(entityManager);
        final SpSolicitudRevisionDocenteEntity verificada = filaDocente("UTC_V2", LocalDateTime.parse("2026-10-19T15:30:45.1234567"));
        final SpSolicitudRevisionDocenteEntity historica = filaDocente(null, LocalDateTime.parse("2026-10-19T15:30:45.1234567"));
        when(query.getResultList()).thenReturn(List.of(verificada, historica));

        final List<SolicitudRevisionDocenteProjection> filas = docente.consultarSolicitudesDocente(USER, "P", 2, 5);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_solicitudes_revision_docente"), any(Class.class));
        verify(query).setParameter("idUsuarioEjecutor", USER);
        verify(query).setParameter("codigoEstado", "P");
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
        assertEquals(Instant.parse("2026-10-19T15:30:45.1234567Z"), filas.get(0).fechaHoraInicioSesionUtc());
        assertEquals("UTC_V2", filas.get(0).procedenciaTemporalSesion());
        assertNull(filas.get(1).fechaHoraInicioSesionUtc());
        assertNull(filas.get(1).procedenciaTemporalSesion());
        assertEquals(7, filas.get(0).totalRegistros());
        assertEquals(LocalDate.of(2026, 10, 19), filas.get(0).fechaSolicitud());
    }

    private static SpSolicitudRevisionDocenteEntity filaDocente(final String procedencia, final LocalDateTime inicio) throws Exception {
        final SpSolicitudRevisionDocenteEntity fila = entidad(SpSolicitudRevisionDocenteEntity.class);
        poner(fila, "idSolicitudRevisionAsistencia", UUID.randomUUID());
        poner(fila, "fechaSolicitud", LocalDate.of(2026, 10, 19));
        poner(fila, "codigoEstado", "P");
        poner(fila, "fechaHoraInicioSesion", inicio);
        poner(fila, "procedenciaTemporalSesion", procedencia);
        poner(fila, "totalRegistros", 7);
        return fila;
    }

    private static <T> T entidad(final Class<T> tipo) throws Exception {
        final Constructor<T> constructor = tipo.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static void poner(final Object objetivo, final String campo, final Object valor) throws Exception {
        final Field field = objetivo.getClass().getDeclaredField(campo);
        field.setAccessible(true);
        field.set(objetivo, valor);
    }
}
