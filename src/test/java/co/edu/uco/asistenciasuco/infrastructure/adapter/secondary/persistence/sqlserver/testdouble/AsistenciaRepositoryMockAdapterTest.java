package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.testdouble;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsistenciaRepositoryMockAdapterTest {

    private final AsistenciaRepositoryMockAdapter adapter = new AsistenciaRepositoryMockAdapter();

    @Test
    void metodos_de_comando_rechazan_dto_nulo() {
        assertThrows(CrosscuttingException.class, () -> adapter.registrarAsistencia(null));
        assertThrows(CrosscuttingException.class, () -> adapter.registrarAsistenciasSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.registrarAsistenciaAutonoma(null));
        assertThrows(CrosscuttingException.class, () -> adapter.solicitarRevisionAsistencia(null));
        assertThrows(CrosscuttingException.class, () -> adapter.resolverSolicitudRevisionAsistencia(null));
    }

    @Test
    void metodos_de_comando_aceptan_dto_valido_sin_lanzar() {
        adapter.registrarAsistencia(new RegistrarAsistenciaRepositoryDTO(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true, "A tiempo"));
        adapter.registrarAsistenciasSesion(new RegistrarAsistenciasSesionRepositoryDTO(UUID.randomUUID(), List.of(), UUID.randomUUID()));
        adapter.registrarAsistenciaAutonoma(new RegistrarAsistenciaAutonomaRepositoryDTO(
                UUID.randomUUID(), UUID.randomUUID(), "123456", UUID.randomUUID()));
        adapter.solicitarRevisionAsistencia(new SolicitarRevisionAsistenciaRepositoryDTO(
                UUID.randomUUID(), UUID.randomUUID(), "SALUD", "Justificacion", UUID.randomUUID(),
                UUID.randomUUID()));
        adapter.resolverSolicitudRevisionAsistencia(new ResolverSolicitudRevisionAsistenciaRepositoryDTO(
                UUID.randomUUID(), UUID.randomUUID(), "APROBAR", "Aceptada", UUID.randomUUID()));
    }

    @Test
    void consultarAsistenciasPorGrupo_rechaza_dto_nulo() {
        assertThrows(CrosscuttingException.class, () -> adapter.consultarAsistenciasPorGrupo(null));
    }

    @Test
    void consultarAsistenciasPorGrupo_con_sesion_nula_retorna_dos_registros_simulados() {
        final UUID grupo = UUID.randomUUID();
        final List<AsistenciaRepositoryProjection> resultado = adapter.consultarAsistenciasPorGrupo(
                new ConsultarAsistenciasPorGrupoRepositoryDTO(grupo, null));

        assertEquals(2, resultado.size());
        assertTrue(resultado.get(0).isPresente());
        assertFalse(resultado.get(1).isPresente());
        assertEquals(grupo, resultado.get(0).getGrupo());
        assertEquals(grupo, resultado.get(1).getGrupo());
        assertEquals("Llego tarde y reporto novedad.", resultado.get(1).getObservacion());
    }

    @Test
    void consultarAsistenciasPorGrupo_con_sesion_especifica_retorna_un_solo_registro() {
        final UUID grupo = UUID.randomUUID();
        final UUID sesion = UUID.randomUUID();
        final List<AsistenciaRepositoryProjection> resultado = adapter.consultarAsistenciasPorGrupo(
                new ConsultarAsistenciasPorGrupoRepositoryDTO(grupo, sesion));

        assertEquals(1, resultado.size());
        assertEquals(sesion, resultado.getFirst().getSesion());
        assertTrue(resultado.getFirst().isPresente());
    }
}
