package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.domain.MateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarMateriasEstudianteUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final MateriaEstudianteQueryPort queryPort = mock(MateriaEstudianteQueryPort.class);
    private final ConsultarMateriasEstudianteUseCaseImpl useCase =
            new ConsultarMateriasEstudianteUseCaseImpl(scopePort, queryPort);

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_estudiante() {
        final UUID actor = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(actor));
    }

    @Test
    void execute_consulta_materias_del_estudiante_resuelto() {
        final UUID actor = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(estudiante));
        // CULM-001 A.2-11: el SP recibe el USUARIO ejecutor y resuelve el estudiante (solo matricula A).
        when(queryPort.consultarMateriasEstudiante(actor)).thenReturn(List.of(
                new MateriaEstudianteProjection(asignatura, "Calculo", UUID.randomUUID(), "Grupo 1")));

        final List<MateriaEstudianteDomain> result = useCase.execute(actor);

        assertEquals(1, result.size());
        assertEquals(asignatura, result.getFirst().idAsignatura());
    }
}
