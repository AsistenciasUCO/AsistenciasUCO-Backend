package co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.domain.HorarioEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarHorariosEstudianteUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final HorarioEstudianteQueryPort queryPort = mock(HorarioEstudianteQueryPort.class);
    private final ConsultarHorariosEstudianteUseCaseImpl useCase =
            new ConsultarHorariosEstudianteUseCaseImpl(scopePort, queryPort);

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_estudiante() {
        final UUID actor = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(actor));
    }

    @Test
    void execute_consulta_horarios_del_estudiante_resuelto() {
        final UUID actor = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(estudiante));
        // CULM-001 A.2-11: el SP recibe el USUARIO ejecutor y resuelve el estudiante (solo matricula A).
        when(queryPort.consultarHorarioEstudiante(actor)).thenReturn(List.of(new HorarioEstudianteProjection(
                id, estudiante, UUID.randomUUID(), "MAT-01", "Calculo", "G1", "LUNES",
                LocalTime.of(8, 0), LocalTime.of(10, 0), "Docente Uno")));

        final List<HorarioEstudianteDomain> result = useCase.execute(actor);

        assertEquals(1, result.size());
        assertEquals(id, result.getFirst().id());
    }
}
