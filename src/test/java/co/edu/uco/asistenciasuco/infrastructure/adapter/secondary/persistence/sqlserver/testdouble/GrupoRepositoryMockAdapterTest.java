package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.testdouble;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrupoRepositoryMockAdapterTest {

    private final GrupoRepositoryMockAdapter adapter = new GrupoRepositoryMockAdapter();

    @Test
    void comandos_rechazan_dto_nulo() {
        assertThrows(CrosscuttingException.class, () -> adapter.crearGrupo(null));
        assertThrows(CrosscuttingException.class, () -> adapter.actualizarGrupo(null));
        assertThrows(CrosscuttingException.class, () -> adapter.registrarEstudianteEnGrupo(null));
    }

    @Test
    void crearGrupo_retorna_proyeccion_con_mismo_id() {
        final UUID id = UUID.randomUUID();
        final GrupoCommandRepositoryProjection resultado = adapter.crearGrupo(
                new CrearGrupoRepositoryDTO(id, UUID.randomUUID(), UUID.randomUUID(), 1, "Grupo 1", UUID.randomUUID(),
                        UUID.randomUUID()));

        assertEquals(id, resultado.idGrupo());
        assertEquals("Grupo registrado correctamente.", resultado.mensajeUsuario());
    }

    @Test
    void actualizarGrupo_retorna_proyeccion_con_mismo_id() {
        final UUID id = UUID.randomUUID();
        final GrupoCommandRepositoryProjection resultado = adapter.actualizarGrupo(
                new ActualizarGrupoRepositoryDTO(id, 1, "Grupo 1", UUID.randomUUID(), 30, UUID.randomUUID()));

        assertEquals(id, resultado.idGrupo());
        assertEquals("Grupo actualizado correctamente.", resultado.mensajeUsuario());
    }

    @Test
    void registrarEstudianteEnGrupo_retorna_mensaje_fijo() {
        final RegistrarEstudianteRepositoryProjection resultado = adapter.registrarEstudianteEnGrupo(
                new RegistrarEstudianteRepositoryDTO(UUID.randomUUID(), 123456789, "PEREZ", "GOMEZ",
                        "ANA", "MARIA", "ana@uco.edu.co", "Clave123!", UUID.randomUUID(), UUID.randomUUID()));

        assertEquals("Estudiante registrado correctamente.", resultado.getMensajeUsuario());
    }

    @Test
    void consultas_retornan_listas_vacias() {
        assertTrue(adapter.consultarGrupos(UUID.randomUUID()).isEmpty());
        assertTrue(adapter.consultarEstudiantesGrupo(UUID.randomUUID(), UUID.randomUUID()).isEmpty());
        assertEquals(List.of(), adapter.consultarGrupos(UUID.randomUUID()));
        assertTrue(adapter.buscarEstudiantePorDocumento(UUID.randomUUID(), UUID.randomUUID(), 123456, UUID.randomUUID()).isEmpty());
    }
}
