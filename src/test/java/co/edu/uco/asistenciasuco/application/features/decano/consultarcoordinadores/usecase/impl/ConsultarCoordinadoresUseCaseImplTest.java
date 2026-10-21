package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain.CoordinadorDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;
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
        when(coordinadorQueryPort.consultarCoordinadoresSinPrograma()).thenReturn(List.of(
                new CoordinadorProjection(coordSinPrograma, UUID.randomUUID(), "789012", "Jorge Henao",
                        null, null, true)));

        final List<CoordinadorDomain> result = useCase.execute(actor);

        assertEquals(2, result.size());
        assertEquals(coordConPrograma, result.get(0).id());
        assertEquals(coordSinPrograma, result.get(1).id());
    }
}
