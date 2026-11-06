package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsulta;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU112/HU128/HU143/HU157: native EXEC de las consultas del decano y del administrador. */
class ConsultasTransversalesJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private ConsultasTransversalesJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new ConsultasTransversalesJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void constructor_rechaza_entity_manager_nulo() {
        assertThrows(NullPointerException.class, () -> new ConsultasTransversalesJpaRepository(null));
    }

    @Test
    void T_112_PAT_01_programas_creditos_enlaza_filtros_y_mapea_uuid_texto() {
        when(query.getResultList()).thenReturn(filas(fila(ID.toString().toUpperCase(), "Sistemas",
                "Pregrado", "Presencial", "Diurna", true, null, null, null, 0, 0, 0, 4)));

        final var pagina = repository.consultarProgramasCreditos(EJECUTOR, "sis", true, 2, 5);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_programas_creditos_facultad_paginado"));
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
        verify(query).setParameter("busqueda", "sis");
        verify(query).setParameter("estaActivo", true);
        assertEquals(4, pagina.total());
        assertEquals(ID, pagina.elementos().getFirst().idPrograma());
        assertNull(pagina.elementos().getFirst().idPlanEstudio());
        assertFalse(pagina.elementos().getFirst().planActivo());
    }

    @Test
    void T_128_PAT_01_docentes_enlaza_el_programa_y_el_estado() {
        when(query.getResultList()).thenReturn(filas(fila(ID, ID, 1017000111L, "Ana", "ana@uco.edu.co", 1,
                "Sistemas", 1, 3, 1)));

        final var docente = repository.consultarDocentes(EJECUTOR, new FiltroConsulta("ana", ID, true), 1, 10)
                .elementos().getFirst();

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_docentes_facultad_paginado"));
        verify(query).setParameter("idPadre", ID);
        assertEquals("1017000111", docente.numeroIdentificacion());
        assertTrue(docente.activo());
        assertEquals(3, docente.totalGrupos());
    }

    @Test
    void T_143_PAT_01_asignaturas_mapea_programa_y_facultad() {
        when(query.getResultList()).thenReturn(filas(fila(ID, "CAL1", "Calculo", 3, "Ciencias", "Basico",
                "S1", ID, 2024, ID, "Sistemas", ID, "Ingenieria", true, 1)));

        final var pagina = repository.buscarAsignaturas(EJECUTOR, new FiltroConsulta(null, null, null), 1, 10);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_buscar_asignaturas_paginado"));
        assertEquals("Ingenieria", pagina.elementos().getFirst().nombreFacultad());
        assertEquals(3, pagina.elementos().getFirst().creditos());
        assertEquals(1, pagina.total());
    }

    @Test
    void T_157_PAT_01_estudiantes_sin_filas_da_total_cero() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final var pagina = repository.buscarEstudiantes(EJECUTOR, new FiltroConsulta(null, ID, false), 1, 10);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_buscar_estudiantes_institucion_paginado"));
        assertEquals(0, pagina.total());
        assertTrue(pagina.elementos().isEmpty());
    }

    @Test
    void T_157_PAT_02_estudiantes_mapea_facultades_y_programas() {
        when(query.getResultList()).thenReturn(filas(fila(ID, ID, 101, "Ana", null, false, "Ingenieria",
                "Sistemas", 2, 1)));

        final var estudiante = repository.buscarEstudiantes(EJECUTOR, new FiltroConsulta(null, null, null), 1, 10)
                .elementos().getFirst();

        assertEquals("Ingenieria", estudiante.facultades());
        assertFalse(estudiante.activo());
    }

    @Test
    void T_157_PAT_03_historial_con_grupo_y_sin_grupo() {
        final UUID grupo = UUID.randomUUID();
        when(query.getResultList()).thenReturn(filas(
                historial(grupo, 10, 8, 2, 0, new BigDecimal("20.00"), "RIESGO"),
                historial(null, null, null, null, null, null, null)));

        final var filas = repository.consultarHistorial(EJECUTOR, ID);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_historial_estudiante"));
        verify(query).setParameter("idEstudiante", ID);
        assertEquals(2, filas.size());
        assertEquals(grupo, filas.getFirst().idGrupo());
        assertEquals(new BigDecimal("20.00"), filas.getFirst().resumen().porcentajeInasistencia());
        assertEquals("RIESGO", filas.getFirst().resumen().codigoPermanencia());
        assertNull(filas.get(1).resumen());
    }

    @Test
    void T_157_PAT_04_porcentaje_no_decimal_se_convierte() {
        when(query.getResultList()).thenReturn(filas(historial(ID, 4, 3, 1, 0, 25.5d, "REGULAR")));

        assertEquals(new BigDecimal("25.5"),
                repository.consultarHistorial(EJECUTOR, ID).getFirst().resumen().porcentajeInasistencia());
    }

    @Test
    void T_ERR_PAT_01_codigos_de_la_db_se_traducen() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=PAG_001|Paginacion invalida"));
        assertThrows(ValidationException.class, () -> repository.consultarProgramasCreditos(EJECUTOR, null, null, 0, 10));
    }

    @Test
    void T_ERR_PAT_02_sec_001_es_403() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_001|RBAC denegado"));
        final var filtro = new FiltroConsulta(null, null, null);
        final ForbiddenException error = assertThrows(ForbiddenException.class,
                () -> repository.buscarEstudiantes(EJECUTOR, filtro, 1, 10));
        assertEquals("SEC_001", error.getCode());
    }

    @Test
    void T_ERR_PAT_03_fallo_sin_codigo_es_fallo_tecnico() {
        when(query.getResultList()).thenThrow(new PersistenceException("timeout"));
        assertThrows(DatabaseOperationException.class, () -> repository.consultarHistorial(EJECUTOR, ID));
    }

    /** Lista de filas Object[] tal como la entrega getResultList (sin expandir el arreglo como varargs). */
    private static List<Object> filas(final Object[]... filas) {
        return Arrays.stream(filas).map(Object.class::cast).toList();
    }

    private static Object[] fila(final Object... valores) {
        return valores;
    }

    private static Object[] historial(final UUID grupo, final Integer sesiones, final Integer asistencias,
                                      final Integer faltas, final Integer excusas, final Object porcentaje,
                                      final String permanencia) {
        return Arrays.asList(ID, ID, "CC", 101, "Ana Ruiz", "ana@uco.edu.co", true, ID, "Ingenieria", ID, "Sistemas",
                2024, ID, "2026-1", "Primer semestre", grupo, grupo == null ? null : 1, ID, "Calculo", "MAT",
                "Matriculado", sesiones, asistencias, faltas, excusas, porcentaje, permanencia).toArray();
    }

    @Test
    void T_136_PAT_01_docentes_de_la_institucion_enlazan_la_facultad_y_el_decano_la_deja_nula() {
        when(query.getResultList()).thenReturn(filas(fila(ID, ID, 1017000111L, "Ana", "ana@uco.edu.co", 1,
                "Sistemas", 1, 3, 1)));

        when(query.getResultList()).thenReturn(filas(fila(ID, ID, 1017000111L, "Ana", "ana@uco.edu.co", 1,
                null, 0, 0, 1, 0)));
        final var retirado = repository.consultarDocentes(EJECUTOR, new FiltroConsulta(null, null, null), ID, "SIN_VINCULO", 1, 10)
                .elementos().getFirst();
        verify(query).setParameter("idFacultad", ID);
        verify(query).setParameter("estadoVinculo", "SIN_VINCULO");
        org.junit.jupiter.api.Assertions.assertFalse(retirado.tieneVinculoActivo());

        repository.consultarDocentes(EJECUTOR, new FiltroConsulta(null, null, null), 1, 10);
        verify(query).setParameter("idFacultad", null);
    }
}
