package co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.dto.OfertaCreacionGruposDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.interactor.ConsultarOfertaCreacionGruposDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.domain.OfertaCreacionGruposDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.impl.ConsultarOfertaCreacionGruposDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.OfertaCreacionGruposDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaOfertaDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** F3: oferta de asignaturas con las que el docente puede abrir un grupo. */
class ConsultarOfertaCreacionGruposDocenteUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final OfertaCreacionGruposDocenteQueryPort port = mock(OfertaCreacionGruposDocenteQueryPort.class);
    private final ConsultarOfertaCreacionGruposDocenteUseCaseImpl useCase =
            new ConsultarOfertaCreacionGruposDocenteUseCaseImpl(scope, port);

    @Test
    void devuelve_el_idDocente_del_token_y_las_asignaturas_de_la_consulta() {
        final UUID docente = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(docente));
        when(port.consultarOferta(ACTOR)).thenReturn(List.of(
                new AsignaturaOfertaDocenteProjection(asignatura, "MAT1", "Algebra", programa, "Sistemas")));

        final OfertaCreacionGruposDocenteDomain oferta = useCase.execute(ACTOR);

        assertEquals(docente, oferta.idDocente());
        assertEquals(1, oferta.asignaturas().size());
        assertEquals("Algebra", oferta.asignaturas().getFirst().nombreAsignatura());
        assertEquals(programa, oferta.asignaturas().getFirst().idPrograma());
    }

    @Test
    void sin_vinculo_devuelve_la_lista_vacia_con_el_idDocente() {
        final UUID docente = UUID.randomUUID();
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(docente));
        when(port.consultarOferta(ACTOR)).thenReturn(List.of());

        final OfertaCreacionGruposDocenteDomain oferta = useCase.execute(ACTOR);

        assertEquals(docente, oferta.idDocente());
        assertTrue(oferta.asignaturas().isEmpty());
    }

    @Test
    void si_el_usuario_no_es_un_docente_responde_forbidden_sin_consultar_la_oferta() {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(ACTOR));

        verifyNoInteractions(port);
    }

    @Test
    void el_interactor_mapea_el_dominio_al_dto() {
        final UUID docente = UUID.randomUUID();
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(docente));
        when(port.consultarOferta(ACTOR)).thenReturn(List.of(
                new AsignaturaOfertaDocenteProjection(UUID.randomUUID(), "MAT1", "Algebra", UUID.randomUUID(), "Sistemas")));

        final OfertaCreacionGruposDocenteDTO dto = new ConsultarOfertaCreacionGruposDocenteInteractor(useCase).execute(ACTOR);

        assertEquals(docente, dto.idDocente());
        assertEquals("MAT1", dto.asignaturas().getFirst().codigoAsignatura());
    }
}
