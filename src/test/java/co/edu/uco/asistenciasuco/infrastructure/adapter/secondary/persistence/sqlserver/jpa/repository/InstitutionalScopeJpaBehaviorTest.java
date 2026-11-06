package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.InstitutionalIdentityAmbiguityException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * QUALITY-PR15 J06: alcance institucional grant/deny/fallo. Toda ausencia o resultado no
 * interpretable niega; un fallo del provider se propaga (nunca se convierte en acceso). El
 * EntityManager simulado no certifica SQL Server: ver {@code AuthorizationReportJpaParityIT}.
 */
class InstitutionalScopeJpaBehaviorTest {

    private static final UUID USUARIO = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID RECURSO = UUID.fromString("00000000-0000-0000-0000-0000000000d2");

    private final EntityManager manager = mock(EntityManager.class);
    private final InstitutionalScopeJpaRepository scope = new InstitutionalScopeJpaRepository(manager);

    @Test
    void cada_resolucion_de_identidad_usa_su_consulta_y_su_parametro() {
        assertResolves(InstitutionalScopeJpaRepository.HQL_USUARIO_POR_ID, "usuarioId", USUARIO,
                s -> s.findUsuarioIdById(USUARIO));
        assertResolves(InstitutionalScopeJpaRepository.HQL_USUARIO_POR_EMAIL, "email", "Luz@UCO.edu.co",
                s -> s.findUsuarioIdByEmail("Luz@UCO.edu.co"));
        assertResolves(InstitutionalScopeJpaRepository.HQL_DOCENTE_POR_USUARIO, "usuarioId", USUARIO,
                s -> s.findDocenteIdByUsuario(USUARIO));
        assertResolves(InstitutionalScopeJpaRepository.HQL_ESTUDIANTE_POR_USUARIO, "usuarioId", USUARIO,
                s -> s.findEstudianteIdByUsuario(USUARIO));
        assertResolves(InstitutionalScopeJpaRepository.HQL_PROGRAMA_POR_COORDINADOR, "usuarioId", USUARIO,
                s -> s.findProgramaIdByCoordinadorUsuario(USUARIO));
        assertResolves(InstitutionalScopeJpaRepository.HQL_COORDINADOR_POR_USUARIO, "usuarioId", USUARIO,
                s -> s.findCoordinadorIdByUsuario(USUARIO));
        assertResolves(InstitutionalScopeJpaRepository.HQL_FACULTAD_POR_DECANO, "usuarioId", USUARIO,
                s -> s.findFacultadIdByDecanoUsuario(USUARIO));
        assertResolves(InstitutionalScopeJpaRepository.HQL_DECANO_POR_USUARIO, "usuarioId", USUARIO,
                s -> s.findDecanoIdByUsuario(USUARIO));
    }

    @Test
    void el_programa_del_coordinador_se_resuelve_de_forma_determinista_prefiriendo_el_activo() {
        // Certificación Fase 3: con datos anteriores a la regla 1 a 1, cada pantalla tomaba un programa distinto.
        final String hql = InstitutionalScopeJpaRepository.HQL_PROGRAMA_POR_COORDINADOR;
        assertTrue(hql.contains("order by case when p.estaActivoPrograma = true then 0 else 1 end, p.nombrePrograma, c.idPrograma"));
        assertTrue(hql.contains("p.id = c.idPrograma"));
    }

    @Test
    void identidad_inexistente_o_nula_es_ausencia() {
        uuidQuery(InstitutionalScopeJpaRepository.HQL_DOCENTE_POR_USUARIO, List.of());
        assertEquals(Optional.empty(), scope.findDocenteIdByUsuario(USUARIO));

        uuidQuery(InstitutionalScopeJpaRepository.HQL_DECANO_POR_USUARIO, Arrays.asList((UUID) null));
        assertEquals(Optional.empty(), scope.findDecanoIdByUsuario(USUARIO));
    }

    @Test
    void correo_duplicado_no_elige_la_primera_coincidencia() {
        final TypedQuery<UUID> query = uuidQuery(InstitutionalScopeJpaRepository.HQL_USUARIO_POR_EMAIL,
                Arrays.asList(RECURSO, USUARIO));

        assertThrows(InstitutionalIdentityAmbiguityException.class,
                () -> scope.findUsuarioIdByEmail("duplicado@uco.edu.co"));
        verify(query, never()).setMaxResults(anyInt());
    }

    @Test
    void usuario_inactivo_no_se_considera_vinculable() {
        final TypedQuery<Boolean> query = mock(TypedQuery.class, RETURNS_SELF);
        when(query.getResultList()).thenReturn(List.of(Boolean.FALSE));
        when(manager.createQuery(eq(InstitutionalScopeJpaRepository.HQL_USUARIO_ACTIVO), eq(Boolean.class)))
                .thenReturn(query);

        assertFalse(scope.isUsuarioActivo(USUARIO));
        verify(query).setMaxResults(1);
    }

    @Test
    void coordinador_y_decano_solo_acceden_con_conteo_positivo() {
        countQuery(InstitutionalScopeJpaRepository.HQL_COORDINADOR_DE_PROGRAMA, 1L);
        assertTrue(scope.canCoordinadorAccessPrograma(USUARIO, RECURSO));

        countQuery(InstitutionalScopeJpaRepository.HQL_DECANO_DE_FACULTAD, 0L);
        assertFalse(scope.canDecanoAccessFacultad(USUARIO, RECURSO));

        final TypedQuery<Long> nulo = countQuery(InstitutionalScopeJpaRepository.HQL_COORDINADOR_DE_PROGRAMA, null);
        assertFalse(scope.canCoordinadorAccessPrograma(USUARIO, RECURSO));
        verify(nulo, atLeastOnce()).setParameter("usuarioId", USUARIO);
        verify(nulo, atLeastOnce()).setParameter("programaId", RECURSO);
    }

    @Test
    void docente_y_estudiante_acceden_por_conteo_nativo_y_niegan_resultados_no_numericos() {
        final Query docente = nativeQuery(InstitutionalScopeJpaRepository.SQL_DOCENTE_ACCEDE_GRUPO, 2);
        assertTrue(scope.canDocenteAccessGrupo(USUARIO, RECURSO));
        verify(docente).setParameter("grupoId", RECURSO);
        verify(docente).setParameter("usuarioId", USUARIO);

        nativeQuery(InstitutionalScopeJpaRepository.SQL_ESTUDIANTE_ACCEDE_GRUPO, "1");
        assertFalse(scope.canEstudianteAccessGrupo(USUARIO, RECURSO));
    }

    @Test
    void fallo_del_provider_nunca_concede_acceso() {
        when(manager.createQuery(anyString(), any(Class.class))).thenThrow(new PersistenceException("db down"));
        when(manager.createNativeQuery(anyString())).thenThrow(new PersistenceException("db down"));

        assertThrows(DatabaseOperationException.class, () -> scope.findUsuarioIdById(USUARIO));
        assertThrows(DatabaseOperationException.class, () -> scope.canCoordinadorAccessPrograma(USUARIO, RECURSO));
        assertThrows(DatabaseOperationException.class, () -> scope.canDecanoAccessFacultad(USUARIO, RECURSO));
        assertThrows(DatabaseOperationException.class, () -> scope.canDocenteAccessGrupo(USUARIO, RECURSO));
        assertThrows(DatabaseOperationException.class, () -> scope.canEstudianteAccessGrupo(USUARIO, RECURSO));
    }

    @Test
    void entity_manager_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new InstitutionalScopeJpaRepository(null));
    }

    private void assertResolves(final String hql, final String parameter, final Object value,
                                final Function<InstitutionalScopeJpaRepository, Optional<UUID>> call) {
        final TypedQuery<UUID> query = uuidQuery(hql, List.of(RECURSO));
        assertEquals(Optional.of(RECURSO), call.apply(scope));
        verify(query).setParameter(parameter, value);
        if (!hql.equals(InstitutionalScopeJpaRepository.HQL_USUARIO_POR_EMAIL)) {
            verify(query).setMaxResults(1);
        }
    }

    @SuppressWarnings("unchecked")
    private TypedQuery<UUID> uuidQuery(final String hql, final List<UUID> rows) {
        final TypedQuery<UUID> query = mock(TypedQuery.class, RETURNS_SELF);
        when(query.getResultList()).thenReturn(rows);
        when(manager.createQuery(eq(hql), eq(UUID.class))).thenReturn(query);
        return query;
    }

    @SuppressWarnings("unchecked")
    private TypedQuery<Long> countQuery(final String hql, final Long count) {
        final TypedQuery<Long> query = mock(TypedQuery.class, RETURNS_SELF);
        when(query.getSingleResult()).thenReturn(count);
        when(manager.createQuery(eq(hql), eq(Long.class))).thenReturn(query);
        return query;
    }

    private Query nativeQuery(final String sql, final Object result) {
        final Query query = mock(Query.class, RETURNS_SELF);
        when(query.getSingleResult()).thenReturn(result);
        when(manager.createNativeQuery(sql)).thenReturn(query);
        return query;
    }
}
