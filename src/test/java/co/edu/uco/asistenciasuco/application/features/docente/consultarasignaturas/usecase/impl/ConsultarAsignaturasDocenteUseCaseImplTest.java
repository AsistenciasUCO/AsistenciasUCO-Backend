package co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.domain.AsignaturaDocenteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarAsignaturasDocenteUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final AsignaturaDocenteQueryPort queryPort = mock(AsignaturaDocenteQueryPort.class);
    private final ConsultarAsignaturasDocenteUseCaseImpl useCase =
            new ConsultarAsignaturasDocenteUseCaseImpl(scopePort, queryPort);

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_docente() {
        final UUID actor = UUID.randomUUID();
        when(scopePort.findDocenteIdByUsuario(actor)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(actor));
    }

    @Test
    void execute_consulta_asignaturas_del_docente_resuelto() {
        final UUID actor = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        when(scopePort.findDocenteIdByUsuario(actor)).thenReturn(Optional.of(docente));
        when(queryPort.consultarAsignaturasDocente(docente)).thenReturn(List.of(
                new AsignaturaDocenteProjection(asignatura, "Calculo", UUID.randomUUID(), "Grupo 1",
                        UUID.randomUUID(), "Ingenieria", UUID.randomUUID(), "pregrado", 7)));

        final List<AsignaturaDocenteDomain> result = useCase.execute(actor);

        assertEquals(1, result.size());
        assertEquals(asignatura, result.getFirst().idAsignatura());
    }
    @Test
    void execute_preserva_tipo_programa_y_conteo_real_sin_inventar_valores_nulos() {
        final UUID actor = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        final UUID tipo = UUID.randomUUID();
        when(scopePort.findDocenteIdByUsuario(actor)).thenReturn(Optional.of(docente));
        when(queryPort.consultarAsignaturasDocente(docente)).thenReturn(List.of(
                new AsignaturaDocenteProjection(UUID.randomUUID(), "Calculo", UUID.randomUUID(), "G1",
                        UUID.randomUUID(), "Sistemas", tipo, "pregrado", 7),
                new AsignaturaDocenteProjection(UUID.randomUUID(), "Fisica", UUID.randomUUID(), "G2",
                        UUID.randomUUID(), "Sistemas", null, null, null)));

        final List<AsignaturaDocenteDomain> result = useCase.execute(actor);

        assertEquals(tipo, result.get(0).idTipoPrograma());
        assertEquals("pregrado", result.get(0).nombreTipoPrograma());
        assertEquals(7, result.get(0).estudiantesActivos());
        assertNull(result.get(1).idTipoPrograma());
        assertNull(result.get(1).estudiantesActivos());
    }
}
