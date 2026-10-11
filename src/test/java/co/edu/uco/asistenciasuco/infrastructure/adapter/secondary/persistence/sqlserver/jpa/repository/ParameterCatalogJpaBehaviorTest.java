package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QUALITY-PR15 JPA03: unit contract of the parameter adapter.
 * An EntityManager mock does NOT establish real SQL Server/JPA mapping.
 * Candidate behavioral tests; RED/GREEN must be classified by Java 25 execution.
 */
class ParameterCatalogJpaBehaviorTest {
    private final EntityManager manager = mock(EntityManager.class);

    @SuppressWarnings("unchecked")
    private TypedQuery<String> queryReturning(final List<String> rows) {
        final TypedQuery<String> query = mock(TypedQuery.class);
        when(manager.createQuery(anyString(), eq(String.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(rows);
        return query;
    }

    @Test
    void parametro_con_grupo_o_clave_vacios_no_consulta_base_de_datos() {
        final ParameterCatalogJpaRepository catalog = new ParameterCatalogJpaRepository(manager);
        assertEquals(Optional.empty(), catalog.getParameter(null, "x"));
        assertEquals(Optional.empty(), catalog.getParameter("g", null));
        assertEquals(Optional.empty(), catalog.getParameter(" ", "x"));
        assertEquals(Optional.empty(), catalog.getParameter("g", " "));
        verifyNoInteractions(manager);
    }

    @Test
    void parametro_existente_usa_parametros_normalizados_y_cache_positivo() {
        final TypedQuery<String> query = queryReturning(List.of("30"));
        final ParameterCatalogJpaRepository catalog = new ParameterCatalogJpaRepository(manager);
        assertEquals(Optional.of("30"), catalog.getParameter(" seguridad ", " maxIntentos "));
        assertEquals(Optional.of("30"), catalog.getParameter("seguridad", "maxIntentos"));
        verify(query).setParameter("grupo", "seguridad");
        verify(query).setParameter("clave", "maxIntentos");
        verify(query).setMaxResults(1);
        verify(manager, times(1)).createQuery(anyString(), eq(String.class));
    }

    @Test
    void grupos_y_claves_distintos_no_colisionan_si_contienen_el_separador_del_cache() {
        // Previously "a::b"/"c" and "a"/"b::c" both encoded to "a::b::c".
        final TypedQuery<String> query = queryReturning(List.of("primer-valor"));
        when(query.getResultList()).thenReturn(List.of("primer-valor"), List.of("segundo-valor"));
        final ParameterCatalogJpaRepository catalog = new ParameterCatalogJpaRepository(manager);

        assertEquals(Optional.of("primer-valor"), catalog.getParameter(" a::b ", " c "));
        assertEquals(Optional.of("segundo-valor"), catalog.getParameter("a", "b::c"));
        assertEquals(Optional.of("primer-valor"), catalog.getParameter("a::b", "c"));
        assertEquals(Optional.of("segundo-valor"), catalog.getParameter("a", "b::c"));

        // Two distinct physical queries, then two cache hits. No state leaks.
        verify(manager, times(2)).createQuery(anyString(), eq(String.class));
        verify(query).setParameter("grupo", "a::b");
        verify(query).setParameter("clave", "c");
        verify(query).setParameter("grupo", "a");
        verify(query).setParameter("clave", "b::c");
    }

    @Test
    void parametro_inexistente_no_se_cachea_y_puede_aparecer_despues() {
        final TypedQuery<String> query = queryReturning(List.of());
        when(query.getResultList()).thenReturn(List.of(), List.of("nuevo"));
        final ParameterCatalogJpaRepository catalog = new ParameterCatalogJpaRepository(manager);
        assertEquals(Optional.empty(), catalog.getParameter("seguridad", "nuevo"));
        assertEquals(Optional.of("nuevo"), catalog.getParameter("seguridad", "nuevo"));
        verify(manager, times(2)).createQuery(anyString(), eq(String.class));
    }

    @Test
    void parametro_obligatorio_inexistente_genera_error_de_dominio_del_puerto() {
        queryReturning(List.of());
        final ParameterCatalogJpaRepository catalog = new ParameterCatalogJpaRepository(manager);
        assertThrows(ParameterCatalogPort.ParameterNotFoundException.class,
                () -> catalog.getRequiredParameter("seguridad", "desconocido"));
    }

    @Test
    void conversiones_tipadas_conservan_valor_de_negocio() {
        queryReturning(List.of("42"));
        final ParameterCatalogJpaRepository catalog = new ParameterCatalogJpaRepository(manager);
        assertEquals("42", catalog.getParameterAs("g", "k", String.class));
        assertEquals(Integer.valueOf(42), catalog.getParameterAs("g", "k", Integer.class));
        assertEquals(Long.valueOf(42L), catalog.getParameterAs("g", "k", Long.class));
        assertEquals(Double.valueOf(42.0), catalog.getParameterAs("g", "k", Double.class));
    }

    @Test
    void conversion_de_boolean_verdadero_y_falso_reconocidos() {
        final TypedQuery<String> query = queryReturning(List.of("true"));
        final ParameterCatalogJpaRepository catalog = new ParameterCatalogJpaRepository(manager);
        assertEquals(Boolean.TRUE, catalog.getParameterAs("feature", "activa", Boolean.class));
        when(query.getResultList()).thenReturn(List.of("false"));
        assertEquals(Boolean.FALSE, catalog.getParameterAs("feature", "inactiva", Boolean.class));
    }

    @Test
    void entero_invalido_no_se_convierte_a_cero_silenciosamente() {
        queryReturning(List.of("abc"));
        final ParameterCatalogJpaRepository catalog = new ParameterCatalogJpaRepository(manager);
        assertThrows(NumberFormatException.class,
                () -> catalog.getParameterAs("seguridad", "maxIntentos", Integer.class));
    }

    @Test
    void tipo_no_soportado_no_se_interpreta_por_aproximacion() {
        queryReturning(List.of("1"));
        final ParameterCatalogJpaRepository catalog = new ParameterCatalogJpaRepository(manager);
        assertThrows(IllegalArgumentException.class,
                () -> catalog.getParameterAs("seguridad", "limite", Float.class));
    }

    @Test
    void fallo_del_proveedor_conserva_causa_y_no_devuelve_optional_vacio() {
        when(manager.createQuery(anyString(), eq(String.class))).thenThrow(new PersistenceException("db down"));
        final ParameterCatalogJpaRepository catalog = new ParameterCatalogJpaRepository(manager);
        final ParameterCatalogPort.ParameterCatalogException error = assertThrows(
                ParameterCatalogPort.ParameterCatalogException.class,
                () -> catalog.getParameter("seguridad", "maxIntentos"));
        assertInstanceOf(PersistenceException.class, error.getCause());
    }
}
