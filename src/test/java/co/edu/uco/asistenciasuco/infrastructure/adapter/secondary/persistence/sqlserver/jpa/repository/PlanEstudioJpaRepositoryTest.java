package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvPlanEstudioEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.HydratedViewRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU081-HU083: comandos de estado y semestres del plan (EXEC) y total de semestres en la consulta de planes. */
class PlanEstudioJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PLAN = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PLAN_SIN_SEMESTRES = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PROGRAMA = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private PlanEstudioJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new PlanEstudioJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "ok", "", true}));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void cambiarEstado_ejecuta_el_sp_con_el_estado_y_el_ejecutor() {
        repository.cambiarEstadoPlanEstudio(PLAN, false, ACTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_cambiar_estado_plan_estudio"));
        verify(query).setParameter("idPlanEstudio", PLAN);
        verify(query).setParameter("estaActivo", false);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
    }

    @Test
    void agregarSemestre_ejecuta_el_sp_del_plan() {
        repository.agregarSemestrePlanEstudio(PLAN, ACTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_agregar_semestre_plan_estudio"));
        verify(query).setParameter("idPlanEstudio", PLAN);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
    }

    @Test
    void quitarSemestre_ejecuta_el_sp_con_el_numero_de_semestre() {
        repository.quitarSemestrePlanEstudio(PLAN, 7, ACTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_quitar_semestre_plan_estudio"));
        verify(query).setParameter("idPlanEstudio", PLAN);
        verify(query).setParameter("numeroSemestre", 7);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
    }

    @Test
    void comandos_exigen_ejecutor_sin_llegar_a_la_db() {
        assertThrows(CrosscuttingException.class, () -> repository.cambiarEstadoPlanEstudio(PLAN, true, null));
        assertThrows(CrosscuttingException.class, () -> repository.agregarSemestrePlanEstudio(PLAN, null));
        assertThrows(CrosscuttingException.class, () -> repository.quitarSemestrePlanEstudio(PLAN, 1, null));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarPlanesPorPrograma_incluye_el_mayor_semestre_de_cada_plan() {
        final TypedQuery<UvPlanEstudioEntity> planes = mock(TypedQuery.class);
        final TypedQuery<Object[]> totales = mock(TypedQuery.class);
        when(entityManager.createQuery(PlanEstudioJpaRepository.HQL_POR_PROGRAMA, UvPlanEstudioEntity.class)).thenReturn(planes);
        when(planes.setParameter("idPrograma", PROGRAMA)).thenReturn(planes);
        when(planes.getResultList()).thenReturn(List.of(fila(PLAN, 1), fila(PLAN_SIN_SEMESTRES, 0)));
        when(entityManager.createQuery(PlanEstudioJpaRepository.HQL_TOTAL_SEMESTRES, Object[].class)).thenReturn(totales);
        when(totales.setParameter(anyString(), any())).thenReturn(totales);
        when(totales.getResultList()).thenReturn(Collections.singletonList(new Object[]{PLAN, 8}));
        final TypedQuery<Object[]> malla = mock(TypedQuery.class);
        when(entityManager.createQuery(PlanEstudioJpaRepository.HQL_TOTALES_MALLA, Object[].class)).thenReturn(malla);
        when(malla.setParameter(anyString(), any())).thenReturn(malla);
        when(malla.getResultList()).thenReturn(Collections.singletonList(new Object[]{PLAN, 1L, 3L}));

        final List<PlanEstudioProjection> resultado = repository.consultarPlanesPorPrograma(PROGRAMA);

        assertEquals(2, resultado.size());
        assertEquals(8, resultado.get(0).totalSemestres());
        assertEquals(true, resultado.get(0).estaActivoPlanEstudio());
        // Certificación F3: totales reales de la malla (ARQ-402, 3 créditos); el plan sin asignaturas queda en 0.
        assertEquals(1, resultado.get(0).totalAsignaturas());
        assertEquals(3, resultado.get(0).totalCreditos());
        assertEquals(0, resultado.get(1).totalSemestres());
        assertEquals(false, resultado.get(1).estaActivoPlanEstudio());
        assertEquals(0, resultado.get(1).totalAsignaturas());
        assertEquals(0, resultado.get(1).totalCreditos());
    }

    @Test
    @SuppressWarnings("unchecked")
    void totales_de_malla_nulos_se_leen_como_cero() {
        final TypedQuery<UvPlanEstudioEntity> planes = mock(TypedQuery.class);
        final TypedQuery<Object[]> totales = mock(TypedQuery.class);
        final TypedQuery<Object[]> malla = mock(TypedQuery.class);
        when(entityManager.createQuery(PlanEstudioJpaRepository.HQL_POR_PROGRAMA, UvPlanEstudioEntity.class)).thenReturn(planes);
        when(planes.setParameter("idPrograma", PROGRAMA)).thenReturn(planes);
        when(planes.getResultList()).thenReturn(List.of(fila(PLAN, 1)));
        when(entityManager.createQuery(PlanEstudioJpaRepository.HQL_TOTAL_SEMESTRES, Object[].class)).thenReturn(totales);
        when(totales.setParameter(anyString(), any())).thenReturn(totales);
        when(totales.getResultList()).thenReturn(List.of());
        when(entityManager.createQuery(PlanEstudioJpaRepository.HQL_TOTALES_MALLA, Object[].class)).thenReturn(malla);
        when(malla.setParameter(anyString(), any())).thenReturn(malla);
        when(malla.getResultList()).thenReturn(Collections.singletonList(new Object[]{PLAN, 2L, null}));

        final PlanEstudioProjection plan = repository.consultarPlanesPorPrograma(PROGRAMA).getFirst();

        assertEquals(2, plan.totalAsignaturas());
        assertEquals(0, plan.totalCreditos());
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarPlanesPorPrograma_sin_planes_no_consulta_semestres() {
        final TypedQuery<UvPlanEstudioEntity> planes = mock(TypedQuery.class);
        when(entityManager.createQuery(PlanEstudioJpaRepository.HQL_POR_PROGRAMA, UvPlanEstudioEntity.class)).thenReturn(planes);
        when(planes.setParameter("idPrograma", PROGRAMA)).thenReturn(planes);
        when(planes.getResultList()).thenReturn(List.of());

        assertEquals(List.of(), repository.consultarPlanesPorPrograma(PROGRAMA));
        verify(entityManager, never()).createQuery(PlanEstudioJpaRepository.HQL_TOTAL_SEMESTRES, Object[].class);
    }

    private static UvPlanEstudioEntity fila(final UUID id, final int activo) {
        final Map<String, Object> columnas = new LinkedHashMap<>();
        columnas.put("id", id);
        columnas.put("idPrograma", PROGRAMA);
        columnas.put("nombrePrograma", "Sistemas");
        columnas.put("inp", 2026);
        columnas.put("estaActivoPlanEstudio", activo);
        columnas.put("estaActivoTextoPlanEstudio", activo == 1 ? "SI" : "NO");
        columnas.put("justificacionEstado", "Plan Estudio activo.");
        return HydratedViewRow.of(UvPlanEstudioEntity.class, columnas);
    }
}
