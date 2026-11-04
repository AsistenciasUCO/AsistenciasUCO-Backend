package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.TipoProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvTipoProgramaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** HU023/HU154: lectura de uv_tipo_programa; el catalogo es de solo consulta (John, 2026-11-04). */
class TipoProgramaJpaRepositoryTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void constructor_rechaza_entity_manager_nulo() {
        assertThrows(NullPointerException.class, () -> new TipoProgramaJpaRepository(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultar_mapea_las_filas_de_la_vista() {
        final EntityManager entityManager = mock(EntityManager.class);
        final TypedQuery<UvTipoProgramaEntity> query = mock(TypedQuery.class);
        final UvTipoProgramaEntity row = mock(UvTipoProgramaEntity.class);
        when(entityManager.createQuery(anyString(), eq(UvTipoProgramaEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(row));
        when(row.id()).thenReturn(ID);
        when(row.codigo()).thenReturn("PRE");
        when(row.nombre()).thenReturn("pregrado");
        when(row.estaActivoTipoPrograma()).thenReturn(false);

        final TipoProgramaProjection result = new TipoProgramaJpaRepository(entityManager).consultarTiposPrograma().get(0);

        assertEquals(ID, result.id());
        assertEquals("PRE", result.codigo());
        assertEquals("pregrado", result.nombre());
        assertFalse(result.estaActivo());
    }
}
