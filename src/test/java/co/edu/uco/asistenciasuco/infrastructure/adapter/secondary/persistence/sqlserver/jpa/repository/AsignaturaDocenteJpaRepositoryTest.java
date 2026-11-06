package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaDocenteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvProgramaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU058: las asignaturas del docente traen el tipo de programa (nivel formativo) desde uv_programa. */
class AsignaturaDocenteJpaRepositoryTest {

    private static final UUID DOCENTE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROGRAMA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TIPO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    @SuppressWarnings("unchecked")
    private final TypedQuery<UvDocenteEntity> docentes = mock(TypedQuery.class);
    @SuppressWarnings("unchecked")
    private final TypedQuery<UvProgramaEntity> programas = mock(TypedQuery.class);
    @SuppressWarnings("unchecked")
    private final TypedQuery<Object[]> grupos = mock(TypedQuery.class);
    private final AsignaturaDocenteJpaRepository repository = new AsignaturaDocenteJpaRepository(entityManager);

    @Test
    void constructor_rechaza_entity_manager_nulo() {
        assertThrows(NullPointerException.class, () -> new AsignaturaDocenteJpaRepository(null));
    }

    @Test
    void T_058_PAT_01_agrega_el_tipo_de_programa_de_cada_grupo() {
        final UvDocenteEntity conPrograma = fila(PROGRAMA);
        final UvDocenteEntity sinPrograma = fila(null);
        final UvProgramaEntity programa = mock(UvProgramaEntity.class);
        when(programa.id()).thenReturn(PROGRAMA);
        when(programa.idTipoPrograma()).thenReturn(TIPO);
        when(programa.nombreTipoPrograma()).thenReturn("pregrado");
        prepararDocentes(List.of(conPrograma, sinPrograma));
        when(entityManager.createQuery(eq(AsignaturaDocenteJpaRepository.HQL_PROGRAMAS), eq(UvProgramaEntity.class)))
                .thenReturn(programas);
        when(programas.setParameter(anyString(), any())).thenReturn(programas);
        when(programas.getResultList()).thenReturn(List.of(programa));

        final List<AsignaturaDocenteProjection> resultado = repository.consultarAsignaturasDocente(DOCENTE);

        assertEquals(TIPO, resultado.get(0).idTipoPrograma());
        assertEquals("pregrado", resultado.get(0).nombreTipoPrograma());
        assertNull(resultado.get(1).idTipoPrograma());
        assertNull(resultado.get(1).nombreTipoPrograma());
    }

    @Test
    void T_058_PAT_02_sin_programas_no_consulta_uv_programa() {
        prepararDocentes(List.of(fila(null)));

        final List<AsignaturaDocenteProjection> resultado = repository.consultarAsignaturasDocente(DOCENTE);

        assertNull(resultado.getFirst().idTipoPrograma());
        verify(entityManager, never()).createQuery(eq(AsignaturaDocenteJpaRepository.HQL_PROGRAMAS), eq(UvProgramaEntity.class));
    }

    @Test
    void T_APP04_01_el_conteo_del_grupo_coexiste_con_el_tipo_de_programa() {
        final UvDocenteEntity fila = fila(PROGRAMA);
        final UUID grupo = UUID.randomUUID();
        when(fila.idGrupo()).thenReturn(grupo);
        final UvProgramaEntity programa = mock(UvProgramaEntity.class);
        when(programa.id()).thenReturn(PROGRAMA);
        when(programa.idTipoPrograma()).thenReturn(TIPO);
        when(programa.nombreTipoPrograma()).thenReturn("pregrado");
        prepararDocentes(List.of(fila));
        when(entityManager.createQuery(eq(AsignaturaDocenteJpaRepository.HQL_PROGRAMAS), eq(UvProgramaEntity.class)))
                .thenReturn(programas);
        when(programas.setParameter(anyString(), any())).thenReturn(programas);
        when(programas.getResultList()).thenReturn(List.of(programa));
        when(grupos.getResultList()).thenReturn(List.<Object[]>of(new Object[]{grupo, 7, 30}));

        final AsignaturaDocenteProjection resultado = repository.consultarAsignaturasDocente(DOCENTE).getFirst();

        assertEquals(7, resultado.estudiantesActivos());
        // Certificación Fase 3: el cupo máximo real del grupo, para que el hub no muestre uno inventado.
        assertEquals(30, resultado.capacidadMaximaPermitida());
        assertEquals(TIPO, resultado.idTipoPrograma());
        assertEquals("pregrado", resultado.nombreTipoPrograma());
        verify(grupos).setParameter(eq("idsGrupo"), eq(List.of(grupo)));
    }

    @Test
    void T_APP04_02_sin_grupo_no_consulta_conteo_y_no_inventa_matriculas() {
        final UvDocenteEntity fila = fila(null);
        when(fila.idGrupo()).thenReturn(null);
        prepararDocentes(List.of(fila));

        final AsignaturaDocenteProjection resultado = repository.consultarAsignaturasDocente(DOCENTE).getFirst();

        assertNull(resultado.estudiantesActivos());
        assertNull(resultado.capacidadMaximaPermitida());
        verify(entityManager, never()).createQuery(eq(AsignaturaDocenteJpaRepository.HQL_ESTUDIANTES_ACTIVOS),
                eq(Object[].class));
    }

    @Test
    void T_APP04_03_sin_filas_no_consulta_programas_ni_grupos() {
        prepararDocentes(List.of());

        assertEquals(List.of(), repository.consultarAsignaturasDocente(DOCENTE));
        verify(entityManager, never()).createQuery(eq(AsignaturaDocenteJpaRepository.HQL_ESTUDIANTES_ACTIVOS),
                eq(Object[].class));
        verify(entityManager, never()).createQuery(eq(AsignaturaDocenteJpaRepository.HQL_PROGRAMAS),
                eq(UvProgramaEntity.class));
    }

    private void prepararDocentes(final List<UvDocenteEntity> filas) {
        when(entityManager.createQuery(eq(AsignaturaDocenteJpaRepository.HQL_POR_DOCENTE), eq(UvDocenteEntity.class)))
                .thenReturn(docentes);
        when(docentes.setParameter(anyString(), any())).thenReturn(docentes);
        when(docentes.getResultList()).thenReturn(filas);
        when(entityManager.createQuery(eq(AsignaturaDocenteJpaRepository.HQL_ESTUDIANTES_ACTIVOS), eq(Object[].class)))
                .thenReturn(grupos);
        when(grupos.setParameter(anyString(), any())).thenReturn(grupos);
        when(grupos.getResultList()).thenReturn(List.of());
    }

    private static UvDocenteEntity fila(final UUID idPrograma) {
        final UvDocenteEntity fila = mock(UvDocenteEntity.class);
        when(fila.idAsignatura()).thenReturn(UUID.randomUUID());
        when(fila.nombreAsignatura()).thenReturn("Calculo");
        when(fila.idGrupo()).thenReturn(UUID.randomUUID());
        when(fila.nombreGrupo()).thenReturn("G1");
        when(fila.idPrograma()).thenReturn(idPrograma);
        when(fila.nombrePrograma()).thenReturn(idPrograma == null ? null : "Sistemas");
        return fila;
    }
}
