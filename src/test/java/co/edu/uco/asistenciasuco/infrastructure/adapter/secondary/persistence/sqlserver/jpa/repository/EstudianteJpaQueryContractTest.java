package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteDetalleRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudiantePaginaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvEstudianteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteResumenQueryRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * QUALITY-PR15 J05 + RED Sonar java:S2077 (AaEaHMGDE62pkux_HRML linea 62, AaEaHMGDE62pkux_HRMM
 * linea 67): la consulta paginada de estudiantes no debe formatear dinamicamente su JPQL segun los
 * filtros recibidos; todo valor de filtro viaja como parametro enlazado.
 *
 * <p>EntityManager mock: verifica el contrato del adapter, NO certifica SQL Server. La semantica
 * real de filtros/paginacion se certifica con {@code CoreViewQueriesJpaParityIT}.</p>
 */
class EstudianteJpaQueryContractTest {

    private final EntityManager manager = mock(EntityManager.class);
    @SuppressWarnings("unchecked")
    private final TypedQuery<Long> countQuery = mock(TypedQuery.class, RETURNS_SELF);
    @SuppressWarnings("unchecked")
    private final TypedQuery<EstudianteResumenQueryRow> rowsQuery = mock(TypedQuery.class, RETURNS_SELF);

    @Test
    void el_texto_jpql_no_depende_de_los_filtros_recibidos() {
        final List<String> sinFiltros = executedQueries(dto(null, null, null, null, null, null, null, null, null, 0, 10));
        final List<String> conTodosLosFiltros = executedQueries(dto(UUID.randomUUID(), 1234, "ana", "Ana@UCO",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Boolean.TRUE, 0, 10));

        assertEquals(sinFiltros, conTodosLosFiltros,
                "El JPQL debe ser constante; los filtros solo cambian parametros enlazados.");
    }

    @Test
    void los_valores_de_filtro_viajan_como_parametros_enlazados_y_normalizados() {
        final UUID tipo = UUID.randomUUID();
        final UUID institucion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final List<String> queries = executedQueries(dto(tipo, 1234, "ana' or 1=1 --", "Ana@UCO",
                institucion, null, null, grupo, Boolean.FALSE, 0, 10));

        verify(rowsQuery).setParameter("tipoIdentificacionId", tipo);
        verify(rowsQuery).setParameter("numeroIdentificacion", 1234);
        verify(rowsQuery).setParameter("nombre", "%ANA' OR 1=1 --%");
        verify(rowsQuery).setParameter("correo", "%ana@uco%");
        verify(rowsQuery).setParameter("institucionId", institucion);
        verify(rowsQuery).setParameter("grupoId", grupo);
        verify(rowsQuery).setParameter("activo", Boolean.FALSE);
        verify(countQuery).setParameter("nombre", "%ANA' OR 1=1 --%");
        queries.forEach(jpql -> assertTrue(!jpql.contains("1=1 --") && !jpql.toLowerCase().contains("ana@uco"),
                "Ningun valor del usuario puede aparecer en el texto JPQL: " + jpql));
    }

    @Test
    void la_paginacion_calcula_desplazamiento_total_y_paginas() {
        when(countQuery.getSingleResult()).thenReturn(21L);
        when(rowsQuery.getResultList()).thenReturn(List.of(row()));
        stubQueries();

        final EstudiantePaginaRepositoryProjection page = new EstudianteJpaRepository(manager)
                .consultarEstudiantes(dto(null, null, null, null, null, null, null, null, null, 2, 10));

        verify(rowsQuery).setFirstResult(20);
        verify(rowsQuery).setMaxResults(10);
        assertEquals(21L, page.totalItems());
        assertEquals(3, page.totalPages());
        assertEquals(2, page.page());
        assertEquals(10, page.size());
        assertEquals(1, page.items().size());
        assertEquals("Ana Ruiz", page.items().getFirst().nombreCompleto());
    }

    @Test
    void sin_resultados_no_hay_paginas() {
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(rowsQuery.getResultList()).thenReturn(List.of());
        stubQueries();

        final EstudiantePaginaRepositoryProjection page = new EstudianteJpaRepository(manager)
                .consultarEstudiantes(dto(null, null, null, null, null, null, null, null, null, 0, 10));

        assertEquals(0L, page.totalItems());
        assertEquals(0, page.totalPages());
        assertTrue(page.items().isEmpty());
    }

    @Test
    void pagina_negativa_es_rechazada_antes_de_consultar_sql() {
        final ConsultarEstudiantesRepositoryDTO invalid = dto(
                null, null, null, null, null, null, null, null, null, -1, 10);

        assertThrows(CrosscuttingException.class,
                () -> new EstudianteJpaRepository(manager).consultarEstudiantes(invalid));
        verifyNoInteractions(manager);
    }

    @Test
    void tamano_cero_o_negativo_se_rechaza_sin_acceder_a_jpa() {
        final EstudianteJpaRepository repository = new EstudianteJpaRepository(manager);
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantes(
                dto(null, null, null, null, null, null, null, null, null, 0, 0)));
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantes(
                dto(null, null, null, null, null, null, null, null, null, 0, -10)));
        verifyNoInteractions(manager);
    }

    @Test
    void offset_desbordado_se_rechaza_antes_de_jpa_y_sin_wrap_de_excepcion_sql() {
        final ConsultarEstudiantesRepositoryDTO invalid = dto(
                null, null, null, null, null, null, null, null, null, Integer.MAX_VALUE, 2);

        assertThrows(CrosscuttingException.class,
                () -> new EstudianteJpaRepository(manager).consultarEstudiantes(invalid));
        verifyNoInteractions(manager);
    }

    @Test
    void entradas_nulas_se_rechazan_sin_consultar() {
        final EstudianteJpaRepository repository = new EstudianteJpaRepository(manager);
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantes(null));
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantePorId(null));
        verifyNoInteractions(manager);
    }

    @Test
    void estudiante_inexistente_no_consulta_contextos() {
        when(rowsQuery.getResultList()).thenReturn(List.of());
        when(manager.createQuery(anyString(), eq(EstudianteResumenQueryRow.class))).thenReturn(rowsQuery);

        assertEquals(Optional.empty(), new EstudianteJpaRepository(manager).consultarEstudiantePorId(UUID.randomUUID()));
        verify(manager, org.mockito.Mockito.never()).createQuery(anyString(), eq(UvEstudianteEntity.class));
    }

    @Test
    void estudiante_existente_incluye_sus_contextos_academicos() {
        @SuppressWarnings("unchecked")
        final TypedQuery<UvEstudianteEntity> contextQuery = mock(TypedQuery.class, RETURNS_SELF);
        final UvEstudianteEntity contexto = mock(UvEstudianteEntity.class);
        when(contexto.nombreInstitucion()).thenReturn("UCO");
        when(contexto.inpPlanEstudio()).thenReturn(77);
        when(contextQuery.getResultList()).thenReturn(List.of(contexto));
        when(rowsQuery.getResultList()).thenReturn(List.of(row()));
        when(manager.createQuery(anyString(), eq(EstudianteResumenQueryRow.class))).thenReturn(rowsQuery);
        when(manager.createQuery(anyString(), eq(UvEstudianteEntity.class))).thenReturn(contextQuery);
        final UUID estudiante = UUID.randomUUID();

        final EstudianteDetalleRepositoryProjection detalle =
                new EstudianteJpaRepository(manager).consultarEstudiantePorId(estudiante).orElseThrow();

        verify(contextQuery).setParameter("estudianteId", estudiante);
        assertEquals("Ana Ruiz", detalle.datosPersonales().nombreCompleto());
        assertEquals(1, detalle.contextosAcademicos().size());
        assertEquals("UCO", detalle.contextosAcademicos().getFirst().nombreInstitucion());
        assertEquals("77", detalle.contextosAcademicos().getFirst().inpPlanEstudio());
    }

    private List<String> executedQueries(final ConsultarEstudiantesRepositoryDTO dto) {
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(rowsQuery.getResultList()).thenReturn(List.of());
        stubQueries();
        new EstudianteJpaRepository(manager).consultarEstudiantes(dto);
        final ArgumentCaptor<String> count = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> rows = ArgumentCaptor.forClass(String.class);
        verify(manager, atLeastOnce()).createQuery(count.capture(), eq(Long.class));
        verify(manager, atLeastOnce()).createQuery(rows.capture(), eq(EstudianteResumenQueryRow.class));
        return List.of(count.getValue(), rows.getValue());
    }

    private void stubQueries() {
        when(manager.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
        when(manager.createQuery(anyString(), eq(EstudianteResumenQueryRow.class))).thenReturn(rowsQuery);
    }

    private static EstudianteResumenQueryRow row() {
        return new EstudianteResumenQueryRow(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1234,
                "Ruiz", null, "Ana", null, "Ana Ruiz", "ana@uco.edu.co", Boolean.TRUE);
    }

    @SuppressWarnings("java:S107")
    private static ConsultarEstudiantesRepositoryDTO dto(final UUID tipo, final Integer numero, final String nombre,
                                                          final String correo, final UUID institucion,
                                                          final UUID facultad, final UUID programa, final UUID grupo,
                                                          final Boolean activo, final int page, final int size) {
        return new ConsultarEstudiantesRepositoryDTO(tipo, numero, nombre, correo, institucion, facultad, programa,
                grupo, activo, page, size);
    }
}
