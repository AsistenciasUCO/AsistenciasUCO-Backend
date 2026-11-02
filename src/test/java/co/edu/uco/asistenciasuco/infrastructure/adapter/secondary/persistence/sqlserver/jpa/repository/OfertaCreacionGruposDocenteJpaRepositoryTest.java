package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** F3: EXEC de la oferta de creacion de grupos del docente con binding nombrado, correlacion y mapeo de columnas. */
class OfertaCreacionGruposDocenteJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private OfertaCreacionGruposDocenteJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new OfertaCreacionGruposDocenteJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void ejecuta_el_sp_con_parametros_nombrados_y_mapea_las_columnas() {
        final UUID docente = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        when(query.getResultList()).thenReturn(java.util.Collections.singletonList(
                new Object[]{docente, asignatura, "MAT1 ", "Algebra", programa, "Ingenieria de Sistemas "}));

        final var oferta = repository.consultarOferta(ACTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_oferta_creacion_grupo_docente"));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        assertEquals(1, oferta.size());
        assertEquals(asignatura, oferta.getFirst().idAsignatura());
        assertEquals("MAT1", oferta.getFirst().codigoAsignatura());
        assertEquals("Algebra", oferta.getFirst().nombreAsignatura());
        assertEquals(programa, oferta.getFirst().idPrograma());
        assertEquals("Ingenieria de Sistemas", oferta.getFirst().nombrePrograma());
    }

    @Test
    void sin_filas_devuelve_una_lista_vacia() {
        when(query.getResultList()).thenReturn(List.of());

        assertTrue(repository.consultarOferta(ACTOR).isEmpty());
    }

    @Test
    void sin_usuario_ejecutor_falla_antes_de_consultar() {
        assertThrows(CrosscuttingException.class, () -> repository.consultarOferta(null));

        verify(entityManager, never()).createNativeQuery(anyString());
    }
}
