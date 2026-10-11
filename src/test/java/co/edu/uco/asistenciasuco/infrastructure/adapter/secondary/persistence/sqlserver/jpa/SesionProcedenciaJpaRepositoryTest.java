package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvSesionV2Entity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.SesionProcedenciaJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SesionProcedenciaJpaRepositoryTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final LocalDateTime INICIO = LocalDateTime.parse("2042-07-15T14:00:00.1234567");
    private static final LocalDateTime FIN = LocalDateTime.parse("2042-07-15T15:30:00.7654321");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final TypedQuery<UvSesionV2Entity> query = mock(TypedQuery.class);
    private final SesionProcedenciaJpaRepository repository = new SesionProcedenciaJpaRepository(entityManager);

    @BeforeEach
    void prepareQuery() {
        when(entityManager.createQuery(anyString(), eq(UvSesionV2Entity.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
    }

    @Test
    void consulta_por_id_mapea_la_fila_y_retorna_vacio_si_no_existe() {
        final UvSesionV2Entity entity = entity("UTC_V2");
        when(query.getResultList()).thenReturn(List.of(entity));
        final Optional<SesionProcedenciaRepositoryProjection> result = repository.consultarSesion(SESION);
        assertEquals(Optional.of(new SesionProcedenciaRepositoryProjection(
                SESION, GRUPO, "Sesion", 1, "S-1", 2, "7", "Grupo", INICIO, FIN, "UTC_V2")), result);

        when(query.getResultList()).thenReturn(List.of());
        assertEquals(Optional.empty(), repository.consultarSesion(SESION));
    }

    @Test
    void consulta_por_grupo_preserva_orden_y_convierte_codigo_nulo() {
        final UvSesionV2Entity entity = entity(null);
        set(entity, "codigoGrupo", null);
        when(query.getResultList()).thenReturn(List.of(entity));
        final List<SesionProcedenciaRepositoryProjection> result = repository.consultarSesionesPorGrupo(GRUPO);
        assertEquals(1, result.size());
        assertEquals(null, result.getFirst().codigoGrupo());
        assertEquals(null, result.getFirst().procedenciaTemporal());
    }

    @Test
    void ids_nulos_se_rechazan_antes_de_consultar() {
        assertThrows(CrosscuttingException.class, () -> repository.consultarSesion(null));
        assertThrows(CrosscuttingException.class, () -> repository.consultarSesionesPorGrupo(null));
    }

    private static UvSesionV2Entity entity(final String procedencia) {
        final UvSesionV2Entity entity;
        try {
            final var constructor = UvSesionV2Entity.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            entity = constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
        set(entity, "id", SESION);
        set(entity, "idGrupo", GRUPO);
        set(entity, "nombre", "Sesion");
        set(entity, "numero", 1);
        set(entity, "codigo", "S-1");
        set(entity, "numeroSemana", 2);
        set(entity, "codigoGrupo", 7);
        set(entity, "nombreGrupo", "Grupo");
        set(entity, "fechaHoraInicio", INICIO);
        set(entity, "fechaHoraFin", FIN);
        set(entity, "procedenciaTemporal", procedencia);
        return entity;
    }

    private static void set(final UvSesionV2Entity target, final String name, final Object value) {
        try {
            final var field = UvSesionV2Entity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
