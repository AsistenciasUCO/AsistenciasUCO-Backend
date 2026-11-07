package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CULM-001 A.2-1 (A2-B-01): el directorio de estudiantes ejecuta {@code usp_consultar_directorio_estudiantes_paginado} con
 * parametros nombrados (incluido el ejecutor), mapea por posicion las 12 columnas del listado, convierte la pagina 0 en
 * {@code @numeroPagina = 1}, calcula {@code totalPages} y agrupa los contextos del detalle. Sustituye al contrato HQL
 * anterior (cambio formal: el ambito pasa al SP).
 */
class EstudianteJpaQueryContractTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ESTUDIANTE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TIPO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final EstudianteJpaRepository repository = new EstudianteJpaRepository(entityManager);

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static ConsultarEstudiantesRepositoryDTO dto(final int page, final int size) {
        return new ConsultarEstudiantesRepositoryDTO(TIPO, 12345678, "Ana", "ana@", UUID.randomUUID(), null, null, null,
                Boolean.TRUE, page, size, ACTOR);
    }

    private static Object[] filaListado(final UUID id, final long total) {
        return new Object[]{id, UUID.randomUUID(), TIPO.toString(), 12345678, "Perez ", null, "Ana", null, "Ana Perez",
                "ana@uco.edu.co", Boolean.TRUE, total};
    }

    private static Object[] filaDetalle(final UUID institucion, final UUID grupo) {
        return new Object[]{ESTUDIANTE, UUID.randomUUID(), TIPO, 12345678, "Perez", null, "Ana", null, "Ana Perez",
                "ana@uco.edu.co", 1, institucion, "UCO", null, null, null, null, null, null, null, null, grupo, "G1"};
    }

    @Test
    void ejecuta_el_sp_con_parametros_nombrados_y_el_ejecutor() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(filaListado(ESTUDIANTE, 41)));

        final var pagina = repository.consultarEstudiantes(dto(0, 20));

        verify(entityManager).createNativeQuery(EstudianteJpaRepository.SQL_DIRECTORIO);
        assertTrue(EstudianteJpaRepository.SQL_DIRECTORIO.contains("EXEC dbo.usp_consultar_directorio_estudiantes_paginado"));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idTipoIdentificacion", TIPO.toString());
        verify(query).setParameter("numeroIdentificacion", "12345678");
        verify(query).setParameter("nombre", "Ana");
        verify(query).setParameter("correo", "ana@");
        verify(query).setParameter("activo", "1");
        verify(query).setParameter("idFacultad", null);
        verify(query).setParameter("idEstudiante", null);
        verify(query).setParameter("numeroPagina", 1);
        verify(query).setParameter("tamanoPagina", 20);
        assertEquals(41, pagina.totalItems());
        assertEquals(3, pagina.totalPages());
        assertEquals(0, pagina.page());
        assertEquals(20, pagina.size());
        final var e = pagina.items().getFirst();
        assertEquals(ESTUDIANTE, e.id());
        assertEquals(TIPO, e.tipoIdentificacionId());
        assertEquals(12345678, e.numeroIdentificacion());
        assertEquals("Perez", e.primerApellido());
        assertNull(e.segundoApellido());
        assertEquals("ana@uco.edu.co", e.correo());
        assertTrue(e.estaActivoUsuario());
    }

    @Test
    void la_pagina_http_se_traduce_a_numero_de_pagina_del_sp_y_sin_filas_no_hay_paginas() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final var pagina = repository.consultarEstudiantes(
                new ConsultarEstudiantesRepositoryDTO(null, null, " ", null, null, null, null, null, Boolean.FALSE, 3, 10, ACTOR));

        verify(query).setParameter("numeroPagina", 4);
        verify(query).setParameter("nombre", null);
        verify(query).setParameter("activo", "0");
        assertEquals(0, pagina.totalItems());
        assertEquals(0, pagina.totalPages());
        assertTrue(pagina.items().isEmpty());
    }

    @Test
    void una_fila_con_otra_cantidad_de_columnas_falla_de_forma_visible() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[11]));

        final DatabaseOperationException error = assertThrows(DatabaseOperationException.class,
                () -> repository.consultarEstudiantes(dto(0, 20)));
        assertInstanceOf(IllegalStateException.class, error.getCause());
    }

    @Test
    void entradas_invalidas_se_rechazan_sin_consultar() {
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantes(null));
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantes(
                new ConsultarEstudiantesRepositoryDTO(null, null, null, null, null, null, null, null, null, 0, 20, null)));
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantes(
                new ConsultarEstudiantesRepositoryDTO(null, null, null, null, null, null, null, null, null, -1, 20, ACTOR)));
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantes(
                new ConsultarEstudiantesRepositoryDTO(null, null, null, null, null, null, null, null, null, 0, 0, ACTOR)));
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantes(
                new ConsultarEstudiantesRepositoryDTO(null, null, null, null, null, null, null, null, null,
                        Integer.MAX_VALUE, 20, ACTOR)));
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantePorId(null, ACTOR));
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantePorId(ESTUDIANTE, null));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void detalle_fuera_de_ambito_o_inexistente_no_tiene_filas() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        assertTrue(repository.consultarEstudiantePorId(ESTUDIANTE, ACTOR).isEmpty());
        verify(query).setParameter("idEstudiante", ESTUDIANTE.toString());
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
    }

    @Test
    void detalle_agrupa_los_contextos_del_ambito_y_descarta_la_fila_sin_contexto() {
        final UUID institucion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.<Object>of(
                filaDetalle(institucion, grupo), filaDetalle(institucion, grupo), filaDetalle(null, null)));

        final var detalle = repository.consultarEstudiantePorId(ESTUDIANTE, ACTOR).orElseThrow();

        assertEquals(ESTUDIANTE, detalle.datosPersonales().id());
        assertTrue(detalle.datosPersonales().estaActivoUsuario());
        assertEquals(1, detalle.contextosAcademicos().size());
        assertEquals(institucion, detalle.contextosAcademicos().getFirst().idInstitucion());
        assertEquals(grupo, detalle.contextosAcademicos().getFirst().idGrupo());
    }

    @Test
    void detalle_con_una_unica_fila_sin_contexto_queda_sin_contextos() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(filaDetalle(null, null)));

        final var detalle = repository.consultarEstudiantePorId(ESTUDIANTE, ACTOR).orElseThrow();

        assertTrue(detalle.contextosAcademicos().isEmpty());
        assertFalse(detalle.datosPersonales().nombreCompleto().isBlank());
    }

    @Test
    void detalle_con_menos_columnas_falla_de_forma_visible() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[12]));

        assertThrows(DatabaseOperationException.class, () -> repository.consultarEstudiantePorId(ESTUDIANTE, ACTOR));
    }

    @Test
    void un_codigo_de_ambito_del_sp_se_traduce_a_403_y_un_fallo_tecnico_a_database_operation_exception() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_002|fuera de ambito"));
        assertThrows(ForbiddenException.class, () -> repository.consultarEstudiantes(dto(0, 20)));

        final Query falla = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(falla);
        when(falla.setParameter(anyString(), any())).thenReturn(falla);
        when(falla.getResultList()).thenThrow(new PersistenceException("timeout"));
        assertThrows(DatabaseOperationException.class, () -> repository.consultarEstudiantePorId(ESTUDIANTE, ACTOR));
    }

    @Test
    void numero_de_identificacion_como_texto_y_total_nulo_se_mapean() {
        final Object[] fila = filaListado(ESTUDIANTE, 0);
        fila[3] = "98765432";
        fila[10] = 0;
        fila[11] = null;
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(fila));

        final var pagina = repository.consultarEstudiantes(dto(0, 20));

        assertEquals(98765432, pagina.items().getFirst().numeroIdentificacion());
        assertFalse(pagina.items().getFirst().estaActivoUsuario());
        assertEquals(0, pagina.totalItems());
    }

    @Test
    void constructor_exige_entity_manager() {
        assertThrows(NullPointerException.class, () -> new EstudianteJpaRepository(null));
    }
}
