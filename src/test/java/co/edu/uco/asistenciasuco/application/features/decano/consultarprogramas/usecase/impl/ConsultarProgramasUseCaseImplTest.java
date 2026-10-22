package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarProgramasUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID FACULTAD = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final ProgramaQueryPort queryPort = mock(ProgramaQueryPort.class);
    private final ConsultarProgramasUseCaseImpl useCase = new ConsultarProgramasUseCaseImpl(scopePort, queryPort);

    @Test
    void execute_consulta_programas_de_la_facultad_del_decano() {
        final UUID idProg = UUID.randomUUID();
        final UUID idCoord = UUID.randomUUID();
        final UUID idTipo = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
        when(queryPort.consultarProgramasPorFacultad(FACULTAD)).thenReturn(List.of(
                new ProgramaProjection(
                        idProg, "Ingenieria de Sistemas", FACULTAD, "Ingenieria",
                        UUID.randomUUID(), "UCO", idCoord, "Coord Sistemas",
                        true, "SI", idTipo, "pregrado"
                )
        ));

        final List<ProgramaDomain> result = useCase.execute(ACTOR);
        assertEquals(1, result.size());
        assertEquals("Ingenieria de Sistemas", result.get(0).nombrePrograma());
        assertEquals("pregrado", result.get(0).nombreTipoPrograma());
    }

    @Test
    void execute_sin_facultad_asociada_lanza_forbiddenException() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());
        assertThrows(ForbiddenException.class, () -> useCase.execute(ACTOR));
    }
}
