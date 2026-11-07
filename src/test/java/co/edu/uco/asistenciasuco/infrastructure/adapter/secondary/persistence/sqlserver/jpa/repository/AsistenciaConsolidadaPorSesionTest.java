package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CULM-001 B.2 ([SUPUESTO-B-15], CONTRACT 3.2.9): GET /grupos/{id}/asistencias conserva una fila por estudiante y sesion
 * con el estado consolidado (SJC > EX > AN) aunque la sesion tenga varias tomas.
 */
class AsistenciaConsolidadaPorSesionTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();
    private static final UUID OTRA_SESION = UUID.randomUUID();
    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LUIS = UUID.randomUUID();

    private static AsistenciaRepositoryProjection fila(final UUID id, final UUID estudiante, final UUID sesion, final String estado) {
        return new AsistenciaRepositoryProjection(id, estudiante, GRUPO, sesion, "AN".equals(estado), estado, "");
    }

    @Test
    void varias_tomas_se_consolidan_en_una_fila_por_estudiante_y_sesion() {
        final UUID primero = UUID.randomUUID();
        final List<AsistenciaRepositoryProjection> resultado = AsistenciaJpaRepository.consolidarPorSesion(List.of(
                fila(primero, ANA, SESION, "AN"),
                fila(UUID.randomUUID(), ANA, SESION, "SJC"),
                fila(UUID.randomUUID(), ANA, SESION, "EX"),
                fila(UUID.randomUUID(), LUIS, SESION, "AN"),
                fila(UUID.randomUUID(), LUIS, SESION, "EX"),
                fila(UUID.randomUUID(), ANA, OTRA_SESION, "AN"),
                fila(UUID.randomUUID(), ANA, OTRA_SESION, "AN")));

        assertEquals(3, resultado.size());
        final AsistenciaRepositoryProjection ana = resultado.get(0);
        assertEquals(primero, ana.getAsistencia());
        assertEquals("SJC", ana.getEstado());
        assertFalse(ana.isPresente());
        assertEquals("EX", resultado.get(1).getEstado());
        assertEquals("AN", resultado.get(2).getEstado());
        assertTrue(resultado.get(2).isPresente());
    }

    @Test
    void una_toma_por_sesion_queda_identica_y_un_estado_desconocido_no_gana() {
        final AsistenciaRepositoryProjection unica = fila(UUID.randomUUID(), LUIS, SESION, "EX");
        assertEquals(List.of(unica), AsistenciaJpaRepository.consolidarPorSesion(List.of(unica)));

        final List<AsistenciaRepositoryProjection> conNulo = AsistenciaJpaRepository.consolidarPorSesion(List.of(
                fila(UUID.randomUUID(), ANA, SESION, null), fila(UUID.randomUUID(), ANA, SESION, "AN")));
        assertEquals("AN", conNulo.getFirst().getEstado());
        assertTrue(AsistenciaJpaRepository.consolidarPorSesion(List.of()).isEmpty());
    }
}
