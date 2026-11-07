package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioReclamoProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** NOTIF-002B: lectura de destinatarios de los avisos de reclamo mediante el SP dedicado. */
class DestinatariosReclamoJpaRepositoryTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final DestinatariosReclamoJpaRepository repository = new DestinatariosReclamoJpaRepository(entityManager);

    private void filas(final Object[]... filas) {
        when(entityManager.createNativeQuery(DestinatariosReclamoJpaRepository.SQL_CONSULTAR_DESTINATARIOS)).thenReturn(query);
        when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
        final List<Object> crudas = new ArrayList<>(java.util.Arrays.asList((Object[]) filas));
        when(query.getResultList()).thenReturn(crudas);
    }

    @Test
    void exige_entity_manager() {
        assertThrows(NullPointerException.class, () -> new DestinatariosReclamoJpaRepository(null));
    }

    @Test
    void por_sesion_mapea_las_filas_y_envia_sesion_y_estudiante() {
        final UUID sesion = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        final UUID usuario = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        filas(new Object[]{"DOCENTE", usuario.toString(), "Maria", "maria@uco.edu.co", grupo, "G01", "2026-08-24"});

        final List<DestinatarioReclamoProjection> resultado = repository.consultarDestinatariosPorSesion(sesion, estudiante);

        assertEquals(1, resultado.size());
        assertEquals("DOCENTE", resultado.getFirst().rol());
        assertEquals(usuario, resultado.getFirst().idUsuario());
        assertEquals(grupo, resultado.getFirst().idGrupo());
        assertEquals("G01", resultado.getFirst().nombreGrupo());
        assertEquals("2026-08-24", resultado.getFirst().fechaSesion());
        verify(query).setParameter("idSesion", sesion);
        verify(query).setParameter("idEstudiante", estudiante);
        verify(query).setParameter(eq("idSolicitud"), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void por_solicitud_envia_solo_la_solicitud_y_tolera_columnas_nulas_o_ausentes() {
        final UUID solicitud = UUID.randomUUID();
        filas(new Object[]{"ESTUDIANTE", null, null, null, null, null, null}, new Object[]{null});

        final List<DestinatarioReclamoProjection> resultado = repository.consultarDestinatariosPorSolicitud(solicitud);

        assertEquals(2, resultado.size());
        assertNull(resultado.getFirst().idUsuario());
        assertNull(resultado.getFirst().correo());
        assertEquals("", resultado.getFirst().nombreCompleto());
        assertEquals("", resultado.getFirst().nombreGrupo());
        assertEquals("", resultado.get(1).rol());
        verify(query).setParameter("idSolicitud", solicitud);
    }
}
