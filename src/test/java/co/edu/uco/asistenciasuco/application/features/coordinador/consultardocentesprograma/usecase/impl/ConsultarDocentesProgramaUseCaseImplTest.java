package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarDocentesProgramaUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final DocenteProgramaQueryPort queryPort = mock(DocenteProgramaQueryPort.class);
    private final ConsultarDocentesProgramaUseCaseImpl useCase =
            new ConsultarDocentesProgramaUseCaseImpl(scopePort, queryPort);

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_programa_del_coordinador() {
        final UUID actor = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(actor)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(actor));
    }

    @Test
    void execute_consulta_docentes_del_programa_resuelto_e_incluye_disponibles_deduplicados() {
        final UUID actor = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID docenteConGrupo = UUID.randomUUID();
        final UUID docenteSinGrupo = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(actor)).thenReturn(Optional.of(programa));
        when(queryPort.consultarDocentesPorPrograma(programa)).thenReturn(List.of(
                new DocenteProgramaProjection(docenteConGrupo, UUID.randomUUID(), "1017001", "Roberto Gómez",
                        "rgomez@uco.edu.co", programa, "Ingeniería de Sistemas", 2, true)));
        when(queryPort.consultarDocentesDisponibles()).thenReturn(List.of(
                new DocenteProgramaProjection(docenteConGrupo, UUID.randomUUID(), "1017001", "Roberto Gómez",
                        "rgomez@uco.edu.co", null, null, 0, true),
                new DocenteProgramaProjection(docenteSinGrupo, UUID.randomUUID(), "1017002", "Luisa Castro",
                        "lcastro@uco.edu.co", null, null, 0, true)));

        final List<DocenteProgramaDomain> result = useCase.execute(actor);

        assertEquals(2, result.size());
        assertEquals(docenteConGrupo, result.get(0).id());
        assertEquals(2, result.get(0).totalGruposAsignados());
        assertEquals(docenteSinGrupo, result.get(1).id());
        assertEquals(0, result.get(1).totalGruposAsignados());
    }
}
