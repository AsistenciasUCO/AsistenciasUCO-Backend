package co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.dto.ProgramaInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.interactor.ConsultarProgramasInstitucionInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaInstitucionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaInstitucionProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 D.4 (CONTRACT §5.3): {@code GET /admin/programas}: pagina, tamano, texto y facultad llegan al puerto; el ambito es del SP. */
class ConsultarProgramasInstitucionUseCaseImplTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID FACULTAD = UUID.randomUUID();

    private final ProgramaInstitucionQueryPort puerto = mock(ProgramaInstitucionQueryPort.class);
    private final ConsultarProgramasInstitucionUseCaseImpl useCase = new ConsultarProgramasInstitucionUseCaseImpl(puerto);

    @Test
    void sin_parametros_pide_la_primera_pagina_con_el_tamano_por_defecto_y_sin_filtros() {
        when(puerto.consultarProgramas(ADMIN, null, null, 1, 50)).thenReturn(new PaginaResultado<>(List.of(), 1, 50, 0));

        assertEquals(0, useCase.execute(ADMIN, null, null, null, null).totalElementos());

        verify(puerto).consultarProgramas(ADMIN, null, null, 1, 50);
    }

    @Test
    void los_filtros_se_normalizan_y_se_entregan_al_puerto() {
        final UUID programa = UUID.randomUUID();
        when(puerto.consultarProgramas(ADMIN, "sistemas", FACULTAD, 2, 10)).thenReturn(new PaginaResultado<>(
                List.of(new ProgramaInstitucionProjection(programa, "Ingenieria de Sistemas", FACULTAD, "Ingenieria")), 2, 10, 11));

        final PaginaResultado<ProgramaInstitucionProjection> pagina = useCase.execute(ADMIN, "  sistemas ", FACULTAD, 2, 10);

        assertEquals(programa, pagina.elementos().getFirst().id());
        assertEquals(11, pagina.totalElementos());
        verify(puerto).consultarProgramas(ADMIN, "sistemas", FACULTAD, 2, 10);
    }

    @Test
    void un_texto_en_blanco_equivale_a_sin_filtro() {
        when(puerto.consultarProgramas(ADMIN, null, null, 1, 50)).thenReturn(new PaginaResultado<>(List.of(), 1, 50, 0));

        useCase.execute(ADMIN, "   ", null, 1, 50);

        verify(puerto).consultarProgramas(ADMIN, null, null, 1, 50);
    }

    @Test
    void el_interactor_convierte_las_proyecciones_a_dto() {
        final UUID programa = UUID.randomUUID();
        when(puerto.consultarProgramas(ADMIN, null, null, 1, 50)).thenReturn(new PaginaResultado<>(
                List.of(new ProgramaInstitucionProjection(programa, "Derecho", FACULTAD, "Juridicas")), 1, 50, 1));
        final ConsultarProgramasInstitucionInteractor interactor = new ConsultarProgramasInstitucionInteractor(useCase);

        final PaginaResultado<ProgramaInstitucionDTO> pagina = interactor.execute(ADMIN, null, null, null, null);

        assertEquals(new ProgramaInstitucionDTO(programa, "Derecho", FACULTAD, "Juridicas"), pagina.elementos().getFirst());
        assertEquals(1, pagina.totalElementos());
    }

    @Test
    void las_dependencias_y_el_actor_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ConsultarProgramasInstitucionUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new ConsultarProgramasInstitucionInteractor(null));
        assertThrows(NullPointerException.class, () -> useCase.execute(null, null, null, null, null));
    }
}
