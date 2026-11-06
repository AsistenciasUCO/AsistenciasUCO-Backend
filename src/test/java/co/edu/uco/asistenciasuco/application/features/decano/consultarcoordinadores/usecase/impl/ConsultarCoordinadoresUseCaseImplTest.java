package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain.CoordinadorDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenCoordinacionProjection;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.mapper.ConsultarCoordinadoresMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarCoordinadoresUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final CoordinadorQueryPort coordinadorQueryPort = mock(CoordinadorQueryPort.class);
    private final ConsultarCoordinadoresUseCaseImpl useCase =
            new ConsultarCoordinadoresUseCaseImpl(scopePort, coordinadorQueryPort);

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_facultad_del_decano() {
        final UUID actor = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actor)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(actor));
    }

    @Test
    void execute_consulta_coordinadores_de_la_facultad_resuelta() {
        final UUID actor = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final UUID coordinadorId = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actor)).thenReturn(Optional.of(facultad));
        when(coordinadorQueryPort.consultarCoordinadoresPorFacultad(facultad)).thenReturn(List.of(
                new CoordinadorProjection(coordinadorId, UUID.randomUUID(), "123456", "Ana Perez",
                        UUID.randomUUID(), "Ingenieria", true)));

        final List<CoordinadorDomain> result = useCase.execute(actor);

        assertEquals(1, result.size());
        assertEquals(coordinadorId, result.getFirst().id());
    }

    @Test
    void execute_incluye_coordinadores_disponibles_sin_programa_y_deduplica_por_id() {
        final UUID actor = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final UUID coordConPrograma = UUID.randomUUID();
        final UUID coordSinPrograma = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actor)).thenReturn(Optional.of(facultad));
        when(coordinadorQueryPort.consultarCoordinadoresPorFacultad(facultad)).thenReturn(List.of(
                new CoordinadorProjection(coordConPrograma, UUID.randomUUID(), "123456", "Ana Perez",
                        UUID.randomUUID(), "Ingenieria de Sistemas", true)));
        when(coordinadorQueryPort.consultarCoordinadoresSinPrograma(facultad)).thenReturn(List.of(
                new CoordinadorProjection(coordSinPrograma, UUID.randomUUID(), "789012", "Jorge Henao",
                        null, null, true)));

        final List<CoordinadorDomain> result = useCase.execute(actor);

        assertEquals(2, result.size());
        assertEquals(coordConPrograma, result.get(0).id());
        assertEquals(coordSinPrograma, result.get(1).id());
    }

    // HU121 T-121-05 / D2: el decano solo ve coordinadores sin programa de SU facultad.
    @Test
    void execute_consulta_los_coordinadores_sin_programa_solo_de_la_facultad_del_decano() {
        final UUID actor = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final UUID otraFacultad = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actor)).thenReturn(Optional.of(facultad));
        when(coordinadorQueryPort.consultarCoordinadoresSinPrograma(facultad)).thenReturn(List.of());

        final List<CoordinadorDomain> result = useCase.execute(actor);

        assertEquals(0, result.size());
        org.mockito.Mockito.verify(coordinadorQueryPort).consultarCoordinadoresSinPrograma(facultad);
        org.mockito.Mockito.verify(coordinadorQueryPort, org.mockito.Mockito.never())
                .consultarCoordinadoresSinPrograma(otraFacultad);
    }

    // HU121 CA-01: el listado entrega los datos editables para precargar el formulario de edicion.
    @Test
    void execute_expone_los_datos_editables_del_coordinador() {
        final UUID actor = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final UUID coordinadorId = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actor)).thenReturn(Optional.of(facultad));
        when(coordinadorQueryPort.consultarCoordinadoresPorFacultad(facultad)).thenReturn(List.of(
                new CoordinadorProjection(coordinadorId, UUID.randomUUID(), "123456", "Ana Maria Perez Gomez",
                        UUID.randomUUID(), "Ingenieria", true,
                        "Ana", "Maria", "Perez", "Gomez", "ana@uco.edu.co", true)));
        when(coordinadorQueryPort.consultarCoordinadoresSinPrograma(facultad)).thenReturn(List.of());

        final CoordinadorDomain result = useCase.execute(actor).getFirst();

        assertEquals("Ana", result.primerNombre());
        assertEquals("Maria", result.segundoNombre());
        assertEquals("Perez", result.primerApellido());
        assertEquals("Gomez", result.segundoApellido());
        assertEquals("ana@uco.edu.co", result.correo());
        assertEquals(true, result.estaActivoRolCoordinador());
    }

    // Certificacion F3: la tarjeta del decano muestra la facultad y los conteos reales del programa que coordina.
    @Test
    void execute_agrega_facultad_y_conteos_del_programa_coordinado() {
        final UUID actor = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actor)).thenReturn(Optional.of(facultad));
        when(coordinadorQueryPort.consultarCoordinadoresPorFacultad(facultad)).thenReturn(List.of(
                new CoordinadorProjection(UUID.randomUUID(), UUID.randomUUID(), "123456", "Ana Perez",
                        programa, "Ingenieria de Sistemas", true)));
        when(coordinadorQueryPort.consultarCoordinadoresSinPrograma(facultad)).thenReturn(List.of(
                new CoordinadorProjection(UUID.randomUUID(), UUID.randomUUID(), "789012", "Jorge Henao",
                        null, null, true)));
        when(coordinadorQueryPort.consultarResumenProgramasFacultad(facultad)).thenReturn(List.of(
                new ResumenCoordinacionProjection(programa, "Facultad de Ingenieria", 1, 2),
                new ResumenCoordinacionProjection(UUID.randomUUID(), "Facultad de Ingenieria", 5, 7)));

        final List<CoordinadorDomain> result = useCase.execute(actor);

        assertEquals("Facultad de Ingenieria", result.get(0).nombreFacultad());
        assertEquals(1, result.get(0).totalDocentes());
        assertEquals(2, result.get(0).totalGrupos());
        assertEquals("Facultad de Ingenieria", result.get(1).nombreFacultad());
        assertEquals(0, result.get(1).totalDocentes());
        assertEquals(0, result.get(1).totalGrupos());
        assertEquals(1, ConsultarCoordinadoresMapper.toDTO(result.get(0)).totalDocentes());
    }

    @Test
    void execute_sin_programas_en_la_facultad_deja_facultad_nula_y_conteos_en_cero() {
        final UUID actor = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actor)).thenReturn(Optional.of(facultad));
        when(coordinadorQueryPort.consultarCoordinadoresSinPrograma(facultad)).thenReturn(List.of(
                new CoordinadorProjection(UUID.randomUUID(), UUID.randomUUID(), "789012", "Jorge Henao",
                        null, null, true)));
        when(coordinadorQueryPort.consultarResumenProgramasFacultad(facultad)).thenReturn(
                java.util.Arrays.asList(null, new ResumenCoordinacionProjection(null, "X", 9, 9)));

        final CoordinadorDomain unico = useCase.execute(actor).getFirst();

        assertEquals(null, unico.nombreFacultad());
        assertEquals(0, unico.totalGrupos());
    }
}
